import type { Config } from "tailwindcss";

/**
 * The single source of truth for the palette is CSS variables in
 * app/globals.css (:root and [data-theme="dark"]). Tailwind reads those
 * vars so utility classes and the design tokens never drift apart.
 *
 * To change the accent colour for the whole site, edit ONE value:
 *   --ink in app/globals.css
 */
const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}", "./lib/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: "var(--bg)",
        "bg-2": "var(--bg-2)",
        paper: "var(--paper)",
        ink: "var(--ink)",
        text: "var(--text)",
        muted: "var(--muted)",
        faint: "var(--faint)",
        line: "var(--line)",
      },
      fontFamily: {
        sans: ["var(--font-inter)", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "monospace"],
      },
      maxWidth: { content: "1200px" },
    },
  },
  plugins: [],
};
export default config;
