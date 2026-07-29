"use client";

import { useLayoutEffect, useRef, type ReactNode } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { useReducedMotion } from "framer-motion";

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger);
}

interface ScrollRevealProps {
  children: ReactNode;
  className?: string;
  /** Selector (relative to this wrapper) for the elements to stagger in. Defaults to direct children. */
  targets?: string;
  stagger?: number;
}

/**
 * GSAP ScrollTrigger-driven staggered reveal, for sections with several
 * elements that should animate in together as the section scrolls into
 * view (e.g. a stat row or feature grid) -- for a single element, use
 * the simpler Framer Motion Reveal instead.
 */
export function ScrollReveal({ children, className, targets = ":scope > *", stagger = 0.15 }: ScrollRevealProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const shouldReduceMotion = useReducedMotion();

  useLayoutEffect(() => {
    const container = containerRef.current;
    if (shouldReduceMotion || !container) {
      return;
    }

    const ctx = gsap.context(() => {
      const elements = container.querySelectorAll(targets);
      gsap.set(elements, { opacity: 0, y: 40 });
      gsap.to(elements, {
        opacity: 1,
        y: 0,
        duration: 0.8,
        ease: "power3.out",
        stagger,
        scrollTrigger: {
          trigger: container,
          start: "top 80%",
          once: true,
        },
      });
    }, container);

    return () => ctx.revert();
  }, [shouldReduceMotion, targets, stagger]);

  return (
    <div ref={containerRef} className={className}>
      {children}
    </div>
  );
}
