import React from "react";
import { useCurrentFrame } from "remotion";
import { C, CAIRO, DOT_REST } from "../theme";
import { TOTAL, easeOut, lin, sStart, window01 } from "../lib/timeline";
import { K, mainDot, toScreen } from "../lib/state";

const Pill: React.FC<{
  x: number;
  y: number;
  text: string;
  opacity: number;
  size?: number;
  pop?: number;
  color?: string;
}> = ({ x, y, text, opacity, size = 36, pop = 1, color = C.white }) => {
  if (opacity <= 0.001) return null;
  return (
    <div
      dir="rtl"
      lang="ar"
      style={{
        position: "absolute",
        left: x,
        top: y,
        translate: "-50% -50%",
        scale: String(0.7 + 0.3 * pop),
        opacity,
        fontFamily: CAIRO,
        fontWeight: 700,
        fontSize: size,
        lineHeight: 1.15,
        color,
        padding: "6px 18px 8px",
        borderRadius: 999,
        background: "rgba(2,10,30,0.72)",
        border: `2px solid rgba(56,232,255,0.55)`,
        boxShadow: "0 0 18px rgba(56,232,255,0.25)",
        whiteSpace: "nowrap",
      }}
    >
      {text}
    </div>
  );
};

/** HTML labels pinned to points of the stage (they follow the camera). */
export const StageLabels: React.FC = () => {
  const f = useCurrentFrame();

  // "أنت" next to the traveller while standing on the surface (frame 0 and the very end)
  const youIn = 1 - lin(f, K.D0 + 18, K.D0 + 26);
  const youOut = lin(f, TOTAL - 13, TOTAL - 4);
  const you = Math.max(youIn, youOut);
  const d = mainDot(f);
  const youPos = toScreen(f, DOT_REST.x + 74, DOT_REST.y - 28);

  // layer names during scene 1
  // the crust is the thin rim, so its label sits just outside it with a short leader line
  const crustA = (-128 * Math.PI) / 180;
  const polar = (r: number) => ({ x: r * Math.cos(crustA), y: r * Math.sin(crustA) });
  const layers = [
    { text: "القشرة", x: polar(394).x, y: polar(394).y, at: K.D0 + 4 },
    { text: "الوشاح", x: -228, y: -46, at: K.D0 + 12 },
    { text: "اللُّب", x: -92, y: 96, at: K.D0 + 20 },
  ];
  const layerEnd = sStart(2) + 4;

  // "الجاذبية" beside the force arrow at the start of the fall
  const gO = window01(f, K.FA + 8, sStart(3) - 4, 8, 8);
  const gPos = toScreen(f, 142, d.y + (d.y > 0 ? -1 : 1) * 0.18 * Math.abs(d.y));

  const crustO = window01(f, K.D0 + 4, layerEnd, 6, 8);
  const l0 = toScreen(f, polar(332).x, polar(332).y);
  const l1 = toScreen(f, polar(364).x, polar(364).y);

  return (
    <>
      {crustO > 0.001 ? (
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0, opacity: crustO }}>
          <line x1={l0.x} y1={l0.y} x2={l1.x} y2={l1.y} stroke={C.cyanSoft} strokeWidth={3} strokeLinecap="round" />
          <circle cx={l0.x} cy={l0.y} r={5} fill={C.cyanSoft} />
        </svg>
      ) : null}
      <Pill x={youPos.x} y={youPos.y} text="أنت" opacity={you} pop={1} size={38} color={C.cyanSoft} />
      {layers.map((l) => {
        const p = toScreen(f, l.x, l.y);
        const o = window01(f, l.at, layerEnd, 6, 8);
        return <Pill key={l.text} x={p.x} y={p.y} text={l.text} opacity={o} pop={easeOut(lin(f, l.at, l.at + 8))} size={34} />;
      })}
      {d.inTunnel ? <Pill x={gPos.x} y={gPos.y} text="الجاذبية" opacity={gO} size={30} color={C.white} /> : null}
    </>
  );
};
