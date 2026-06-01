"use client";
import { useEffect, useState } from "react";

export default function TopBar() {
  const [theme, setTheme] = useState<"light" | "dark">("light");
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const initial = (document.documentElement.getAttribute("data-theme") as "light" | "dark") || "light";
    setTheme(initial);
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  function toggle() {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.setAttribute("data-theme", next);
    try { localStorage.setItem("gc-theme", next); } catch {}
    window.dispatchEvent(new CustomEvent("gc-theme", { detail: next }));
  }

  return (
    <header className={"topbar" + (scrolled ? " scrolled" : "")}>
      <div className="wrap">
        <a className="brand" href="#top" aria-label="GraphCompose home">
          <span className="mark" aria-hidden="true" />
          GraphCompose
        </a>
        <nav className="navlinks" aria-label="Sections">
          <a href="#playground">Playground</a>
          <a href="#how">How it works</a>
          <a href="#templates">Templates</a>
          <a href="#positioning">Positioning</a>
          <a href="#engineering">Engineering</a>
        </nav>
        <div className="topbar-actions">
          <button className="iconbtn" onClick={toggle} aria-label="Toggle dark mode" title="Toggle theme">
            {theme === "dark" ? (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z" strokeLinejoin="round" />
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <circle cx="12" cy="12" r="4.2" />
                <path d="M12 2v2.5M12 19.5V22M4.2 4.2l1.8 1.8M18 18l1.8 1.8M2 12h2.5M19.5 12H22M4.2 19.8 6 18M18 6l1.8-1.8" strokeLinecap="round" />
              </svg>
            )}
          </button>
          <a className="iconbtn" href="https://github.com/demchaav/graph-compose" target="_blank" rel="noopener" aria-label="GitHub repository" title="GitHub">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 1.5A10.5 10.5 0 0 0 8.68 22c.52.1.71-.23.71-.5v-1.9c-2.9.63-3.52-1.24-3.52-1.24-.48-1.2-1.16-1.53-1.16-1.53-.95-.65.07-.64.07-.64 1.05.08 1.6 1.08 1.6 1.08.94 1.6 2.46 1.14 3.06.87.1-.68.37-1.14.66-1.4-2.31-.26-4.74-1.16-4.74-5.14 0-1.13.4-2.06 1.07-2.79-.11-.26-.46-1.32.1-2.75 0 0 .87-.28 2.85 1.06a9.9 9.9 0 0 1 5.18 0c1.98-1.34 2.85-1.06 2.85-1.06.56 1.43.21 2.49.1 2.75.67.73 1.07 1.66 1.07 2.79 0 3.99-2.43 4.87-4.75 5.13.38.32.71.95.71 1.92v2.85c0 .27.19.61.72.5A10.5 10.5 0 0 0 12 1.5Z" />
            </svg>
          </a>
        </div>
      </div>
    </header>
  );
}
