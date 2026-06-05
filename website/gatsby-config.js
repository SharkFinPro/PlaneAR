/**
 * @type {import('gatsby').GatsbyConfig}
 */
module.exports = {
  siteMetadata: {
    title: `PlaneAR | Augmented Reality Flight Tracking`,
    description: `A Senior Capstone Project bridging aerospace data with mobile AR technology to visualize real-time aircraft vectors in the physical world.`,
    author: `@planear`,
    siteUrl: `https://planear.dev`,
  },
  plugins: [
    `gatsby-plugin-typescript`,
    `gatsby-plugin-postcss`,
    {
      resolve: `gatsby-plugin-manifest`,
      options: {
        name: `PlaneAR`,
        short_name: `PlaneAR`,
        start_url: `/`,
        background_color: `#f7fafc`,
        theme_color: `#006e1c`,
        display: `minimal-ui`,
        icon: `src/images/icon.svg`,
      },
    },
  ],
};
