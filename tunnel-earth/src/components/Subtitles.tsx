import React from "react";
import { useCurrentFrame } from "remotion";
import { C, CAIRO, H, SAFE, SAFE_W } from "../theme";
import { SCENES, TOTAL, easeOut, lin } from "../lib/timeline";
import { ArabicText } from "./ArabicText";

type Cue = { text: string; from: number; to: number; fadeOut: boolean };

const CUES: Cue[] = SCENES.flatMap((s, si) =>
  s.chunks.map((c, ci) => {
    const last = ci === s.chunks.length - 1;
    const to = si === SCENES.length - 1 && last ? Math.min(c.endFrame, TOTAL - 14) : c.endFrame;
    return { text: c.text, from: c.startFrame, to, fadeOut: last };
  }),
);

/** Captions synced to the voice, sitting right on top of the bottom safe zone. */
export const Subtitles: React.FC = () => {
  const f = useCurrentFrame();
  const cue = CUES.find((c) => f >= c.from && f < c.to);
  if (!cue) return null;
  const inT = easeOut(lin(f, cue.from, cue.from + 6));
  const outT = cue.fadeOut ? lin(f, cue.to - 5, cue.to) : 0;
  return (
    <div
      style={{
        position: "absolute",
        left: SAFE.left,
        width: SAFE_W,
        bottom: H - SAFE.bottom + 12,
        display: "flex",
        justifyContent: "center",
      }}
    >
      <div
        dir="rtl"
        lang="ar"
        style={{
          maxWidth: SAFE_W,
          boxSizing: "border-box",
          padding: "8px 32px 14px",
          borderRadius: 26,
          background: "rgba(2,8,26,0.78)",
          border: "1.5px solid rgba(56,232,255,0.28)",
          fontFamily: CAIRO,
          fontWeight: 700,
          fontSize: 52,
          lineHeight: 1.4,
          color: C.white,
          textAlign: "center",
          textWrap: "balance",
          opacity: inT * (1 - outT),
          translate: `0 ${(1 - inT) * 18}px`,
          scale: String(0.96 + 0.04 * inT),
        }}
      >
        <ArabicText text={cue.text} numberGlow={false} />
      </div>
    </div>
  );
};
