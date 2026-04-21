import React from "react";
import MaterialIcon from "./MaterialIcon";
import { resources as resourcesData } from "../data";
import { useFadeIn } from "../hooks/useFadeIn";

const Documentation: React.FC = () => {
  const { title, description, resources, image } = resourcesData;

  return (
    <section id="documentation" className="py-16 md:py-24 lg:py-32 bg-surface">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-12">
        <div className="bg-surface-container-highest rounded-xl md:rounded-2xl p-6 md:p-12 relative overflow-hidden">
          <div className="relative z-10 grid lg:grid-cols-2 gap-8 lg:gap-16 items-center">
            {/* Left Content */}
            <div>
              <h2 className="section-title text-2xl md:text-3xl lg:text-4xl mb-4 md:mb-6">
                {title}
              </h2>
              <p className="text-on-surface-variant mb-6 md:mb-10 leading-relaxed text-sm md:text-base">
                {description}
              </p>

              <div className="space-y-3 md:space-y-4">
                {resources.map((resource, i) => {
                  const ref = useFadeIn(i * 250);
                  return (
                    <div ref={ref} key={resource.label}>
                      <a
                        href={resource.href}
                        target={resource.external ? "_blank" : undefined}
                        rel={
                          resource.external ? "noopener noreferrer" : undefined
                        }
                        className="flex items-center justify-between p-4 md:p-6 bg-surface rounded-lg md:rounded-xl
                        group hover:shadow-md transition-all card-hover"
                      >
                        <div className="flex items-center gap-3 md:gap-4">
                          <MaterialIcon
                            icon={resource.icon}
                            className="text-primary text-lg md:text-xl"
                          />
                          <span className="font-headline font-bold text-sm md:text-base">
                            {resource.label}
                          </span>
                        </div>
                        <MaterialIcon
                          icon="arrow_forward"
                          className="group-hover:translate-x-1 transition-transform text-lg md:text-xl"
                        />
                      </a>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Right Content - Image */}
            <div className="hidden lg:block relative">
              <div className="absolute inset-0 bg-primary/5 rounded-full blur-3xl"></div>
              <img
                alt={image.alt}
                className="relative rounded-2xl shadow-2xl border border-outline-variant/10"
                src={image.src}
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Documentation;
