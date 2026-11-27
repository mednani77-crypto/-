import React from "react";
import { Composition } from "remotion";
import { TunnelEarth } from "./TunnelEarth";
import { TIMING } from "./lib/timeline";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="TunnelEarth"
        component={TunnelEarth}
        durationInFrames={TIMING.totalFrames}
        fps={30}
        width={1080}
        height={1920}
        defaultProps={{ guides: false }}
      />
      <Composition
        id="TunnelEarthGuides"
        component={TunnelEarth}
        durationInFrames={TIMING.totalFrames}
        fps={30}
        width={1080}
        height={1920}
        defaultProps={{ guides: true }}
      />
    </>
  );
};
