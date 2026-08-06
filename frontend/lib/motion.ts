// Mirrors the --duration-*/--ease-* tokens in app/globals.css so Framer
// Motion animations move at the same pace as plain CSS transitions. Keep
// these two files in sync if the tokens change.

export const DURATION = {
  fast: 0.15,
  base: 0.3,
  slow: 0.6,
  slower: 1,
} as const;

// Framer Motion easing array (cubic-bezier control points) matching the
// CSS cubic-bezier() string. Typed as a plain mutable tuple (not `as
// const`) since that's what Framer Motion's Easing type expects.
export const EASE_OUT: [number, number, number, number] = [0.16, 1, 0.3, 1];
