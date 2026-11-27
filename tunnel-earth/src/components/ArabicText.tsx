import React from "react";
import { C } from "../theme";

const DIGITS = /([٠-٩0-9][٠-٩0-9:.,٫]*)/g;

/** Arabic text with every number painted orange. */
export const ArabicText: React.FC<{ text: string; numberGlow?: boolean }> = ({ text, numberGlow = true }) => {
  const parts = text.split(DIGITS);
  return (
    <>
      {parts.map((p, i) =>
        /^[٠-٩0-9]/.test(p) ? (
          <span
            key={i}
            style={{
              color: C.orange,
              textShadow: numberGlow ? `0 0 18px rgba(255,155,61,0.65), 0 0 2px rgba(255,155,61,0.9)` : undefined,
            }}
          >
            {p}
          </span>
        ) : (
          <React.Fragment key={i}>{p}</React.Fragment>
        ),
      )}
    </>
  );
};
