/**
 * Everything that moves on the stage is a pure function of the global frame,
 * so the picture is deterministic and the last frame flows back into frame 0.
 *
 * Physics: inside a uniform-density Earth gravity grows linearly with the distance
 * from the centre, so a body in a straight tunnel performs simple harmonic motion:
 *   position along the tunnel  s(φ) = -L cos φ,   speed ∝ sin φ,   φ = ω t.
 * The visuals run the simulation clock at a variable rate (slow motion through the
 * centre), but position, speed read-out and timer are all derived from the same φ.
 */
import { DOT_REST, EARTH, PHYS } from "../theme";
import {
  TOTAL,
  chunkAt,
  easeInOut,
  easeOut,
  lin,
  mix,
  sEnd,
  sMid,
  sStart,
  voEnd,
} from "./timeline";

const R = EARTH.R;

// ---------- key frames ----------
export const K = (() => {
  const FA = Math.min(chunkAt(1, 1) + 18, sStart(2)); // the fall begins (= when the jump lands)
  const FM = sMid(3); // passing the centre
  const FB = sEnd(4) - 8; // arrival at the far side
  const D0 = sStart(1) + 10; // drilling starts
  const D1 = Math.max(D0 + 24, chunkAt(1, 1) - 3); // drilling ends as "وقفزتَ فيه" starts
  const H0 = Math.min(chunkAt(1, 1) + 2, sStart(2) - 16); // the jump starts on "وقفزتَ"
  const H1 = H0 + 16; // ...and lands in the tunnel mouth; the fall starts right away
  const G0 = sStart(6) + 16; // chord race starts
  const G1 = Math.min(voEnd(6) + 3, sEnd(6) - 12); // ...and ends: everyone arrives together
  const FC = sStart(7) + 8; // the way back starts
  const FD = TOTAL - 24; // back at the top
  const RESET0 = FD + 1; // climb out + tunnel fades: back to frame 0
  const RESET1 = TOTAL - 3;
  return { FA, FM, FB, D0, D1, H0, H1, G0, G1, FC, FD, RESET0, RESET1 };
})();

// ---------- simulation clock for the first crossing (with slow motion at the centre) ----------
const SIGMA = Math.max(8, (sEnd(3) - sStart(3)) * 0.3);
const rate = (f: number) => 1 - 0.8 * Math.exp(-(((f - K.FM) / SIGMA) ** 2));
const CUM: number[] = (() => {
  const c = new Array(TOTAL + 2).fill(0);
  for (let f = 1; f < c.length; f++) c[f] = c[f - 1] + (f - 1 >= K.FA && f - 1 < K.FB ? rate(f - 1 + 0.5) : 0);
  return c;
})();
const cumAt = (f: number) => {
  const i = Math.max(0, Math.min(CUM.length - 2, Math.floor(f)));
  return CUM[i] + (CUM[i + 1] - CUM[i]) * (f - i);
};
/** Fraction of the first crossing completed, 0 → 1. */
export const uFall = (f: number) => {
  if (f <= K.FA) return 0;
  if (f >= K.FB) return 1;
  if (f <= K.FM) return (0.5 * (cumAt(f) - cumAt(K.FA))) / (cumAt(K.FM) - cumAt(K.FA));
  return 0.5 + (0.5 * (cumAt(f) - cumAt(K.FM))) / (cumAt(K.FB) - cumAt(K.FM));
};

/** Phase φ of the main traveller (0 at the top, π at the bottom, 2π back at the top). */
export const mainPhase = (f: number) => {
  if (f < sStart(7)) return Math.PI * uFall(f);
  return Math.PI + Math.PI * lin(f, K.FC, K.FD);
};

export type DotState = {
  x: number; // local coordinates, Earth centre = (0, 0), y down
  y: number;
  vx: number; // unit direction of motion
  vy: number;
  speed01: number; // physical speed / max speed
  inTunnel: boolean;
  opacity: number;
};

export const mainDot = (f: number): DotState => {
  // standing on the surface (start and end of the loop)
  if (f < K.H0) return { x: DOT_REST.x, y: DOT_REST.y, vx: 0, vy: 1, speed01: 0, inTunnel: false, opacity: 1 };
  if (f < K.H1) {
    const t = lin(f, K.H0, K.H1);
    const e = easeInOut(t);
    const x = mix(DOT_REST.x, 0, e);
    const y = mix(DOT_REST.y, -R, easeInOut(t)) - 62 * Math.sin(Math.PI * Math.min(1, t * 1.05));
    return { x, y, vx: -1, vy: 0, speed01: 0, inTunnel: false, opacity: 1 };
  }
  if (f >= K.RESET0) {
    const t = lin(f, K.RESET0, K.RESET1);
    const x = mix(0, DOT_REST.x, easeInOut(t));
    const y = mix(-R, DOT_REST.y, easeInOut(t)) - 52 * Math.sin(Math.PI * t);
    return { x, y, vx: 1, vy: 0, speed01: 0, inTunnel: false, opacity: 1 };
  }
  const phi = mainPhase(f);
  const y = -R * Math.cos(phi);
  const dir = Math.sin(phi) >= 0 ? 1 : -1; // moving down during the first crossing, up on the way back
  const parked = f >= sStart(6) && f < sStart(7);
  const dimIn = lin(f, sStart(6), sStart(6) + 10);
  const dimOut = lin(f, sStart(7), sStart(7) + 10);
  return {
    x: 0,
    y,
    vx: 0,
    vy: dir,
    speed01: Math.abs(Math.sin(phi)),
    inTunnel: true,
    opacity: parked ? mix(1, 0.35, dimIn) : f >= sStart(7) ? mix(0.35, 1, dimOut) : 1,
  };
};

/** Speed read-out, km/s (main traveller). */
export const speedKmS = (f: number) => PHYS.vMaxKmS * Math.abs(Math.sin(mainPhase(f)));

/** Timer read-out, minutes. */
export const timerMin = (f: number) => {
  if (f < sStart(6)) return PHYS.halfPeriodMin * uFall(f);
  if (f < sStart(7)) return PHYS.halfPeriodMin * chordU(f);
  return PHYS.halfPeriodMin * (1 + lin(f, K.FC, K.FD));
};

// ---------- the main tunnel ----------
export const tunnel = (f: number) => {
  const drill = easeInOut(lin(f, K.D0, K.D1));
  const fadeOut = 1 - lin(f, K.RESET0 - 2, K.RESET1 - 2);
  const dim = f >= sStart(6) && f < sStart(7) + 10 ? mix(1, 0.4, Math.min(lin(f, sStart(6), sStart(6) + 10), 1 - lin(f, sStart(7), sStart(7) + 10))) : 1;
  return { progress: f >= K.RESET0 - 2 ? 1 : drill, opacity: (f < K.D0 ? 0 : 1) * fadeOut * dim, drilling: f >= K.D0 && f < K.D1 };
};

// ---------- scene 6: many tunnels, one travel time ----------
/** A straight tunnel from the surface point at angle `a` to the one at angle `b` (degrees, screen space, y down). */
export type Chord = { a: number; b: number; hue: string };
export const CHORDS: Chord[] = [
  { a: 300, b: 120, hue: "#38E8FF" }, // through the centre: the longest
  { a: 200, b: 70, hue: "#8BF4FF" }, // 0.91 of the diameter
  { a: 160, b: 255, hue: "#5FD0FF" }, // 0.74
  { a: 330, b: 30, hue: "#B8F9FF" }, // 0.50: short, close to the surface
];
const onCircle = (deg: number) => ({ x: R * Math.cos((deg * Math.PI) / 180), y: R * Math.sin((deg * Math.PI) / 180) });
export const chordGeom = (c: Chord) => {
  const A = onCircle(c.a);
  const B = onCircle(c.b);
  const L = Math.hypot(B.x - A.x, B.y - A.y) / 2;
  const tx = (B.x - A.x) / (2 * L);
  const ty = (B.y - A.y) / (2 * L);
  return { tx, ty, L, mx: (A.x + B.x) / 2, my: (A.y + B.y) / 2, ax: A.x, ay: A.y, bx: B.x, by: B.y };
};
export const chordU = (f: number) => lin(f, K.G0, K.G1);
export const chordsVisible = (f: number) =>
  Math.min(lin(f, sStart(6), sStart(6) + 8), 1 - lin(f, sStart(7), sStart(7) + 10));
export const chordDraw = (f: number, i: number) => easeOut(lin(f, sStart(6) + 2 + i * 3, sStart(6) + 14 + i * 3));
export const chordDot = (f: number, c: Chord): DotState => {
  const g = chordGeom(c);
  const phi = Math.PI * chordU(f);
  const s = -g.L * Math.cos(phi);
  return {
    x: g.mx + s * g.tx,
    y: g.my + s * g.ty,
    vx: g.tx,
    vy: g.ty,
    speed01: (Math.abs(Math.sin(phi)) * g.L) / R,
    inTunnel: true,
    opacity: 1,
  };
};

// ---------- camera ----------
type CamKey = [frame: number, scale: number, tx: number, ty: number];
const CAM: CamKey[] = [
  [0, 1, 0, 0],
  [sStart(2), 1, 0, 0],
  [sStart(3) - 2, 1.07, 0, 0],
  [sStart(3) + 12, 1.85, 0, 0],
  [sEnd(3) - 2, 1.95, 0, 0],
  [sStart(4) + 15, 1, 0, 0],
  [sStart(5), 1, 0, 0],
  [sStart(5) + 14, 0.62, 0, -178],
  [sStart(6), 0.62, 0, -178],
  [sStart(6) + 14, 1, 0, 0],
  [TOTAL, 1, 0, 0],
];
export const camera = (f: number) => {
  for (let i = 0; i < CAM.length - 1; i++) {
    const [a, s0, x0, y0] = CAM[i];
    const [b, s1, x1, y1] = CAM[i + 1];
    if (f <= b) {
      const t = easeInOut(lin(f, a, b));
      return { s: mix(s0, s1, t), tx: mix(x0, x1, t), ty: mix(y0, y1, t) };
    }
  }
  return { s: 1, tx: 0, ty: 0 };
};
/** Local stage point -> screen pixels. */
export const toScreen = (f: number, x: number, y: number) => {
  const c = camera(f);
  return { x: EARTH.cx + c.tx + c.s * x, y: EARTH.cy + c.ty + c.s * y, s: c.s };
};

export const burst = (f: number, at: number, len = 20) => {
  const t = lin(f, at, at + len);
  return t <= 0 || t >= 1 ? null : { r: easeOut(t), o: 1 - t };
};
