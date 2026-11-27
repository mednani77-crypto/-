import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";
import fonts from "./data/fonts.json";

/**
 * Cairo (variable, weights 200–1000) from @remotion/google-fonts.
 * scripts/fetch_fonts.mjs downloads the exact files that @remotion/google-fonts/Cairo lists
 * into public/fonts, because the render browser in this container cannot reach fonts.gstatic.com.
 */
export const CAIRO = fonts.family;
for (const f of fonts.files) {
  loadFont({
    family: fonts.family,
    url: staticFile(f.file),
    weight: fonts.weights,
    unicodeRange: f.unicodeRange,
    format: "woff2",
  });
}

export const W = 1080;
export const H = 1920;

/**
 * Safe area: nothing important in the top 8%, the bottom 18% or the right 12%.
 * The left edge keeps the usual 80px video margin.
 */
export const SAFE = {
  top: Math.round(H * 0.08), // 154
  bottom: Math.round(H * (1 - 0.18)), // 1574
  right: Math.round(W * (1 - 0.12)), // 950
  left: 80,
};
export const SAFE_W = SAFE.right - SAFE.left; // 870
export const SAFE_CX = (SAFE.left + SAFE.right) / 2; // 515

export const C = {
  bg0: "#02061A",
  bg1: "#0A1A42",
  ink: "#01040D",
  cyan: "#38E8FF",
  cyanSoft: "#8BF4FF",
  cyanDeep: "#119FCB",
  ice: "#E6FDFF",
  orange: "#FF9B3D",
  orangeHot: "#FFC27A",
  white: "#F3FAFF",
  muted: "#9CC4D6",
};

/** Earth cross-section, in screen pixels at camera scale 1. */
export const EARTH = {
  cx: SAFE_CX,
  cy: 836,
  R: 330,
  // real proportions: core 3480 km, inner core 1220 km, of 6371 km
  outerCore: 0.546,
  innerCore: 0.19,
  crust: 14, // drawn thicker than reality so it is visible
};

export const DOT_REST = { x: 44, y: -EARTH.R - 17 }; // standing on the surface, beside the tunnel mouth

/** Physics shown on screen (uniform-density Earth). */
export const PHYS = {
  vMaxKmS: 7.9, // sqrt(g R) = sqrt(9.81 * 6.371e6) ≈ 7.9 km/s
  halfPeriodMin: 42, // pi * sqrt(R / g) ≈ 42.2 min
};

export const orangeDigits = /([٠-٩0-9][٠-٩0-9:.,]*)/;
