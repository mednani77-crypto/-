import React from "react";
import { useCurrentFrame } from "remotion";
import { C, CAIRO, PHYS, SAFE, SAFE_CX } from "../theme";
import { TOTAL, easeInOut, easeOut, lin, mix, sEnd, sStart, window01 } from "../lib/timeline";
import { K, speedKmS, timerMin } from "../lib/state";

const CARD_W = 404;
const CARD_H = 150;
const CARD_TOP = 1194;
const SLOT_RIGHT = SAFE.right - CARD_W; // 546
const SLOT_LEFT = SAFE.left; // 80
const SLOT_CENTER = SAFE_CX - CARD_W / 2;

/** Fixed-width digits so the read-out does not jitter while it counts. */
const Digits: React.FC<{ text: string; size: number; glow?: number }> = ({ text, size, glow = 1 }) => (
  <div
    dir="ltr"
    style={{
      display: "flex",
      fontFamily: CAIRO,
      fontWeight: 800,
      fontSize: size,
      lineHeight: 1,
      color: C.orange,
      textShadow: `0 0 ${14 * glow}px rgba(255,155,61,${0.55 * glow}), 0 0 2px rgba(255,170,90,0.9)`,
    }}
  >
    {[...text].map((ch, i) => (
      <span key={i} style={{ display: "inline-block", width: size * (/[0-9٠-٩]/.test(ch) ? 0.58 : 0.28), textAlign: "center" }}>
        {ch}
      </span>
    ))}
  </div>
);

const toArabicDigits = (s: string) => s.replace(/[0-9]/g, (d) => "٠١٢٣٤٥٦٧٨٩"[Number(d)]);

const fmtTime = (min: number) => {
  const s = Math.round(min * 60);
  return `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
};

const Card: React.FC<{ x: number; w?: number; opacity: number; lift: number; scale?: number; glow?: number; children: React.ReactNode }> = ({
  x,
  w = CARD_W,
  opacity,
  lift,
  scale = 1,
  glow = 0,
  children,
}) => {
  if (opacity <= 0.001) return null;
  return (
    <div
      dir="rtl"
      style={{
        position: "absolute",
        left: x,
        top: CARD_TOP + (1 - lift) * 40,
        width: w,
        height: CARD_H,
        opacity,
        scale: String(scale),
        boxSizing: "border-box",
        padding: "16px 26px",
        borderRadius: 30,
        background: "linear-gradient(180deg, rgba(10,32,70,0.86), rgba(4,14,38,0.9))",
        border: `2px solid rgba(56,232,255,${0.55 + 0.35 * glow})`,
        boxShadow: `0 0 ${24 + 30 * glow}px rgba(56,232,255,${0.22 + 0.3 * glow}), inset 0 0 22px rgba(56,232,255,0.08)`,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: 14,
        fontFamily: CAIRO,
      }}
    >
      {children}
    </div>
  );
};

const Label: React.FC<{ text: string }> = ({ text }) => (
  <div style={{ fontSize: 34, fontWeight: 700, color: C.cyanSoft, lineHeight: 1.1, whiteSpace: "nowrap" }}>{text}</div>
);

const Gauge: React.FC<{ v01: number }> = ({ v01 }) => {
  const a = Math.PI * (1 - v01); // 180° (left) → 0° (right)
  const r = 54;
  const cx = 64;
  const cy = 64;
  const end = { x: cx + r * Math.cos(a), y: cy - r * Math.sin(a) };
  const large = 0;
  return (
    <svg width={128} height={74} style={{ overflow: "visible" }}>
      <path d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`} fill="none" stroke={C.cyan} strokeOpacity={0.18} strokeWidth={11} strokeLinecap="round" />
      {v01 > 0.004 ? (
        <path d={`M ${cx - r} ${cy} A ${r} ${r} 0 ${large} 1 ${end.x} ${end.y}`} fill="none" stroke={C.cyan} strokeWidth={11} strokeLinecap="round" />
      ) : null}
      {[0, 0.25, 0.5, 0.75, 1].map((t) => {
        const b = Math.PI * (1 - t);
        return (
          <line
            key={t}
            x1={cx + (r - 20) * Math.cos(b)}
            y1={cy - (r - 20) * Math.sin(b)}
            x2={cx + (r - 13) * Math.cos(b)}
            y2={cy - (r - 13) * Math.sin(b)}
            stroke={C.cyanSoft}
            strokeOpacity={0.7}
            strokeWidth={2.5}
          />
        );
      })}
      <line x1={cx} y1={cy} x2={cx + (r - 6) * Math.cos(a)} y2={cy - (r - 6) * Math.sin(a)} stroke={C.white} strokeWidth={4} strokeLinecap="round" />
      <circle cx={cx} cy={cy} r={7} fill={C.white} />
    </svg>
  );
};

const RingClock: React.FC<{ p: number; size?: number; stroke?: number }> = ({ p, size = 70, stroke = 8 }) => {
  const r = size / 2 - stroke;
  const c = 2 * Math.PI * r;
  return (
    <svg width={size} height={size}>
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={C.cyan} strokeOpacity={0.18} strokeWidth={stroke} />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        fill="none"
        stroke={C.cyan}
        strokeWidth={stroke}
        strokeLinecap="round"
        strokeDasharray={`${c * Math.max(0.001, p)} ${c}`}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
    </svg>
  );
};

export const Hud: React.FC = () => {
  const f = useCurrentFrame();
  const v = speedKmS(f);
  const min = timerMin(f);

  // the read-outs appear while the traveller jumps in, so they start from 0.0 km/s and 00:00
  const IN = K.FA - 6;
  // speed card: first crossing (scenes 1-4) and the way back (scene 7)
  const SB0 = sStart(7) + 6; // after the timer card has slid out of the way
  const sA = window01(f, IN, sStart(5), 8, 8);
  const sB = window01(f, SB0, K.FD + 4, 10, 8);
  const speedO = Math.max(sA, sB);
  const speedLift = f < sStart(5) ? easeOut(lin(f, IN, IN + 12)) : easeOut(lin(f, SB0, SB0 + 12));
  const hot = window01(f, K.FM - 26, K.FM + 30, 8, 10); // peak speed through the centre: glow only

  // timer card: scenes 1-4 (left), scene 6 (centre), scene 7 (back to the left)
  const tA = window01(f, IN, sStart(5), 8, 8);
  const tB = window01(f, sStart(6) + 8, K.FD + 4, 10, 8);
  const timerO = Math.max(tA, tB);
  const slide = easeInOut(lin(f, sStart(7) - 10, sStart(7) + 4));
  const timerX = f < sStart(6) ? SLOT_LEFT : mix(SLOT_CENTER, SLOT_LEFT, slide);
  const timerLift = f < sStart(6) ? easeOut(lin(f, IN, IN + 12)) : easeOut(lin(f, sStart(6) + 8, sStart(6) + 20));
  const lap =
    f < sStart(7) - 10
      ? min / PHYS.halfPeriodMin
      : mix(1, Math.max(0, (min - PHYS.halfPeriodMin) / PHYS.halfPeriodMin), slide); // ring resets during the slide
  const arrived = window01(f, K.G1, sEnd(6) - 4, 2, 8);

  // scene 1 (and the loop point): how long the tunnel is — the Earth's diameter
  const lenO = Math.max(1 - lin(f, K.H0, IN), lin(f, TOTAL - 12, TOTAL - 3));

  // scene 5: the big stopwatch (after the cards have gone and the Earth has started to shrink)
  const D = sStart(5) + 6;
  const dialO = window01(f, D, sStart(6) - 4, 8, 8); // gone before the Earth zooms back in
  const sweep = easeOut(lin(f, D + 1, D + 23));
  const dialPop = easeOut(lin(f, D, D + 11));
  // a heartbeat on the stopwatch half-way through the hold, so the scene keeps moving
  const B = Math.round((D + 23 + sStart(6) - 12) / 2);
  const beatT = lin(f, B, B + 14);
  const beat = beatT > 0 && beatT < 1 ? Math.sin(Math.PI * beatT) : 0;
  const noteO = lin(f, D + 34, D + 44);

  return (
    <>
      <Card x={SAFE_CX - 250} w={500} opacity={lenO} lift={1}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
          <Label text="طول النفق" />
          <svg width={120} height={34}>
            <line x1={10} y1={17} x2={110} y2={17} stroke={C.cyan} strokeWidth={4} />
            <path d="M 4 17 L 18 7 L 18 27 Z M 116 17 L 102 7 L 102 27 Z" fill={C.cyan} />
          </svg>
        </div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          <Digits text="12,742" size={76} />
          <div style={{ fontSize: 32, fontWeight: 700, color: C.white, marginTop: 4 }}>كم</div>
        </div>
      </Card>

      <Card x={SLOT_RIGHT} opacity={speedO} lift={speedLift} glow={hot}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 2 }}>
          <Label text="السرعة" />
          <Gauge v01={Math.min(1, v / 8)} />
        </div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          <Digits text={v.toFixed(1)} size={80} glow={1 + hot} />
          <div style={{ fontSize: 32, fontWeight: 700, color: C.white, marginTop: 4 }}>كم/ث</div>
        </div>
      </Card>

      <Card x={timerX} opacity={timerO} lift={timerLift} glow={arrived}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
          <Label text="الزمن" />
          <RingClock p={Math.min(1, Math.max(0, lap))} />
        </div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          <Digits text={fmtTime(min)} size={76} glow={1 + arrived} />
          <div dir="ltr" style={{ fontSize: 26, fontWeight: 700, color: C.white, marginTop: 6, whiteSpace: "nowrap" }}>
            دقيقة : ثانية
          </div>
        </div>
      </Card>

      {dialO > 0.001 ? (
        <div
          style={{
            position: "absolute",
            left: SAFE_CX - 200,
            top: 948,
            width: 400,
            height: 400,
            opacity: dialO,
            scale: String((0.75 + 0.25 * dialPop) * (1 + 0.07 * beat)),
          }}
        >
          <svg width={400} height={400} style={{ position: "absolute", inset: 0 }}>
            <circle cx={200} cy={200} r={178} fill="rgba(4,14,38,0.88)" stroke={C.cyan} strokeOpacity={0.5 + 0.5 * beat} strokeWidth={2 + 3 * beat} />
            {beatT > 0 && beatT < 1 ? (
              <circle cx={200} cy={200} r={178 + 22 * beatT} fill="none" stroke={C.orange} strokeOpacity={0.7 * (1 - beatT)} strokeWidth={4} />
            ) : null}
            <circle cx={200} cy={200} r={152} fill="none" stroke={C.cyan} strokeOpacity={0.16} strokeWidth={18} />
            <circle
              cx={200}
              cy={200}
              r={152}
              fill="none"
              stroke={C.orange}
              strokeWidth={18}
              strokeLinecap="round"
              strokeDasharray={`${2 * Math.PI * 152 * (42 / 60) * sweep} ${2 * Math.PI * 152}`}
              transform="rotate(-90 200 200)"
              style={{ filter: "drop-shadow(0 0 10px rgba(255,155,61,0.7))" }}
            />
            {new Array(12).fill(0).map((_, i) => {
              const a = (i / 12) * Math.PI * 2 - Math.PI / 2;
              return (
                <line
                  key={i}
                  x1={200 + 132 * Math.cos(a)}
                  y1={200 + 132 * Math.sin(a)}
                  x2={200 + (i % 3 === 0 ? 118 : 124) * Math.cos(a)}
                  y2={200 + (i % 3 === 0 ? 118 : 124) * Math.sin(a)}
                  stroke={C.cyanSoft}
                  strokeOpacity={0.75}
                  strokeWidth={i % 3 === 0 ? 4 : 2.5}
                  strokeLinecap="round"
                />
              );
            })}
          </svg>
          <div
            style={{
              position: "absolute",
              inset: 0,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              fontFamily: CAIRO,
            }}
          >
            <Digits text={toArabicDigits(String(Math.round(PHYS.halfPeriodMin * sweep)))} size={132} glow={1.6 + 1.4 * beat} />
            <div dir="rtl" style={{ fontSize: 42, fontWeight: 700, color: C.white, marginTop: 2 }}>
              دقيقة
            </div>
          </div>
        </div>
      ) : null}
      {dialO > 0.001 ? (
        <div
          dir="rtl"
          lang="ar"
          style={{
            position: "absolute",
            left: SAFE.left,
            width: SAFE.right - SAFE.left,
            top: 1356,
            textAlign: "center",
            fontFamily: CAIRO,
            fontWeight: 600,
            fontSize: 30,
            color: C.muted,
            opacity: dialO * noteO,
          }}
        >
          * بافتراض أرضٍ متجانسة الكثافة، بلا هواء ولا احتكاك
        </div>
      ) : null}
    </>
  );
};
