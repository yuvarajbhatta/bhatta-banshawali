"use client";

import { Children } from "react";
import { motion, useReducedMotion, type Variants } from "framer-motion";
import type { ReactNode } from "react";
import { DURATION, EASE_OUT } from "@/lib/motion";

interface ScrollRevealProps {
  children: ReactNode;
  className?: string;
  stagger?: number;
}

const itemVariants: Variants = {
  hidden: { opacity: 0, y: 40 },
  visible: { opacity: 1, y: 0, transition: { duration: DURATION.slow, ease: EASE_OUT } },
};

/**
 * Staggered reveal for sections with several elements that should animate
 * in together as the section scrolls into view (e.g. a stat row or feature
 * grid) -- for a single element, use the simpler Reveal instead. Framer
 * Motion's own `staggerChildren`, not a second animation library (this
 * used to be GSAP ScrollTrigger -- consolidated so the app only ships one
 * animation library, not two solving the same "reveal on scroll" problem).
 */
export function ScrollReveal({ children, className, stagger = 0.15 }: ScrollRevealProps) {
  const shouldReduceMotion = useReducedMotion();

  if (shouldReduceMotion) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      className={className}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: "-80px" }}
      variants={{ visible: { transition: { staggerChildren: stagger } } }}
    >
      {Children.map(children, (child) => (
        <motion.div variants={itemVariants}>{child}</motion.div>
      ))}
    </motion.div>
  );
}
