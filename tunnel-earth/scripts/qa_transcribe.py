#!/usr/bin/env python3
"""QA: transcribe every trimmed voice clip with a Gemini model and compare it with the script.
Checks that the narrator said the right words (and nothing extra, e.g. no style text read aloud)
and that the edges were not cut by the silence trimming."""
import base64, json, os, re, sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_audio as g  # noqa: E402

MODEL = "gemini-3.8-flash"
ROOT = g.ROOT


def norm(s):
    s = re.sub(r"[ً-ٰٟـ]", "", s)  # harakat, tatweel
    s = s.translate(str.maketrans("أإآٱىة", "اااايه"))
    s = re.sub(r"[^ء-ي0-9٠-٩ ]", " ", s)
    return " ".join(s.split())


def transcribe(scene):
    path = os.path.join(ROOT, "public", "vo", f"s{scene['id']}.wav")
    body = {
        "contents": [{"parts": [
            {"inlineData": {"mimeType": "audio/wav", "data": base64.b64encode(open(path, "rb").read()).decode()}},
            {"text": "Transcribe this Arabic speech exactly, word for word, in Arabic script. "
                     "Write numbers as words. Output only the transcript. If the first or last word sounds cut off, "
                     "add the tag [CUT] after the transcript."},
        ]}],
        "generationConfig": {"temperature": 0},
    }
    resp = g.request(f"models/{MODEL}:generateContent", body)
    text = "".join(p.get("text", "") for p in resp["candidates"][0]["content"]["parts"]).strip()
    want, got = norm(scene["tts"]).split(), norm(text).split()
    missing = [w for w in want if w not in got]
    extra = [w for w in got if w not in want]
    return scene["id"], text, missing, extra


with ThreadPoolExecutor(4) as ex:
    for sid, text, missing, extra in sorted(ex.map(transcribe, g.SCRIPT["scenes"])):
        flag = "OK " if not missing and not extra and "[CUT]" not in text else "CHECK"
        print(f"[{flag}] s{sid}: {text}")
        if missing or extra:
            print(f"        missing={missing} extra={extra}")
