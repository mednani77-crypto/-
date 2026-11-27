import React from "react";
import { Audio } from "@remotion/media";
import { Sequence, interpolate, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { SCENES, TIMING, TOTAL } from "../lib/timeline";

const Music: React.FC<{ src: string; volume: number }> = ({ src, volume }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Audio
      name="Music"
      src={staticFile(src)}
      loop
      volume={interpolate(frame, [0, 0.6 * fps, TOTAL - 1.2 * fps, TOTAL - 1], [0, volume, volume, 0], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
      })}
    />
  );
};

export const Soundtrack: React.FC = () => {
  const { fps } = useVideoConfig();
  const wh = TIMING.whoosh;
  return (
    <>
      {SCENES.map((s) =>
        s.audio ? (
          <Sequence key={`vo${s.id}`} name={`Voice ${s.id}`} from={s.voStartFrame} durationInFrames={s.voFrames + 12} layout="none">
            <Audio src={staticFile(s.audio)} />
          </Sequence>
        ) : null,
      )}
      {wh
        ? SCENES.slice(1).map((s) => (
            <Sequence
              key={`wh${s.id}`}
              name={`Whoosh → ${s.id}`}
              from={Math.max(0, s.startFrame - Math.round(wh.peakSec * fps))}
              durationInFrames={Math.ceil(wh.durationSec * fps) + 2}
              layout="none"
            >
              <Audio src={staticFile(wh.file)} volume={wh.volume} />
            </Sequence>
          ))
        : null}
      {TIMING.music ? <Music src={TIMING.music.file} volume={TIMING.music.volume} /> : null}
    </>
  );
};
