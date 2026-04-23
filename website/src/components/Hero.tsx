import React, { useRef, useCallback } from "react";
import MaterialIcon from "./MaterialIcon";
import { hero } from "../data";

const HeroImage: React.FC = () => {
  const cardRef = useRef<HTMLDivElement>(null);

  const handleMouseMove = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const el = cardRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;   // -0.5 → 0.5
    const y = (e.clientY - rect.top)  / rect.height - 0.5;
    el.style.transform = `perspective(800px) rotateY(${x * 8}deg) rotateX(${-y * 6}deg) scale(1.02)`;
  }, []);

  const handleMouseLeave = useCallback(() => {
    const el = cardRef.current;
    if (!el) return;
    el.style.transform = `perspective(800px) rotateY(0deg) rotateX(0deg) scale(1)`;
  }, []);

  return (
    <div className="relative order-1 lg:order-2">
      <div
        ref={cardRef}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        style={{ transition: "transform 0.15s ease-out", willChange: "transform" }}
        className="aspect-[4/3] sm:aspect-[4/5] bg-surface-container-low rounded-xl md:rounded-2xl
                   overflow-hidden relative hero-image-shadow border border-outline-variant/10"
      >
        <img alt={hero.image.alt} className="w-full h-full object-cover" src={hero.image.src} />
        <div className="absolute inset-0 bg-gradient-to-t from-background/40 to-transparent" />
        {/* Floating Glass Metric Card — unchanged */}
        <div className="absolute bottom-4 left-4 md:bottom-8 md:left-8 glass-panel p-3 md:p-6 rounded-lg md:rounded-xl shadow-xl max-w-[180px] md:max-w-[240px]">
          <div className="flex items-center gap-2 md:gap-3 mb-1 md:mb-2">
            <div className="live-dot" />
            <span className="text-[10px] md:text-xs font-bold tracking-widest uppercase text-on-surface-variant">
              {hero.floatingCard.label}
            </span>
          </div>
          <div className="text-lg md:text-2xl font-headline font-extrabold text-on-surface">
            {hero.floatingCard.value}
          </div>
          <p className="text-[10px] md:text-xs text-on-surface-variant/80">{hero.floatingCard.description}</p>
        </div>
      </div>
      <div className="absolute -top-6 -right-6 md:-top-12 md:-right-12 w-24 h-24 md:w-48 md:h-48 tech-grid -z-10" />
    </div>
  );
};

const Hero: React.FC = () => {
  return (
    <section
      id="overview"
      className="relative min-h-[auto] lg:min-h-[870px] flex items-center overflow-hidden bg-surface pt-8 md:pt-16 lg:pt-24"
    >
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/20 rounded-full blur-[100px] animate-[drift_8s_ease-in-out_infinite]" />
        <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-tertiary/15 rounded-full blur-[70px] animate-[drift_6s_ease-in-out_infinite_reverse]" />
        <div className="absolute top-1/3 left-1/2 w-80 h-80 bg-primary/10 rounded-full blur-[90px] animate-[drift_10s_ease-in-out_infinite_1s]" />
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-12 grid lg:grid-cols-2 gap-8 lg:gap-16 items-center w-full py-12 md:py-16 lg:py-0">
        {/* Left Content */}
        <div className="z-10 order-2 lg:order-1">
          <span className="inline-block px-3 py-1 md:px-4 md:py-1.5 bg-surface-container-highest text-primary font-label text-[10px] md:text-xs font-bold tracking-[0.1em] uppercase mb-4 md:mb-8 rounded">
            {hero.badge}
          </span>

          <h1 className="font-headline text-3xl sm:text-4xl md:text-5xl lg:text-6xl xl:text-7xl font-extrabold tracking-tight text-on-background leading-[1.1] mb-4 md:mb-8">
            {hero.title} <span className="text-primary">{hero.highlight}</span>
          </h1>

          <p className="text-base md:text-lg lg:text-xl text-on-surface-variant leading-relaxed mb-6 md:mb-10 max-w-xl">
            {hero.description}
          </p>

          <div className="flex flex-col sm:flex-row flex-wrap items-start sm:items-center gap-3 sm:gap-4 md:gap-6">
            <a
              href={hero.primaryButton.href}
              className="btn-primary text-sm md:text-base px-6 py-3 md:px-8 md:py-4"
            >
              <MaterialIcon icon={hero.primaryButton.icon} />
              {hero.primaryButton.label}
            </a>
            <a
              href={hero.secondaryButton.href}
              target={hero.secondaryButton.external ? "_blank" : undefined}
              rel={
                hero.secondaryButton.external
                  ? "noopener noreferrer"
                  : undefined
              }
              className="btn-secondary text-sm md:text-base"
            >
              <MaterialIcon icon={hero.secondaryButton.icon} />
              {hero.secondaryButton.label}
            </a>
          </div>
        </div>

        {/* Right Content - Image */}
        <HeroImage />
      </div>
    </section>
  );
};

export default Hero;
