import React from "react";
import MaterialIcon from "./MaterialIcon";
import { team } from "../data";
import { useFadeIn } from "../hooks/useFadeIn";
import {FaEnvelope, FaGithub, FaGlobe, FaLinkedin} from "react-icons/fa";

const Team: React.FC = () => {
  const { title, subtitle, members } = team;

  return (
    <section
      id="team"
      className="py-16 md:py-24 lg:py-32 bg-surface-container-low"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-12">
        {/* Header */}
        <div className="text-center mb-10 md:mb-20">
          <h2 className="section-title text-2xl md:text-3xl lg:text-4xl mb-3 md:mb-4">
            {title}
          </h2>
          <p className="text-on-surface-variant text-sm md:text-base">
            {subtitle}
          </p>
        </div>

        {/* Team Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 md:gap-6">
          {members.map((member, i) => {
            const ref = useFadeIn(i * 250);
            return (
              <div ref={ref}>
                <div
                  key={member.name}
                  className="group team-card bg-surface rounded-lg md:rounded-xl overflow-hidden shadow-sm
                             border border-outline-variant/10 transition-all duration-300
                             hover:-translate-y-1 hover:shadow-xl hover:shadow-primary/10"
                >
                  <div className="p-4 md:p-6 flex flex-col items-center text-center">
                    <div className="flex justify-center mb-4 md:mb-6">
                      <img
                        src={member.headshot}
                        alt={`${member.name}'s headshot`}
                        className="w-20 h-20 md:w-32 md:h-32 rounded-xl group-hover:rounded-2xl object-cover shadow-sm border border-outline-variant/20 transition-all duration-300"
                      />
                    </div>
                    <div className="text-[10px] md:text-xs font-bold text-primary tracking-widest uppercase mb-1">
                      {member.role}
                    </div>
                    <h3 className="font-headline text-sm md:text-lg font-bold mb-3 md:mb-4">
                      {member.name}
                    </h3>
                    <div className="flex gap-3 md:gap-4 justify-center">
                      {member.github && (
                        <a href={member.github} target="_blank" rel="noopener noreferrer"
                           aria-label={`${member.name}'s GitHub profile`}>
                          <FaGithub className="w-5 h-5 text-on-surface-variant text-lg md:text-xl cursor-pointer hover:text-primary transition-colors" aria-hidden="true" />
                        </a>
                      )}
                      {member.linkedin && (
                        <a
                          href={member.linkedin}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label={`${member.name}'s LinkedIn profile`}
                        >
                          <FaLinkedin className="w-5 h-5 text-on-surface-variant text-lg md:text-xl cursor-pointer hover:text-primary transition-colors" aria-hidden="true" />
                        </a>
                      )}
                      {member.email && (
                        <a
                          href={`mailto:${member.email}`}
                          aria-label={`Email ${member.name}`}
                        >
                          <FaEnvelope className="w-5 h-5 text-on-surface-variant text-lg md:text-xl cursor-pointer hover:text-primary transition-colors" aria-hidden="true" />
                        </a>
                      )}
                      {member.portfolio && (
                        <a
                          href={member.portfolio}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label={`${member.name}'s portfolio`}
                        >
                          <FaGlobe className="w-5 h-5 text-on-surface-variant text-lg md:text-xl cursor-pointer hover:text-primary transition-colors" aria-hidden="true" />
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default Team;
