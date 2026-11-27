#!/usr/bin/env python3
"""Generate the voiceover (one file per sentence) and the background music with the Gemini API.

Key lookup order (the key is never printed):
  1. GEMINI_API_KEY environment variable
  2. GEMINI_API_KEY in a .env file (project folder or repository root)
  3. none -> a placeholder header is sent, so a network secret configured for
     generativelanguage.googleapis.com (header x-goog-api-key) can be injected by the proxy.

Usage:
  python3 scripts/gen_audio.py voice      # all sentences, in parallel
  python3 scripts/gen_audio.py voice 3    # only sentence 3
  python3 scripts/gen_audio.py music
  python3 scripts/gen_audio.py check      # verify the key works (lists TTS/music models)
"""
import base64, json, os, re, subprocess, sys, time, urllib.error, urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "https://generativelanguage.googleapis.com/v1beta/"
SCRIPT = json.load(open(os.path.join(ROOT, "scripts", "script.json"), encoding="utf-8"))
RAW_DIR = os.path.join(ROOT, "build", "raw")
VO_DIR = os.path.join(ROOT, "public", "vo")
MUSIC_DIR = os.path.join(ROOT, "public", "music")


def _key():
    k = os.environ.get("GEMINI_API_KEY", "").strip()
    if k:
        return k
    for p in (os.path.join(ROOT, ".env"), os.path.join(os.path.dirname(ROOT), ".env")):
        if os.path.exists(p):
            for line in open(p, encoding="utf-8"):
                m = re.match(r"\s*GEMINI_API_KEY\s*=\s*(.*)\s*$", line)
                if m:
                    v = m.group(1).strip().strip('"').strip("'")
                    if v and "MY_GEMINI" not in v:
                        return v
    return "proxy-injected"


def _redact(text):
    k = _key()
    return text.replace(k, "<redacted>") if len(k) > 8 else text


def request(path, body=None, tries=4):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method="POST" if data else "GET",
                                 headers={"x-goog-api-key": _key(), "Content-Type": "application/json"})
    for t in range(tries):
        try:
            with urllib.request.urlopen(req, timeout=300) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            msg = _redact(e.read().decode(errors="replace"))[:700]
            if "spending cap" in msg or "spend cap" in msg:
                raise SystemExit("SPEND_CAP: the Gemini project hit its monthly spending cap (raise it at https://ai.studio/spend)")
            if e.code in (429, 500, 502, 503, 504) and t < tries - 1 and "limit: 0" not in msg:
                time.sleep(6 * (t + 1))
                continue
            raise SystemExit(f"API_ERROR {e.code} on {path.split(':')[0]}: {msg}")
        except (urllib.error.URLError, TimeoutError) as e:
            if t < tries - 1:
                time.sleep(5)
                continue
            raise SystemExit(f"NET_ERROR: {e}")


def first_audio(resp):
    try:
        parts = resp["candidates"][0]["content"]["parts"]
    except (KeyError, IndexError):
        raise SystemExit("NO_OUTPUT: " + _redact(json.dumps(resp, ensure_ascii=False))[:600])
    for p in parts:
        inline = p.get("inlineData") or p.get("inline_data")
        if inline and inline.get("mimeType", inline.get("mime_type", "")).startswith("audio"):
            return base64.b64decode(inline["data"]), inline.get("mimeType", inline.get("mime_type"))
    raise SystemExit("NO_AUDIO in response: " + ", ".join(sorted({k for p in parts for k in p})))


def to_wav(raw, mime, out):
    """Write the model's audio (raw PCM or a container) to a 24-bit-safe 48 kHz mono WAV."""
    tmp = out + ".src"
    open(tmp, "wb").write(raw)
    fmt = []
    if raw[:4] != b"RIFF" and not re.search(r"mpeg|mp3|wav|ogg|flac", mime or ""):
        rate = re.search(r"rate=(\d+)", mime or "")
        fmt = ["-f", "s16le", "-ar", rate.group(1) if rate else "24000", "-ac", "1"]
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", *fmt, "-i", tmp, "-ac", "1", "-ar", "48000", out], check=True)
    os.remove(tmp)


def gen_voice(scene):
    body = {
        "contents": [{"parts": [{"text": scene["tts"]}]}],  # the sentence only: no style prompt
        "generationConfig": {
            "responseModalities": ["AUDIO"],
            "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": SCRIPT["voice"]}}},
        },
    }
    resp = request(f"models/{SCRIPT['ttsModel']}:generateContent", body)
    raw, mime = first_audio(resp)
    os.makedirs(RAW_DIR, exist_ok=True)
    out = os.path.join(RAW_DIR, f"s{scene['id']}.wav")
    to_wav(raw, mime, out)
    print(f"voice s{scene['id']}: saved ({mime})")
    return out


def gen_music():
    body = {"contents": [{"parts": [{"text": SCRIPT["musicPrompt"]}]}]}
    resp = request(f"models/{SCRIPT['musicModel']}:generateContent", body)
    raw, mime = first_audio(resp)
    os.makedirs(MUSIC_DIR, exist_ok=True)
    src = os.path.join(RAW_DIR, "music.src")
    os.makedirs(RAW_DIR, exist_ok=True)
    open(src, "wb").write(raw)
    out = os.path.join(MUSIC_DIR, "bg.mp3")
    fmt = [] if (raw[:4] == b"RIFF" or re.search(r"mpeg|mp3|wav|ogg|flac", mime or "")) else ["-f", "s16le", "-ar", "48000", "-ac", "2"]
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", *fmt, "-i", src, "-ar", "48000", "-b:a", "192k", out], check=True)
    print(f"music: saved ({mime})")


def check():
    resp = request("models?pageSize=200")
    names = [m["name"].split("/")[-1] for m in resp.get("models", [])]
    for want in (SCRIPT["ttsModel"], SCRIPT["musicModel"]):
        print(f"{want}: {'available' if want in names else 'NOT LISTED'}")
    print("other tts/lyria models:", [n for n in names if "tts" in n or "lyria" in n])


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "voice"
    if cmd == "check":
        check()
    elif cmd == "music":
        gen_music()
    elif cmd == "voice":
        ids = {int(x) for x in sys.argv[2:]}
        todo = [s for s in SCRIPT["scenes"] if not ids or s["id"] in ids]
        with ThreadPoolExecutor(max_workers=4) as ex:
            list(ex.map(gen_voice, todo))
    else:
        raise SystemExit(__doc__)
