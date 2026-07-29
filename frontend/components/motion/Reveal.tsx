"use client";

import { motion, useReducedMotion, type Variants } from "framer-motion";
import type { ReactNode } from "react";
import { DURATION, EASE_OUT } from "@/lib/motion";

interface RevealProps {
  children: ReactNode;
  delay?: number;
  className?: string;
  /** Horizontal offset instead of the default upward reveal -- useful for side-by-side content. */
  direction?: "up" | "left" | "right";
}

const OFFSETS: Record<NonNullable<RevealProps["direction"]>, { x?: number; y?: number }> = {
  up: { y: 24 },
  left: { x: -24 },
  right: { x: 24 },
};

/**
 * Fade/slide-in-on-scroll wrapper used throughout the marketing and
 * dashboard pages instead of one-off Framer Motion setups per component.
 * Honors prefers-reduced-motion by skipping the animation entirely
 * (content is simply visible, no motion).
 */
export function Reveal({ children, delay = 0, className, direction = "up" }: RevealProps) {
  const shouldReduceMotion = useReducedMotion();
  const offset = OFFSETS[direction];

  const variants: Variants = {
    hidden: { opacity: 0, ...offset },
    visible: { opacity: 1, x: 0, y: 0 },
  };

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      className={className}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: "-80px" }}
      variants={variants}
      transition={{ duration: DURATION.slow, delay, ease: EASE_OUT }}
    >
      {children}
    </motion.div>
  );
}
