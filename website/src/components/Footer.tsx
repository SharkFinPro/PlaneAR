import React from "react";
import { siteConfig, navigation, links } from "../data";

const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();
  const displayYear = siteConfig.year || currentYear;

  return (
    <footer className="bg-surface-container-low border-t border-outline-variant/5">
      <div className="h-px bg-gradient-to-r from-transparent via-outline-variant/40 to-transparent" />

      <div className="flex flex-col md:flex-row justify-between items-center px-4 sm:px-6 md:px-12 py-10 md:py-16 max-w-7xl mx-auto gap-4 md:gap-8">
        <div className="text-center md:text-left">
          <div
            className="font-headline font-bold text-lg md:text-xl mb-2"
            style={{
              background: "linear-gradient(135deg, var(--color-primary) 0%, var(--color-tertiary) 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            {siteConfig.name}
          </div>
          <p className="font-body text-[10px] md:text-xs tracking-widest uppercase text-on-surface-variant">
            © {displayYear} {siteConfig.name} Capstone. Developed for University
            Engineering Excellence.
          </p>
        </div>

        <div className="flex gap-4 md:gap-8 flex-wrap justify-center">
          {navigation.footer.map((link) => (
            <a
              key={link.label}
              href={link.href}
              target={link.external ? "_blank" : undefined}
              rel={link.external ? "noopener noreferrer" : undefined}
              className="font-body text-[10px] md:text-xs tracking-widest uppercase text-on-surface-variant hover:text-primary link-underline"
            >
              {link.label}
            </a>
          ))}
        </div>
      </div>
    </footer>
  );
};

export default Footer;
