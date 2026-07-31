import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// @testing-library/react only auto-registers its afterEach(cleanup) when it
// detects Jest's global `afterEach` -- Vitest doesn't expose test globals by
// default (test.globals isn't set), so without this, DOM trees from earlier
// tests in the same file leak into later ones.
afterEach(cleanup);
