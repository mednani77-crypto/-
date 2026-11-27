import timing from "../data/timing.json";

export type Chunk = { text: string; startFrame: number; endFrame: number };
export type SceneTiming = {
  id: number;
  text: string;
  audio: string | null;
  voiceSec: number;
  startFrame: number;
  durationFrames: number;
  voStartFrame: number;
  voFrames: number;
  chunks: Chunk[];
};

export const TIMING = timing as unknown as {
  fps: number;
  width: number;
  height: number;
  measured: boolean;
  totalFrames: number;
  music: { file: string; volume: number; durationSec: number } | null;
  whoosh: { file: string; volume: number; durationSec: number; peakSec: number } | null;
  scenes: SceneTiming[];
};

export const FPS = TIMING.fps;
export const TOTAL = TIMING.totalFrames;
export const SCENES = TIMING.scenes;

const sc = (n: number) => SCENES[n - 1];
export const sStart = (n: number) => sc(n).startFrame;
export const sEnd = (n: number) => sc(n).startFrame + sc(n).durationFrames;
export const sMid = (n: number) => Math.round((sStart(n) + sEnd(n)) / 2);
export const voStart = (n: number) => sc(n).voStartFrame;
export const voEnd = (n: number) => sc(n).voStartFrame + sc(n).voFrames;
/** Frame at which chunk k (0-based) of scene n starts being spoken. */
export const chunkAt = (n: number, k: number) => sc(n).chunks[Math.min(k, sc(n).chunks.length - 1)].startFrame + 2;

export const clamp01 = (x: number) => Math.max(0, Math.min(1, x));
export const lin = (f: number, a: number, b: number) => (b <= a ? (f >= b ? 1 : 0) : clamp01((f - a) / (b - a)));
export const easeInOut = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
export const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);
export const easeIn = (t: number) => t * t * t;
export const mix = (a: number, b: number, t: number) => a + (b - a) * t;

/** Opacity of something shown from `a` to `b`, fading in over `fi` and out over `fo` frames. */
export const window01 = (f: number, a: number, b: number, fi = 8, fo = 8) =>
  Math.min(easeOut(lin(f, a, a + fi)), 1 - easeIn(lin(f, b - fo, b)));
