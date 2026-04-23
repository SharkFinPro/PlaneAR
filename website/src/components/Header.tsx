import React, { useState, useEffect, useRef, useCallback } from "react";
import ThemeToggle from "./ThemeToggle";
import MaterialIcon from "./MaterialIcon";
import { siteConfig, navigation, links } from "../data";

const Header: React.FC = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [activeLabel, setActiveLabel] = useState("Overview");
  const isClickScrolling = useRef(false);
  const clickScrollTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rafId = useRef<number | null>(null);

  const progressBarRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const sectionIds = navigation.header.map((link) =>
      link.href.replace("#", ""),
    );

    const observers: IntersectionObserver[] = [];

    sectionIds.forEach((id) => {
      const el = document.getElementById(id);
      if (!el) return;

      const observer = new IntersectionObserver(
        ([entry]) => {
          if (isClickScrolling.current) return;
          if (entry.isIntersecting) {
            const match = navigation.header.find(
              (link) => link.href === `#${id}`,
            );
            if (match) setActiveLabel(match.label);
          }
        },
        {
          rootMargin: "-40% 0px -55% 0px",
          threshold: 0,
        },
      );

      observer.observe(el);
      observers.push(observer);
    });

    return () => observers.forEach((o) => o.disconnect());
  }, []);

  const handleClick = (clickedLabel: string) => {
    setActiveLabel(clickedLabel);
    setIsMobileMenuOpen(false);

    isClickScrolling.current = true;

    // unlock after scroll settles
    if (clickScrollTimer.current) clearTimeout(clickScrollTimer.current);

    clickScrollTimer.current = setTimeout(() => {
      isClickScrolling.current = false;
    }, 800);
  };

  useEffect(() => {
    return () => {
      if (clickScrollTimer.current) clearTimeout(clickScrollTimer.current);
      if (rafId.current) cancelAnimationFrame(rafId.current);
    };
  }, []);

  useEffect(() => {
    let raf: number;

    const tick = () => {
      const el = document.documentElement;

      const maxScroll = el.scrollHeight - el.clientHeight;
      const scrolled = maxScroll > 0 ? el.scrollTop / maxScroll : 0;

      const percent = scrolled * 100;

      if (progressBarRef.current) {
        progressBarRef.current.style.width = `${percent}%`;
      }

      raf = requestAnimationFrame(tick);
    };

    raf = requestAnimationFrame(tick);

    return () => cancelAnimationFrame(raf);
  }, []);

  return (
    <header className="bg-surface fixed top-0 left-0 right-0 z-50 border-b border-outline-variant/10">
      <div
        ref={progressBarRef}
        className="absolute bottom-0 left-0 h-[2px] bg-primary"
        style={{ width: "0%" }}
      />

      <nav className="flex justify-between items-center w-full px-4 sm:px-6 md:px-12 py-4 md:py-6 max-w-7xl mx-auto">
        {/* Logo */}
        <div className="text-xl md:text-2xl font-black tracking-tighter text-primary font-headline">
          {siteConfig.name}
        </div>

        {/* Desktop Navigation */}
        <div className="hidden md:flex items-center gap-8">
          {navigation.header.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={() => handleClick(link.label)}
              className={`
                  font-headline tracking-[-0.02em] font-semibold transition-colors duration-300
                  ${
                activeLabel === link.label
                  ? "text-primary border-b-2 border-primary pb-1"
                  : "text-on-surface-variant hover:text-primary"
              }
              `}
            >
              {link.label}
            </a>
          ))}
        </div>

        {/* Right Side Actions */}
        <div className="flex items-center gap-2 md:gap-4">
          <ThemeToggle />
          <a
            href={links.github}
            target="_blank"
            rel="noopener noreferrer"
            className="hidden sm:block bg-primary text-on-primary px-4 md:px-6 py-2 md:py-2.5 rounded font-headline font-bold text-sm md:text-base scale-95 active:scale-100 transition-transform hover:opacity-90"
          >
            View on GitHub
          </a>

          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden w-10 h-10 p-2 rounded-lg bg-surface-container-high hover:bg-surface-container-highest transition-colors duration-200"
            aria-label={isMobileMenuOpen ? "Close menu" : "Open menu"}
            aria-expanded={isMobileMenuOpen}
          >
            <MaterialIcon
              icon={isMobileMenuOpen ? "close" : "menu"}
              className="text-on-surface"
            />
          </button>
        </div>
      </nav>

      {/* Mobile Navigation Drawer */}
      <div
        className={`md:hidden absolute top-full left-0 right-0 bg-surface border-b border-outline-variant/10 shadow-lg transition-all duration-300 ${
          isMobileMenuOpen
            ? "opacity-100 translate-y-0"
            : "opacity-0 -translate-y-2 pointer-events-none"
        }`}
      >
        <div className="px-4 py-4 space-y-2">
          {navigation.header.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={() => handleClick(link.label)}
              className={`
                  block px-4 py-3 rounded-lg font-headline font-semibold transition-colors duration-200
                  ${
                activeLabel === link.label
                  ? "text-primary bg-primary/10"
                  : "text-on-surface-variant hover:text-primary hover:bg-surface-container-high"
              }
              `}
            >
              {link.label}
            </a>
          ))}
          <a
            href={links.github}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-3 mt-2 bg-primary text-on-primary rounded-lg font-headline font-bold sm:hidden"
          >
            <MaterialIcon icon="code" className="text-sm" />
            View on GitHub
          </a>
        </div>
      </div>
    </header>
  );
};

export default Header;
