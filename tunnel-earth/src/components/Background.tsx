import React, { useMemo } from "react";
import { AbsoluteFill, random, useCurrentFrame } from "remotion";
import { C, H, W } from "../theme";
import { TOTAL } from "../lib/timeline";

type Star = { x: number; y: number; r: number; o: number; k: number; p: number };

/** Dark navy sky with faint stars. Every twinkle has a whole number of cycles per video, so it loops. */
export const Background: React.FC = () => {
  const frame = useCurrentFrame();
  const stars = useMemo<Star[]>(
    () =>
      new Array(190).fill(0).map((_, i) => {
        const big = random(`big${i}`) > 0.93;
        return {
          x: random(`x${i}`) * W,
          y: random(`y${i}`) * H,
          r: big ? 1.8 + random(`r${i}`) * 1.4 : 0.7 + random(`r${i}`) * 1.1,
          o: big ? 0.75 : 0.25 + random(`o${i}`) * 0.45,
          k: 1 + Math.floor(random(`k${i}`) * 4),
          p: random(`p${i}`) * Math.PI * 2,
        };
      }),
    [],
  );
  const loop = (frame / TOTAL) * Math.PI * 2;

  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(120% 75% at 48% 44%, ${C.bg1} 0%, #071233 45%, ${C.bg0} 100%)`,
      }}
    >
      <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
        <defs>
          <radialGradient id="neb1" cx="0.5" cy="0.5" r="0.5">
            <stop offset="0" stopColor="#0E6C9A" stopOpacity={0.22} />
            <stop offset="1" stopColor="#0E6C9A" stopOpacity={0} />
          </radialGradient>
          <radialGradient id="neb2" cx="0.5" cy="0.5" r="0.5">
            <stop offset="0" stopColor="#3B2C8F" stopOpacity={0.2} />
            <stop offset="1" stopColor="#3B2C8F" stopOpacity={0} />
          </radialGradient>
        </defs>
        <ellipse cx={200} cy={300} rx={520} ry={380} fill="url(#neb1)" />
        <ellipse cx={900} cy={1560} rx={560} ry={420} fill="url(#neb2)" />
        {stars.map((s, i) => {
          const tw = 0.6 + 0.4 * Math.sin(loop * s.k + s.p);
          return <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#DDF6FF" opacity={s.o * tw} />;
        })}
      </svg>
      <AbsoluteFill
        style={{ background: "radial-gradient(90% 60% at 50% 45%, rgba(0,0,0,0) 55%, rgba(0,2,10,0.55) 100%)" }}
      />
    </AbsoluteFill>
  );
};
