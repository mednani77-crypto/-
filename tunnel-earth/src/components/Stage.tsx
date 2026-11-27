import React from "react";
import { random, useCurrentFrame } from "remotion";
import { C, EARTH, H, W } from "../theme";
import { TOTAL, lin, sEnd, sStart, window01 } from "../lib/timeline";
import {
  CHORDS,
  DotState,
  K,
  burst,
  camera,
  chordDot,
  chordDraw,
  chordGeom,
  chordsVisible,
  mainDot,
  tunnel,
} from "../lib/state";

const R = EARTH.R;
const TW = 10; // tunnel half width

const Dot: React.FC<{ d: DotState; id: string; color?: string; size?: number }> = ({ d, id, color = C.cyan, size = 1 }) => {
  const tail = 230 * d.speed01 * size;
  const hx = d.x - d.vx * tail;
  const hy = d.y - d.vy * tail;
  return (
    <g opacity={d.opacity}>
      {tail > 4 ? (
        <>
          <defs>
            <linearGradient id={`trail-${id}`} gradientUnits="userSpaceOnUse" x1={d.x} y1={d.y} x2={hx} y2={hy}>
              <stop offset="0" stopColor={color} stopOpacity={0.95} />
              <stop offset="1" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <line x1={d.x} y1={d.y} x2={hx} y2={hy} stroke={`url(#trail-${id})`} strokeWidth={13 * size} strokeLinecap="round" />
          <line x1={d.x} y1={d.y} x2={hx} y2={hy} stroke={`url(#trail-${id})`} strokeWidth={4 * size} strokeLinecap="round" />
        </>
      ) : null}
      <circle cx={d.x} cy={d.y} r={42 * size} fill="url(#dotGlow)" />
      <circle cx={d.x} cy={d.y} r={13 * size} fill={C.ice} />
      <circle cx={d.x} cy={d.y} r={17 * size} fill="none" stroke={color} strokeWidth={3 * size} opacity={0.9} />
    </g>
  );
};

const Ring: React.FC<{ x: number; y: number; f: number; at: number; color?: string; max?: number }> = ({ x, y, f, at, color = C.cyanSoft, max = 90 }) => {
  const b = burst(f, at, 22);
  if (!b) return null;
  return (
    <g>
      <circle cx={x} cy={y} r={14 + b.r * max} fill="none" stroke={color} strokeWidth={5 * b.o + 1} opacity={b.o} />
      <circle cx={x} cy={y} r={8 + b.r * max * 0.55} fill="none" stroke={C.ice} strokeWidth={2} opacity={b.o * 0.7} />
    </g>
  );
};

export const Stage: React.FC = () => {
  const f = useCurrentFrame();
  const cam = camera(f);
  const loop = (f / TOTAL) * 360;
  const t = tunnel(f);
  const tipY = -R + t.progress * 2 * R;
  const dot = mainDot(f);
  const chordsO = chordsVisible(f);

  // gravity pulls toward the centre with a force proportional to the distance
  const arrowO =
    dot.inTunnel && (f < sStart(5) || f >= sStart(7) + 8) && f < K.FD
      ? Math.min(1, Math.abs(dot.y) / 45) * window01(f, K.FA + 4, K.FD, 8, 6)
      : 0;
  const arrowLen = 0.36 * Math.abs(dot.y);
  const arrowDir = dot.y > 0 ? -1 : 1;

  const streaks = window01(f, sStart(3) + 4, sEnd(3) + 2, 6, 8);

  return (
    <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
      <defs>
        <radialGradient id="halo" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0.72" stopColor={C.cyan} stopOpacity={0.3} />
          <stop offset="0.8" stopColor={C.cyan} stopOpacity={0.12} />
          <stop offset="1" stopColor={C.cyan} stopOpacity={0} />
        </radialGradient>
        <radialGradient id="mantle" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0.5" stopColor="#0E5677" />
          <stop offset="1" stopColor="#082B4B" />
        </radialGradient>
        <radialGradient id="outerCore" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0.3" stopColor="#1DB4D8" />
          <stop offset="1" stopColor="#0C6A92" />
        </radialGradient>
        <radialGradient id="innerCore" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="0.55" stopColor="#A7F6FF" />
          <stop offset="1" stopColor="#3CCDEB" />
        </radialGradient>
        <radialGradient id="coreGlow" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#B9FAFF" stopOpacity={0.55} />
          <stop offset="1" stopColor="#B9FAFF" stopOpacity={0} />
        </radialGradient>
        <radialGradient id="dotGlow" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity={0.95} />
          <stop offset="0.35" stopColor={C.cyan} stopOpacity={0.55} />
          <stop offset="1" stopColor={C.cyan} stopOpacity={0} />
        </radialGradient>
        <radialGradient id="tipGlow" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity={1} />
          <stop offset="0.4" stopColor={C.cyanSoft} stopOpacity={0.6} />
          <stop offset="1" stopColor={C.cyan} stopOpacity={0} />
        </radialGradient>
        <clipPath id="earthClip">
          <circle cx={0} cy={0} r={R - EARTH.crust / 2} />
        </clipPath>
      </defs>

      <g transform={`translate(${EARTH.cx + cam.tx} ${EARTH.cy + cam.ty}) scale(${cam.s})`}>
        {/* ---- Earth cross-section ---- */}
        <circle cx={0} cy={0} r={R * 1.32} fill="url(#halo)" />
        <circle cx={0} cy={0} r={R} fill="#0A3A58" />
        <circle cx={0} cy={0} r={R - EARTH.crust} fill="url(#mantle)" />
        <g opacity={0.5}>
          {[0.66, 0.78, 0.9].map((k, i) => (
            <circle
              key={k}
              cx={0}
              cy={0}
              r={R * k}
              fill="none"
              stroke={C.cyanSoft}
              strokeWidth={2}
              strokeDasharray={`${18 + i * 10} ${34 + i * 16}`}
              opacity={0.28}
              transform={`rotate(${(i % 2 ? -1 : 1) * loop * (i + 1)})`}
            />
          ))}
        </g>
        <circle cx={0} cy={0} r={R * EARTH.outerCore} fill="url(#outerCore)" stroke={C.cyanSoft} strokeWidth={3} strokeOpacity={0.75} />
        <circle cx={0} cy={0} r={R * EARTH.innerCore * 1.9} fill="url(#coreGlow)" />
        <circle cx={0} cy={0} r={R * EARTH.innerCore} fill="url(#innerCore)" />
        {/* crust: neon rim */}
        <circle cx={0} cy={0} r={R - EARTH.crust / 2} fill="none" stroke={C.cyan} strokeWidth={22} strokeOpacity={0.16} />
        <circle cx={0} cy={0} r={R - EARTH.crust / 2} fill="none" stroke="#1A7FA6" strokeWidth={EARTH.crust} />
        <circle
          cx={0}
          cy={0}
          r={R}
          fill="none"
          stroke={C.cyan}
          strokeWidth={4}
          opacity={0.85 + 0.15 * Math.sin(((f * 3) / TOTAL) * Math.PI * 2)}
        />

        {/* ---- scene 6: other straight tunnels ---- */}
        {chordsO > 0 ? (
          <g opacity={chordsO}>
            {CHORDS.map((c, i) => {
              const g = chordGeom(c);
              const p = chordDraw(f, i);
              const ex = g.ax + (g.bx - g.ax) * p;
              const ey = g.ay + (g.by - g.ay) * p;
              return (
                <g key={i} clipPath="url(#earthClip)">
                  <line x1={g.ax} y1={g.ay} x2={ex} y2={ey} stroke={c.hue} strokeWidth={30} strokeOpacity={0.12} strokeLinecap="round" />
                  <line x1={g.ax} y1={g.ay} x2={ex} y2={ey} stroke={C.ink} strokeWidth={13} />
                  <line x1={g.ax} y1={g.ay} x2={ex} y2={ey} stroke={c.hue} strokeWidth={2.5} strokeOpacity={0.95} />
                </g>
              );
            })}
            {CHORDS.map((c, i) => {
              const g = chordGeom(c);
              return (
                <g key={`e${i}`}>
                  <circle cx={g.ax} cy={g.ay} r={7} fill={c.hue} opacity={chordDraw(f, i)} />
                  <circle cx={g.bx} cy={g.by} r={9} fill="none" stroke={c.hue} strokeWidth={3} opacity={chordDraw(f, i)} />
                  <Ring x={g.bx} y={g.by} f={f} at={K.G1} color={c.hue} max={70} />
                </g>
              );
            })}
          </g>
        ) : null}

        {/* ---- the main tunnel ---- */}
        {t.opacity > 0 ? (
          <g opacity={t.opacity}>
            <line x1={0} y1={-R} x2={0} y2={tipY} stroke={C.cyan} strokeWidth={48} strokeOpacity={0.12} />
            <rect x={-TW} y={-R - 1} width={TW * 2} height={tipY - (-R - 1)} fill={C.ink} />
            <line x1={-TW} y1={-R} x2={-TW} y2={tipY} stroke={C.cyan} strokeWidth={2.5} />
            <line x1={TW} y1={-R} x2={TW} y2={tipY} stroke={C.cyan} strokeWidth={2.5} />
            {t.drilling ? (
              <g>
                <circle cx={0} cy={tipY} r={46} fill="url(#tipGlow)" />
                {new Array(16).fill(0).map((_, i) => {
                  const life = ((f * 0.11 + random(`sp${i}`)) % 1 + 1) % 1;
                  const a = -Math.PI / 2 + (random(`sa${i}`) - 0.5) * 2.4;
                  const dist = 10 + life * (40 + random(`sd${i}`) * 50);
                  return (
                    <circle
                      key={i}
                      cx={Math.cos(a) * dist}
                      cy={tipY + Math.sin(a) * dist}
                      r={2.2 + random(`sr${i}`) * 2}
                      fill={C.ice}
                      opacity={1 - life}
                    />
                  );
                })}
              </g>
            ) : null}
          </g>
        ) : null}

        {/* ---- scene 3: speed streaks through the zoomed centre ---- */}
        {streaks > 0 ? (
          <g opacity={streaks}>
            {new Array(18).fill(0).map((_, i) => {
              const x = (random(`lx${i}`) - 0.5) * 300;
              const len = 50 + random(`ll${i}`) * 110;
              const span = 640;
              const y = ((((random(`ly${i}`) * span - f * 34) % span) + span) % span) - span / 2;
              return (
                <line
                  key={i}
                  x1={Math.abs(x) < 26 ? x * 4 : x}
                  y1={y}
                  x2={Math.abs(x) < 26 ? x * 4 : x}
                  y2={y + len}
                  stroke={C.cyanSoft}
                  strokeWidth={2.2}
                  strokeOpacity={0.18 + random(`lo${i}`) * 0.3}
                  strokeLinecap="round"
                />
              );
            })}
          </g>
        ) : null}

        {/* ---- gravity arrow (force ∝ distance from the centre) ---- */}
        {arrowO > 0.01 && arrowLen > 8 ? (
          <g opacity={arrowO * 0.9} transform={`translate(${TW + 26} ${dot.y})`}>
            <line x1={0} y1={0} x2={0} y2={arrowDir * (arrowLen - 10)} stroke={C.white} strokeWidth={4} strokeLinecap="round" />
            <path
              d={`M -9 ${arrowDir * (arrowLen - 14)} L 0 ${arrowDir * arrowLen} L 9 ${arrowDir * (arrowLen - 14)} Z`}
              fill={C.white}
            />
          </g>
        ) : null}

        {/* ---- travellers ---- */}
        {chordsO > 0
          ? CHORDS.map((c, i) => (
              <g key={`d${i}`} opacity={chordsO * lin(f, sStart(6) + 6 + i * 3, sStart(6) + 14 + i * 3)}>
                <Dot d={chordDot(f, c)} id={`c${i}`} color={c.hue} size={0.82} />
              </g>
            ))
          : null}
        <Ring x={0} y={-R} f={f} at={K.H1 - 2} />
        <Ring x={0} y={R} f={f} at={K.FB - 2} />
        <Ring x={0} y={0} f={f} at={K.FM - 8} color={C.cyan} max={120} />
        <Ring x={0} y={-R} f={f} at={K.FD - 2} />
        <Dot d={dot} id="main" />
      </g>
    </svg>
  );
};
