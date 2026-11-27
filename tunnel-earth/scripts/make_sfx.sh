#!/usr/bin/env bash
# Soft "whoosh" for scene transitions, synthesised with ffmpeg only:
# pink noise through three band-pass layers whose envelopes peak one after another
# (low -> mid -> high, so the pitch seems to rise), panned right-to-left, then normalised.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=public/sfx/whoosh.wav
TMP=build/whoosh_raw.wav
mkdir -p public/sfx build
D=0.75
ffmpeg -loglevel error -y -f lavfi -i "anoisesrc=d=${D}:c=pink:r=48000:a=0.9:seed=7" -filter_complex "
[0]asetnsamples=n=64,asplit=3[a][b][c];
[a]bandpass=f=450:width_type=q:w=1.1,volume=eval=frame:volume='exp(-pow((t-0.26)/0.13,2))'[a1];
[b]bandpass=f=1100:width_type=q:w=1.1,volume=eval=frame:volume='0.9*exp(-pow((t-0.36)/0.12,2))'[b1];
[c]bandpass=f=2600:width_type=q:w=1.0,volume=eval=frame:volume='0.7*exp(-pow((t-0.45)/0.12,2))'[c1];
[a1][b1][c1]amix=inputs=3:normalize=0,lowpass=f=6000,
aeval=exprs='val(0)*(0.55-0.35*cos(PI*t/${D}))|val(0)*(0.55+0.35*cos(PI*t/${D}))':channel_layout=stereo,
afade=t=in:d=0.04,afade=t=out:st=0.57:d=0.18[out]" -map "[out]" "$TMP"
PEAK=$(ffmpeg -hide_banner -i "$TMP" -af volumedetect -f null - 2>&1 | sed -n 's/.*max_volume: \(-\?[0-9.]*\) dB/\1/p')
GAIN=$(python3 -c "print(round(-3 - ($PEAK), 2))")
ffmpeg -loglevel error -y -i "$TMP" -af "volume=${GAIN}dB" -ar 48000 "$OUT"
echo "whoosh: $OUT ($(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT")s, gain ${GAIN} dB)"
