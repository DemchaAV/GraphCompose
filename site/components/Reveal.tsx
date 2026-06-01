"use client";
import { useEffect, useRef, useState, type ElementType, type ReactNode } from "react";

/** Fades children up when scrolled into view. Honours prefers-reduced-motion. */
export default function Reveal({
  children,
  as: Tag = "div",
  className = "",
  ...rest
}: {
  children: ReactNode;
  as?: ElementType;
  className?: string;
  [key: string]: unknown;
}) {
  const ref = useRef<HTMLElement>(null);
  const [shown, setShown] = useState(false);

  useEffect(() => {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduce || !("IntersectionObserver" in window)) {
      setShown(true);
      return;
    }
    const el = ref.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            setShown(true);
            io.unobserve(e.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -8% 0px" }
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <Tag ref={ref as never} className={`rv ${shown ? "in" : ""} ${className}`.trim()} {...rest}>
      {children}
    </Tag>
  );
}
