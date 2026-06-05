# PlaneAR Website

A high-performance, static single-page website for the PlaneAR project, built with Gatsby, React, and Tailwind CSS. This site serves as the primary landing page and informational hub for the PlaneAR augmented reality aircraft tracking system.

## Features

- **Gatsby Framework**: Optimized static site generation for industry-leading load times and SEO.
- **Adaptive Theming**: Built-in Dark/Light mode toggle with `localStorage` persistence for a personalized user experience.
- **Modular Architecture**: Component-based design allowing for easy expansion and maintenance of site sections.
- **Responsive Interface**: A mobile-first approach powered by Tailwind CSS, ensuring seamless viewing across all device sizes.
- **Material Design**: Implementation of the Material 3 color system using custom CSS variables for a modern, cohesive look.

## Development Setup

### Prerequisites

- **Node.js**: Version 24 or higher recommended.
- **npm** or **yarn** package manager.

### Local Installation

1. Navigate to the website directory:
   ```bash
   cd website
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run develop
   ```

4. Access the site:
   Open your browser and navigate to `http://localhost:8000`.

## Build and Deployment

### Production Build
To generate the optimized static production files:

```bash
npm run build
```
The output will be located in the `public/` directory.

### Production Preview
To serve the production build locally for testing:

```bash
npm run serve
```
