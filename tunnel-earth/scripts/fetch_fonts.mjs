// Download the Cairo font files that @remotion/google-fonts points to, so the render
// browser can load them from public/ (it cannot reach fonts.gstatic.com in this container).
import { getInfo } from "@remotion/google-fonts/Cairo";
import { execFileSync } from "node:child_process";
import fs from "node:fs";

const info = getInfo();
const subsets = ["arabic", "latin"];
const files = [];
fs.mkdirSync("public/fonts", { recursive: true });
for (const subset of subsets) {
  const url = info.fonts.normal["900"][subset];
  const file = `fonts/cairo-${subset}.woff2`;
  execFileSync("curl", ["-sSfL", "-o", `public/${file}`, url]);
  files.push({ subset, file, unicodeRange: info.unicodeRanges[subset], source: url });
}
fs.writeFileSync("src/data/fonts.json", JSON.stringify({ family: info.fontFamily, version: info.version, weights: "200 1000", files }, null, 2));
console.log(files.map((f) => `${f.file} (${fs.statSync("public/" + f.file).size} bytes)`).join("\n"));
