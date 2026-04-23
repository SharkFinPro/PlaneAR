import React from "react";
import { siteConfig } from "../data";

const Seo: React.FC = () => {
  const { seo, theme, url, name } = siteConfig;

  return (
    <>
      <title>{seo.title}</title>
      <meta name="description" content={seo.description} />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <meta charSet="utf-8" />
      <meta name="theme-color" content={theme.themeColor} />

      {/* Open Graph */}
      <meta property="og:title" content={seo.title} />
      <meta property="og:description" content={seo.description} />
      <meta property="og:type" content={seo.ogType} />
      <meta property="og:url" content={url} />

      {/* Twitter */}
      <meta name="twitter:card" content={seo.twitterCard} />
      <meta name="twitter:title" content={seo.title} />
      <meta name="twitter:description" content={seo.description} />

      <link rel="canonical" href={url} />
    </>
  );
};

export default Seo;
