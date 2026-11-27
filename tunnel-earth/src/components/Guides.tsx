import React from "react";
import { AbsoluteFill } from "remotion";
import { H, SAFE, W } from "../theme";

/** Review overlay only (never in the final video): the areas that must stay clear. */
export const Guides: React.FC = () => {
  const red = "rgba(255,40,80,0.28)";
  const line = "2px dashed rgba(255,60,100,0.95)";
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <div style={{ position: "absolute", left: 0, top: 0, width: W, height: SAFE.top, background: red, borderBottom: line }} />
      <div style={{ position: "absolute", left: 0, top: SAFE.bottom, width: W, height: H - SAFE.bottom, background: red, borderTop: line }} />
      <div style={{ position: "absolute", left: SAFE.right, top: 0, width: W - SAFE.right, height: H, background: red, borderLeft: line }} />
      <div style={{ position: "absolute", left: SAFE.left, top: 0, width: 0, height: H, borderLeft: "2px dotted rgba(255,200,0,0.8)" }} />
    </AbsoluteFill>
  );
};
