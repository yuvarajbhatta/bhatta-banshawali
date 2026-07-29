"use client";

import { useEffect, useState } from "react";

export type SceneQuality = "low" | "high";

/**
 * Lets a scene reduce its own node/particle count and effects on small
 * viewports, instead of running the same complexity on a phone as on a
 * desktop. Re-evaluates on resize/orientation change.
 */
export function useSceneQuality(): SceneQuality {
  const [quality, setQuality] = useState<SceneQuality>("high");

  useEffect(() => {
    const mediaQuery = window.matchMedia("(max-width: 640px)");
    const update = () => setQuality(mediaQuery.matches ? "low" : "high");
    update();
    mediaQuery.addEventListener("change", update);
    return () => mediaQuery.removeEventListener("change", update);
  }, []);

  return quality;
}
