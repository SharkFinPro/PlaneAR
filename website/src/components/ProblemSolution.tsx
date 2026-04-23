import React, { useEffect, useRef, useState } from "react";
import MaterialIcon from "./MaterialIcon";
import { problemSolution } from "../data";

const iconColors: Record<string, string> = {
  problem: "text-tertiary",
  solution: "text-primary",
  methodology: "",
};

const TypewriterQuote: React.FC<{ text: string }> = ({ text }) => {
  const [displayed, setDisplayed] = useState("");
  const [started, setStarted] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) { setStarted(true); obs.disconnect(); }
    }, { threshold: 0.6 });
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  useEffect(() => {
    if (!started) return;
    let i = 0;
    const interval = setInterval(() => {
      setDisplayed(text.slice(0, i + 1));
      i++;
      if (i >= text.length) clearInterval(interval);
    }, 28);
    return () => clearInterval(interval);
  }, [started, text]);

  return (
    <div
      ref={ref}
      className="p-4 md:p-8 bg-surface-container-highest rounded-lg md:rounded-xl border-l-4 border-primary"
    >
      <p className="text-sm font-medium italic text-on-surface min-h-[3em]">
        "{displayed}
        <span className="animate-pulse not-italic font-light text-primary">
          {displayed.length < text.length ? "▋" : ""}
        </span>"
      </p>
    </div>
  );
};

const ProblemSolution: React.FC = () => {
  const { title, description, quote, cards } = problemSolution;
  const [problemCard, solutionCard, methodologyCard] = cards;

  const sectionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const cards = sectionRef.current?.querySelectorAll(".trace-border");
    if (!cards) return;
    const obs = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        cards.forEach((card, i) => {
          setTimeout(() => card.classList.add("traced"), i * 200);
        });
        obs.disconnect();
      }
    }, { threshold: 0.4 });
    if (sectionRef.current) obs.observe(sectionRef.current);
    return () => obs.disconnect();
  }, []);

  return (
    <section
      ref={sectionRef}
      id="challenge"
      className="py-16 md:py-24 lg:py-32 bg-surface-container-low"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-12">
        <div className="grid lg:grid-cols-3 gap-8 lg:gap-12">
          {/* Left Column - Title */}
          <div className="lg:col-span-1">
            <h2 className="section-title text-2xl md:text-3xl lg:text-4xl mb-4 md:mb-6">
              {title}
            </h2>
            <p className="text-on-surface-variant mb-6 md:mb-8 leading-relaxed text-sm md:text-base">
              {description}
            </p>
            <TypewriterQuote text={quote.text} />
          </div>

          {/* Right Column - Cards */}
          <div className="lg:col-span-2 grid sm:grid-cols-2 gap-4 md:gap-8">
            {/* Problem Card */}
            <div className="bg-surface p-6 md:p-10 rounded-lg md:rounded-xl shadow-sm border border-outline-variant/5 trace-border">
              <MaterialIcon
                icon={problemCard.icon}
                className={`${iconColors[problemCard.variant || ""]} text-3xl md:text-4xl mb-4 md:mb-6`}
              />
              <h3 className="font-headline text-lg md:text-xl font-bold mb-3 md:mb-4">
                {problemCard.title}
              </h3>
              <p className="text-on-surface-variant text-xs md:text-sm leading-relaxed">
                {problemCard.description}
              </p>
            </div>

            {/* Solution Card */}
            <div className="bg-surface p-6 md:p-10 rounded-lg md:rounded-xl shadow-sm border border-outline-variant/5 trace-border">
              <MaterialIcon
                icon={solutionCard.icon}
                className={`${iconColors[solutionCard.variant || ""]} text-3xl md:text-4xl mb-4 md:mb-6`}
              />
              <h3 className="font-headline text-lg md:text-xl font-bold mb-3 md:mb-4">
                {solutionCard.title}
              </h3>
              <p className="text-on-surface-variant text-xs md:text-sm leading-relaxed">
                {solutionCard.description}
              </p>
            </div>

            {/* Methodology Card - Full Width */}
            <div className="bg-surface p-6 md:p-10 rounded-lg md:rounded-xl shadow-sm border border-outline-variant/5 sm:col-span-2 trace-border">
              <div className="flex flex-col md:flex-row items-start md:items-center gap-6 md:gap-12">
                <div className="flex-1">
                  <h3 className="font-headline text-lg md:text-xl font-bold mb-3 md:mb-4">
                    {methodologyCard.title}
                  </h3>
                  <p className="text-on-surface-variant text-xs md:text-sm leading-relaxed">
                    {methodologyCard.description}
                  </p>
                </div>
                <img
                  alt={methodologyCard.image?.alt || ""}
                  className="w-full md:w-48 h-32 md:h-40 object-cover rounded-lg"
                  src={methodologyCard.image?.src}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default ProblemSolution;
