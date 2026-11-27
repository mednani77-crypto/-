import React from "react";
import { AbsoluteFill } from "remotion";
import { Background } from "./components/Background";
import { Stage } from "./components/Stage";
import { StageLabels } from "./components/StageLabels";
import { Hud } from "./components/Hud";
import { Titles } from "./components/Titles";
import { Subtitles } from "./components/Subtitles";
import { Soundtrack } from "./components/Soundtrack";
import { Guides } from "./components/Guides";

export type TunnelEarthProps = {
  guides: boolean;
};

export const TunnelEarth: React.FC<TunnelEarthProps> = ({ guides }) => {
  return (
    <AbsoluteFill style={{ backgroundColor: "#02061A", overflow: "hidden" }}>
      <Background />
      <Stage />
      <StageLabels />
      <Hud />
      <Titles />
      <Subtitles />
      <Soundtrack />
      {guides ? <Guides /> : null}
    </AbsoluteFill>
  );
};
