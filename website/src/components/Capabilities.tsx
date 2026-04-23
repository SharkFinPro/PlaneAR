import React from "react";
import MaterialIcon from "./MaterialIcon";
import {capabilities, CapabilityCard} from "../data";
import { useFadeIn } from "../hooks/useFadeIn";

const Capabilities: React.FC = () => {
  const { title, subtitle, cards } = capabilities;

  return (
    <section className="py-16 md:py-24 lg:py-32 bg-surface">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-12">
        {/* Header */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 md:mb-16 gap-4 md:gap-6">
          <div>
            <h2 className="section-title text-2xl md:text-3xl lg:text-4xl mb-3 md:mb-4">
              {title}
            </h2>
            <p className="text-on-surface-variant max-w-xl text-sm md:text-base">
              {subtitle}
            </p>
          </div>
          <div className="flex gap-4">
            <div className="w-20 md:w-32 h-1 bg-primary"></div>
            <div className="w-12 md:w-16 h-1 bg-surface-container-highest"></div>
          </div>
        </div>

        {/* Capabilities Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4">
          {cards.map((card: CapabilityCard, i: number) => {
            const ref = useFadeIn(i * 250);
            return (
              <div ref={ref} key={card.title} className="h-full">
                <div className="group h-full p-5 md:p-8 bg-surface rounded-xl border border-outline-variant/30
                      shadow-sm hover:border-primary/50 hover:shadow-lg hover:shadow-primary/10
                      transition-all duration-300 relative overflow-hidden">
                  <span className="absolute top-4 right-5 text-[10px] font-mono font-bold tracking-widest
                         text-primary/20 group-hover:text-primary/40 transition-colors select-none">
                    {String(i + 1).padStart(2, '0')}
                  </span>
                  <div className="text-primary mb-4 md:mb-6">
                    <MaterialIcon icon={card.icon} className="text-3xl md:text-4xl icon-pop" />
                  </div>
                  <h4 className="font-headline font-bold text-base md:text-lg mb-2">{card.title}</h4>
                  <p className="text-xs md:text-sm text-on-surface-variant leading-relaxed">{card.description}</p>
                </div>
              </div>
            );
          })}

        </div>
      </div>
    </section>
  );
};

export default Capabilities;
