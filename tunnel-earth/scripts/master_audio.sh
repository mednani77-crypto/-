#!/usr/bin/env bash
# Final loudness for YouTube: -14 LUFS integrated, true peak <= -1 dBTP (two-pass loudnorm).
# The picture is copied untouched; only the audio track is re-encoded.
set -euo pipefail
cd "$(dirname "$0")/.."
IN=${1:-out/tunnel-earth-raw.mp4}
OUT=${2:-out/tunnel-earth.mp4}
M=$(ffmpeg -hide_banner -i "$IN" -af loudnorm=I=-14:TP=-1.0:LRA=11:print_format=json -f null - 2>&1 | sed -n '/^{/,/^}/p')
get() { echo "$M" | python3 -c "import json,sys; print(json.load(sys.stdin)['$1'])"; }
ffmpeg -loglevel error -y -i "$IN" -c:v copy \
  -af "loudnorm=I=-14:TP=-1.0:LRA=11:measured_I=$(get input_i):measured_TP=$(get input_tp):measured_LRA=$(get input_lra):measured_thresh=$(get input_thresh):offset=$(get target_offset):linear=true,aresample=48000" \
  -c:a aac -b:a 192k -movflags +faststart "$OUT"
echo "before: I=$(get input_i) LUFS, TP=$(get input_tp) dBTP"
ffmpeg -hide_banner -i "$OUT" -af ebur128=peak=true -f null - 2>&1 | grep -A12 Summary | grep -E "I:|Peak:" | sed 's/^ */after:  /'
