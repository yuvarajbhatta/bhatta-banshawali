// Mirrors the --duration-*/--ease-* tokens in app/globals.css so Framer
// Motion and GSAP animations move at the same pace as plain CSS
// transitions. Keep these two files in sync if the tokens change.

export const DURATION = {
  fast: 0.15,
  base: 0.3,
  slow: 0.6,
  slower: 1,
} as const;

// Framer Motion easing arrays (cubic-bezier control points) matching the
// CSS cubic-bezier() strings. Typed as plain mutable tuples (not `as
// const`) since that's what Framer Motion's Easing type expects.
export const EASE_OUT: [number, number, number, number] = [0.16, 1, 0.3, 1];
export const EASE_IN_OUT: [number, number, number, number] = [0.65, 0, 0.35, 1];

// Same curves as CSS-friendly strings, for GSAP (which accepts this
// format directly as its `ease` option).
export const GSAP_EASE_OUT = "cubic-bezier(0.16, 1, 0.3, 1)";
export const GSAP_EASE_IN_OUT = "cubic-bezier(0.65, 0, 0.35, 1)";
