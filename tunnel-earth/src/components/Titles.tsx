import React from "react";
import { spring, useCurrentFrame, useVideoConfig } from "remotion";
import { C, CAIRO, SAFE, SAFE_W } from "../theme";
import { TOTAL, chunkAt, easeIn, lin, sEnd, sMid, sStart } from "../lib/timeline";
import { ArabicText } from "./ArabicText";

type TitleDef = {
  lines: string[][];
  from: number;
  to: number;
  size?: number;
  sub?: { text: string; from: number };
  /** "pop": words spring in one by one. "beat": already visible, each word pulses in turn (frame 0 of the loop). */
  mode?: "pop" | "beat";
};

const TITLES = (): TitleDef[] => [
  // frame 0 has an empty headline area, like the last frame, so the loop is seamless
  { lines: [["نفقٌ", "عبرَ", "الأرض"]], from: sStart(1) + 1, to: sEnd(1) },
  { lines: [["سقوطٌ", "نحو", "المركز"]], from: sStart(2) + 3, to: sEnd(2) },
  {
    lines: [["قرابةَ", "٨", "كم"], ["في", "الثانية!"]],
    from: sStart(3) + 3,
    to: sEnd(3),
    sub: { text: "أي نحو ٢٨ ألف كم في الساعة", from: sMid(3) - 4 },
  },
  { lines: [["تباطؤٌ", "حتى", "التوقف"]], from: sStart(4) + 3, to: sEnd(4), size: 86 },
  { lines: [["٤٢", "دقيقة", "فقط!"]], from: sStart(5) + 3, to: sEnd(5), size: 112 },
  { lines: [["أيُّ", "نفقٍ", "مستقيمٍ…"]], from: sStart(6) + 3, to: chunkAt(6, 1) + 4, size: 88 },
  { lines: [["المدةُ", "نفسُها!"]], from: chunkAt(6, 1) + 6, to: sEnd(6), size: 112 },
  { lines: [["ذهابٌ", "وإيابٌ…"], ["بلا", "نهاية"]], from: sStart(7) + 3, to: TOTAL - 4 },
];

const STAGGER = 5;

const Word: React.FC<{ text: string; local: number; size: number; mode: "pop" | "beat" }> = ({ text, local, size, mode }) => {
  const { fps } = useVideoConfig();
  const sprung = spring({ frame: local, fps, config: { damping: 15, stiffness: 170, mass: 0.6 } });
  // "beat": a heartbeat pulse 1 → 1.16 → 1 over 10 frames
  const pulse = mode === "beat" && local > 0 && local < 10 ? Math.sin((Math.PI * local) / 10) : 0;
  const pop = mode === "beat" ? 1 + 0.23 * pulse : sprung;
  const beat = mode === "beat" ? pulse : local > 0 ? Math.exp(-local / 7) : 0; // glow flash when the word lands
  const isNumber = /[٠-٩0-9]/.test(text);
  return (
    <span
      style={{
        display: "inline-block",
        opacity: mode === "beat" ? 1 : Math.min(1, Math.max(0, local / 4)),
        scale: String(0.3 + 0.7 * pop),
        translate: `0 ${(1 - pop) * 26}px`,
        fontSize: isNumber ? size * 1.18 : size,
        textShadow: `0 6px 22px rgba(0,0,0,0.65), 0 0 ${18 + 40 * beat}px rgba(56,232,255,${0.35 + 0.55 * beat})`,
      }}
    >
      <ArabicText text={text} />
    </span>
  );
};

const Title: React.FC<{ def: TitleDef; frame: number }> = ({ def, frame }) => {
  if (frame < def.from || frame >= def.to) return null;
  const size = def.size ?? 96;
  const out = def.to > TOTAL ? 0 : easeIn(lin(frame, def.to - 7, def.to));
  let i = 0;
  return (
    <div
      dir="rtl"
      lang="ar"
      style={{
        position: "absolute",
        left: SAFE.left,
        top: SAFE.top + 26,
        width: SAFE_W,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 4,
        fontFamily: CAIRO,
        fontWeight: 900,
        color: C.white,
        lineHeight: 1.25,
        opacity: 1 - out,
        translate: `0 ${-28 * out}px`,
        scale: String(1 - 0.06 * out),
      }}
    >
      {def.lines.map((line, li) => (
        <div key={li} style={{ display: "flex", flexDirection: "row", justifyContent: "center", alignItems: "baseline", gap: size * 0.3 }}>
          {line.map((w) => {
            const local = frame - def.from - STAGGER * i++;
            return <Word key={`${li}-${w}`} text={w} local={local} size={size} mode={def.mode ?? "pop"} />;
          })}
        </div>
      ))}
      {def.sub && frame >= def.sub.from ? (
        <div
          style={{
            marginTop: 30,
            fontSize: 44,
            fontWeight: 700,
            color: C.white,
            opacity: lin(frame, def.sub.from, def.sub.from + 6),
            translate: `0 ${(1 - lin(frame, def.sub.from, def.sub.from + 8)) * 14}px`,
            textShadow: "0 4px 16px rgba(0,0,0,0.7)",
            background: "rgba(2,10,30,0.86)",
            border: "1.5px solid rgba(56,232,255,0.45)",
            padding: "2px 26px 8px",
            borderRadius: 999,
          }}
        >
          <ArabicText text={def.sub.text} />
        </div>
      ) : null}
    </div>
  );
};

export const Titles: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <>
      {TITLES().map((d, i) => (
        <Title key={i} def={d} frame={frame} />
      ))}
    </>
  );
};
