import React from "react";
import { useCurrentFrame } from "remotion";
import { C, CAIRO, H, SAFE, SAFE_W } from "../theme";
import { SCENES, TOTAL, easeOut, lin } from "../lib/timeline";
import { ArabicText } from "./ArabicText";

type Cue = { text: string; from: number; to: number; fadeOut: boolean; continues: boolean };

/** Keep a number and the word after it on the same line ("٨ كيلومترات"). */
const bindNumbers = (t: string) => t.replace(/([٠-٩0-9][٠-٩0-9.,٫]*) /g, "$1\u00A0");

const CUES: Cue[] = SCENES.flatMap((s, si) =>
  s.chunks.map((c, ci) => {
    const last = ci === s.chunks.length - 1;
    const next = s.chunks[ci + 1];
    const end = next ? next.startFrame : c.endFrame; // hand over exactly when the next chunk starts
    const to = si === SCENES.length - 1 && last ? Math.min(end, TOTAL - 14) : end;
    return { text: bindNumbers(c.text), from: c.startFrame, to, fadeOut: last, continues: ci > 0 };
  }),
);

/** Captions synced to the voice, sitting right on top of the bottom safe zone. */
export const Subtitles: React.FC = () => {
  const f = useCurrentFrame();
  const cue = CUES.find((c) => f >= c.from && f < c.to);
  if (!cue) return null;
  // a new sentence slides in; the next chunk of the same sentence only cross-fades its text
  const inT = cue.continues ? 1 : easeOut(lin(f, cue.from, cue.from + 6));
  const textIn = cue.continues ? easeOut(lin(f, cue.from, cue.from + 4)) : 1;
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
          background: "rgba(2,8,26,0.9)",
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
        <span style={{ opacity: textIn }}>
          <ArabicText text={cue.text} numberGlow={false} />
        </span>
      </div>
    </div>
  );
};
