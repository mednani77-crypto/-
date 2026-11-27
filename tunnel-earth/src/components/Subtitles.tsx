import React from "react";
import { useCurrentFrame } from "remotion";
import { C, CAIRO, H, SAFE, SAFE_W } from "../theme";
import { SCENES, TOTAL, easeOut, lin } from "../lib/timeline";
import { ArabicText } from "./ArabicText";

/** Keep a number and the word after it on the same line ("٨ كيلومترات"). */
const bindNumbers = (t: string) => t.replace(/([٠-٩0-9][٠-٩0-9.,٫]*) /g, "$1 ");

type Sentence = { from: number; to: number; chunks: { text: string; from: number; to: number }[] };

const SENTENCES: Sentence[] = SCENES.map((s, si) => {
  const chunks = s.chunks.map((c, ci) => {
    const next = s.chunks[ci + 1];
    return { text: bindNumbers(c.text), from: c.startFrame, to: next ? next.startFrame : c.endFrame };
  });
  const last = chunks[chunks.length - 1];
  if (si === SCENES.length - 1) last.to = Math.min(last.to, TOTAL - 14); // empty at the loop point
  return { from: chunks[0].from, to: last.to, chunks };
});

const XF = 2; // fade-in of the next chunk of the same sentence, in frames

/**
 * Captions synced to the voice, sitting right on top of the bottom safe zone.
 * All chunks of a sentence are stacked in the same grid cell, so the box keeps one size for the
 * whole sentence and the text simply cross-fades when the next chunk is spoken.
 */
export const Subtitles: React.FC = () => {
  const f = useCurrentFrame();
  const s = SENTENCES.find((x) => f >= x.from && f < x.to);
  if (!s) return null;
  const inT = easeOut(lin(f, s.from, s.from + 6));
  const outT = lin(f, s.to - 5, s.to);
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
          display: "grid",
          opacity: inT * (1 - outT),
          translate: `0 ${(1 - inT) * 18}px`,
          scale: String(0.96 + 0.04 * inT),
        }}
      >
        {s.chunks.map((c, i) => {
          // hand-over without overlap or an empty box: old text 1 -> 0.5 -> 0, new text 0 -> 0.5 -> 1
          const fadeIn = i === 0 ? 1 : lin(f, c.from, c.from + XF);
          const fadeOut = i === s.chunks.length - 1 ? 1 : 1 - lin(f, c.to - 1, c.to + 1);
          return (
            <div key={i} style={{ gridArea: "1 / 1", alignSelf: "center", textWrap: "balance", opacity: Math.min(fadeIn, fadeOut) }}>
              <ArabicText text={c.text} numberGlow={false} />
            </div>
          );
        })}
      </div>
    </div>
  );
};
