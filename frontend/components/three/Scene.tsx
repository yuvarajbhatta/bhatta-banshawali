"use client";

import { Canvas, type CanvasProps } from "@react-three/fiber";
import { useReducedMotion } from "framer-motion";
import { Suspense, type ReactNode } from "react";
import styles from "./Scene.module.css";

interface SceneProps {
  children: ReactNode;
  className?: string;
  /** Rendered instead of the live Canvas when the user prefers reduced motion. */
  fallback?: ReactNode;
  camera?: CanvasProps["camera"];
}

/**
 * Shared entry point for every R3F scene in the app (landing hero,
 * dashboard family constellation) -- one place to keep dpr capping,
 * the reduced-motion fallback, and Suspense wiring consistent instead
 * of repeating this setup per scene. See useSceneQuality for the
 * companion viewport-based node/particle-count scaling each scene
 * applies to its own content.
 */
export function Scene({ children, className, fallback, camera }: SceneProps) {
  const shouldReduceMotion = useReducedMotion();

  if (shouldReduceMotion) {
    return <div className={[styles.fallback, className].filter(Boolean).join(" ")}>{fallback}</div>;
  }

  return (
    <Canvas
      className={className}
      dpr={[1, 1.75]}
      gl={{ antialias: true, alpha: true }}
      camera={camera ?? { position: [0, 0, 10], fov: 45 }}
    >
      <Suspense fallback={null}>{children}</Suspense>
    </Canvas>
  );
}
