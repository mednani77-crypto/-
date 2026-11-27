#!/usr/bin/env python3
"""Trim the voice clips, measure them with ffprobe, and write src/data/timing.json.

For every sentence:
  build/raw/sN.wav  --(silence trimmed at both ends)-->  public/vo/sN.wav
  duration          <- ffprobe of the trimmed file
  subtitle chunks   <- split at the pauses found by silencedetect inside the clip

If a raw clip is missing, the sentence length is estimated (and timing.json says so),
so the visuals can be built before the audio exists.
"""
import json, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FPS = 30
SCRIPT = json.load(open(os.path.join(ROOT, "scripts", "script.json"), encoding="utf-8"))
RAW = os.path.join(ROOT, "build", "raw")
VO = os.path.join(ROOT, "public", "vo")
OUT = os.path.join(ROOT, "src", "data", "timing.json")

# seconds of picture before the voice starts / after it ends, per scene
LEAD = {1: 0.20, 2: 0.22, 3: 0.22, 4: 0.22, 5: 0.22, 6: 0.22, 7: 0.22}
TAIL = {1: 0.50, 2: 0.25, 3: 0.40, 4: 0.50, 5: 0.65, 6: 0.85, 7: 1.15}
EXTRA_WEIGHT = {1: 1, 2: 0.6, 3: 1, 4: 1, 5: 1, 6: 1.6, 7: 1.6}
MIN_SCENE = 2.3
TARGET_MIN, TARGET_MAX = 26.0, 34.0
THRESH = "-45dB"
VOICE_LUFS = -16.0


def ffprobe_duration(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path],
                         capture_output=True, text=True, check=True).stdout.strip()
    return float(out)


def trim(src, dst):
    edge = f"silenceremove=start_periods=1:start_threshold={THRESH}:start_silence=0.03:detection=peak"
    af = f"{edge},areverse,{edge},afade=t=in:d=0.012,areverse,afade=t=in:d=0.006"
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", src, "-af", af, "-ar", "48000", "-ac", "1", dst], check=True)


def loudness(path):
    err = subprocess.run(["ffmpeg", "-hide_banner", "-i", path, "-af", "ebur128=peak=true", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    summary = err[err.rfind("Summary:"):]
    return float(re.search(r"I:\s+(-?[0-9.]+) LUFS", summary).group(1)), float(re.search(r"Peak:\s+(-?[0-9.]+) dBFS", summary).group(1))


def normalize(path, target=VOICE_LUFS):
    """Same loudness for every sentence (plain gain; a limiter only if the peak would clip)."""
    i, _ = loudness(path)
    tmp = path + ".tmp.wav"
    gain = target - i
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", path, "-af",
                    f"volume={gain:.2f}dB,alimiter=limit=0.84:attack=2:release=40:level=false", tmp], check=True)
    os.replace(tmp, path)
    return round(i, 1), round(loudness(path)[0], 1)


def inner_silences(path):
    err = subprocess.run(["ffmpeg", "-hide_banner", "-i", path, "-af", "silencedetect=noise=-32dB:d=0.09", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    starts = [float(x) for x in re.findall(r"silence_start: ([0-9.]+)", err)]
    ends = [float(x) for x in re.findall(r"silence_end: ([0-9.]+)", err)]
    merged = []  # a tiny blip inside a pause must not split it in two
    for s, e in zip(starts, ends):
        if merged and s - merged[-1][1] < 0.06:
            merged[-1] = (merged[-1][0], e)
        else:
            merged.append((s, e))
    return merged


def letters(s):
    return len(re.sub(r"[\sً-ٰٟ…؟?!.،,]", "", s))


def estimate(scene):
    pauses = scene["tts"].count("…") + scene["tts"].count("؟")
    return round(letters(scene["tts"]) / 13.0 + 0.32 * pauses + 0.25, 3)


def chunk_offsets(scene, dur, silences):
    """Start offset (s, from voice start) of every chunk after the first."""
    chunks = scene["chunks"]
    total = sum(letters(c) for c in chunks)
    offs, acc = [], 0
    for c in chunks[:-1]:
        acc += letters(c)
        expected = dur * acc / total
        best = None
        for s, e in silences:
            if 0.15 < s and e < dur - 0.1 and abs((s + e) / 2 - expected) < 0.35 * dur:
                if best is None or abs((s + e) / 2 - expected) < abs((best[0] + best[1]) / 2 - expected):
                    best = (s, e)
        offs.append(round(max(best[1] - 0.04, best[0]), 3) if best else round(expected, 3))
    return offs


def main():
    os.makedirs(VO, exist_ok=True)
    rows = []
    for sc in SCRIPT["scenes"]:
        raw = os.path.join(RAW, f"s{sc['id']}.wav")
        vo = os.path.join(VO, f"s{sc['id']}.wav")
        if os.path.exists(raw):
            trim(raw, vo)
            before, after = normalize(vo)
            print(f"  s{sc['id']}: loudness {before} -> {after} LUFS")
            dur, measured, sil = ffprobe_duration(vo), True, inner_silences(vo)
            raw_dur = ffprobe_duration(raw)
        else:
            dur, measured, sil, raw_dur = estimate(sc), False, [], None
        rows.append(dict(sc=sc, dur=dur, measured=measured, sil=sil, raw_dur=raw_dur))

    lead = dict(LEAD)
    tail = dict(TAIL)
    scene_len = lambda r: max(MIN_SCENE, lead[r["sc"]["id"]] + r["dur"] + tail[r["sc"]["id"]])
    total = sum(scene_len(r) for r in rows)
    if total < TARGET_MIN:
        extra = TARGET_MIN - total
        wsum = sum(EXTRA_WEIGHT.values())
        for k in tail:
            tail[k] += extra * EXTRA_WEIGHT[k] / wsum
    elif total > TARGET_MAX:
        over = total - TARGET_MAX
        for k in tail:
            tail[k] = max(0.15, tail[k] - over / len(tail))

    scenes, t = [], 0.0
    for r in rows:
        sc, sid = r["sc"], r["sc"]["id"]
        length = scene_len(r)
        start_f, end_f = round(t * FPS), round((t + length) * FPS)
        vo_start_f = start_f + round(lead[sid] * FPS)
        vo_frames = max(1, round(r["dur"] * FPS))
        offs = chunk_offsets(sc, r["dur"], r["sil"])
        starts = [vo_start_f] + [vo_start_f + round(o * FPS) for o in offs]
        chunks = []
        for i, text in enumerate(sc["chunks"]):
            end = starts[i + 1] if i + 1 < len(starts) else end_f - 2
            chunks.append({"text": text, "startFrame": starts[i] - 2, "endFrame": end})
        scenes.append({
            "id": sid, "text": sc["text"], "tts": sc["tts"],
            "audio": f"vo/s{sid}.wav" if r["measured"] else None,
            "voiceSec": round(r["dur"], 3), "rawVoiceSec": r["raw_dur"], "measured": r["measured"],
            "innerSilences": [[round(a, 3), round(b, 3)] for a, b in r["sil"]],
            "startFrame": start_f, "durationFrames": end_f - start_f,
            "voStartFrame": vo_start_f, "voFrames": vo_frames,
            "chunks": chunks,
        })
        t += length

    total_frames = scenes[-1]["startFrame"] + scenes[-1]["durationFrames"]
    music = os.path.join(ROOT, "public", "music", "bg.mp3")
    whoosh = os.path.join(ROOT, "public", "sfx", "whoosh.wav")
    data = {
        "fps": FPS, "width": 1080, "height": 1920,
        "measured": all(r["measured"] for r in rows),
        "totalFrames": total_frames, "totalSec": round(total_frames / FPS, 3),
        "voice": SCRIPT["voice"], "ttsModel": SCRIPT["ttsModel"],
        "music": {"file": "music/bg.mp3", "volume": 0.10, "durationSec": round(ffprobe_duration(music), 3)} if os.path.exists(music) else None,
        "whoosh": {"file": "sfx/whoosh.wav", "volume": 0.32, "durationSec": round(ffprobe_duration(whoosh), 3), "peakSec": 0.36} if os.path.exists(whoosh) else None,
        "scenes": scenes,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(data, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"timing.json: {len(scenes)} scenes, {data['totalSec']}s ({total_frames} frames), measured={data['measured']}")
    for s in scenes:
        src = f"raw {s['rawVoiceSec']}s -> trimmed" if s["measured"] else "ESTIMATED"
        print(f"  s{s['id']}: start {s['startFrame']/FPS:6.2f}s  len {s['durationFrames']/FPS:5.2f}s  voice {s['voiceSec']:.2f}s ({src})"
              f"  chunks@ {[round((c['startFrame']+2-s['voStartFrame'])/FPS,2) for c in s['chunks']]}")


if __name__ == "__main__":
    main()
