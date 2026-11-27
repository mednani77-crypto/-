import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { camera } from "../lib/state";

/** While the camera is zoomed into the Earth, darken the top so the headline stays readable. */
export const TopScrim: React.FC = () => {
  const f = useCurrentFrame();
  const o = Math.max(0, Math.min(1, (camera(f).s - 1.1) / 0.5));
  if (o <= 0) return null;
  return (
    <AbsoluteFill
      style={{
        opacity: o,
        background: "linear-gradient(180deg, rgba(2,6,26,0.92) 0px, rgba(2,6,26,0.85) 470px, rgba(2,6,26,0) 640px)",
      }}
    />
  );
};
