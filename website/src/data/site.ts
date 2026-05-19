// --- Core Configuration ------------------------------------------------------
export const siteConfig = {
  name: "PlaneAR",
  tagline: "Augmented Reality Flight Tracking",
  url: "https://github.com/SharkFinPro/PlaneAR",
  year: 2026,
  capstoneClass: "Senior Capstone 2026",
  description:
    "A Senior Capstone Project bridging aerospace telemetry with a native mobile AR pipeline that integrates ARCore camera and sensor data to render real-time aircraft vectors in the physical world.",
  theme: {
    defaultTheme: "light" as const,
    themeColor: "#006e1c",
  },
  seo: {
    title: "PlaneAR | Augmented Reality Flight Tracking",
    description:
      "A Senior Capstone Project bridging aerospace telemetry with a native mobile AR pipeline that integrates ARCore camera and sensor data to render real-time aircraft vectors in the physical world.",
    ogType: "website" as const,
    twitterCard: "summary_large_image" as const,
  },
};

// --- Shared Links (used across multiple sections) ----------------------------
export const links = {
  github: "https://github.com/SharkFinPro/PlaneAR",
  download: "#download",
  contact: "https://github.com/SharkFinPro/PlaneAR/issues",
};

// --- Navigation --------------------------------------------------------------
export const navigation = {
  header: [
    { label: "Overview", href: "#overview", active: true },
    { label: "The Problem", href: "#challenge", active: false },
    { label: "Meet The Team", href: "#team", active: false },
  ],
  footer: [
    { label: "Download APK", href: links.download },
    { label: "GitHub Source", href: links.github, external: true },
    { label: "Contact Team", href: links.contact, external: true },
  ],
};

// --- Hero Section ------------------------------------------------------------
export const hero = {
  badge: siteConfig.capstoneClass,
  title: "Augmented Reality",
  highlight: "Flight Tracking.",
  description: siteConfig.description,
  primaryButton: {
    label: "Download APK",
    icon: "download",
    href: links.download,
  },
  secondaryButton: {
    label: "View Source on GitHub",
    icon: "code",
    href: links.github,
    external: true,
  },
  image: {
    src: "https://lh3.googleusercontent.com/aida-public/AB6AXuD1NlJVZe7qs_dZjigSKI78XNIRqFU9M7USuRPkjTI3toZ9PbSpxVRmDOQEa0aa9oVlMFhAtbsfrRkpjUONeRT7I54le1M23RdlS68mMrZ5CV2PbBh-oIOyYmz27KMlKaop71xwHizpk4eql5NA2t4G3sVTM0_Q4AYZ6d9Vc94-bTV1DYh6IBsjZ2gt43psXcuZmmqAsjjleZvhVaRmnl4e1anXxob95JCaGr7E3B0gGHRy6o6vv6ij5hCsDX53uYHoeLrXccpKK3YE",
    alt: "A smartphone showing AR flight tracking overlay on live camera view",
  },
  floatingCard: {
    icon: "location_on",
    label: "Live Tracking",
    value: "FL350",
    description: "Current Altitude Reference",
  },
};

// --- Team Section ------------------------------------------------------------
export const team = {
  title: "The Development Team.",
  subtitle: "Senior Engineering Students, Class of 2026.",
  members: [
    {
      name: "Alexander Martin",
      role: "Engineering",
      linkedin: "https://linkedin.com/in/iamalexmartin/",
      email: "alex.martin.cs@outlook.com",
      portfolio: "https://www.iamalexmartin.com/",
      github: "https://github.com/SharkFinPro"
    },
    {
      name: "Douglas Sandford",
      role: "Engineering",
      email: "sandford@oregonstate.edu",
      github: "https://github.com/eageroden"
    },
    {
      name: "Johnny Vo",
      role: "Engineering",
      email: "vojoh@oregonstate.edu",
      github: "https://github.com/vojoh"
    },
    {
      name: "Kaden Allen",
      role: "Engineering",
      email: "allekade@oregonstate.edu",
      github: "https://github.com/allekade"
    },
    {
      name: "Owen Jones",
      role: "Engineering",
      linkedin: "https://www.linkedin.com/in/owen-jones-ojones28/",
      email: "owenmjones28@gmail.com",
      github: "https://github.com/ojones28"
    },
  ],
};

// --- Capabilities Section ------------------------------------------------------
export const capabilities = {
  title: "Key Capabilities.",
  subtitle: "Everything you need to identify aircraft in real time — just point your phone at the sky.",
  cards: [
    {
      icon: "flight",
      title: "Point Your Phone at the Sky",
      description: "Aim your camera upward and instantly see overlaid flight labels on every aircraft above you — no manual searching required.",
    },
    {
      icon: "my_location",
      title: "Real-Time Position Accuracy",
      description: "Aircraft positions update live using ADS-B transponder data, so the overlay stays locked to where planes actually are.",
    },
    {
      icon: "explore",
      title: "Altitude & Flight Path Data",
      description: "See each aircraft's current altitude, heading, and flight level displayed directly in your camera view.",
    },
    {
      icon: "offline_bolt",
      title: "Smooth, Jitter-Free Tracking",
      description: "Custom stabilization keeps AR labels steady even as you move, preventing the flickering common in other AR apps.",
    },
  ],
};

// --- Problem/Solution Section ------------------------------------------------
export const problemSolution = {
  title: "The Technical Challenge.",
  description:
    "Traditional flight tracking relies on 2D maps. For aviation enthusiasts and students, visualizing the actual geometric relationship between a ground observer and a cruising aircraft remains a mental abstraction.",
  quote: {
    text: "PlaneAR solves the parallax gap by mapping ADS-B telemetry directly into a world-space AR coordinate system.",
  },
  cards: [
    {
      icon: "error_outline",
      title: "The Challenge",
      description:
        "Mapping real-world aircraft into accurate 3D space is inherently unstable—sensor drift, atmospheric distortion, and noisy real-time data continuously disrupt spatial alignment.",
      variant: "problem" as const,
    },
    {
      icon: "verified_user",
      title: "Our Approach",
      description:
        "We combine raw device sensor data with ARCore’s camera feed and spatial transforms to directly compute world-space positions for ADS-B telemetry. By avoiding filtering and high-level abstractions, our native C++ engine achieves precise, real-time AR alignment.",
      variant: "solution" as const,
    },
    {
      icon: "",
      title: "Engineering Methodology",
      description:
      "We use a two-layer architecture: Kotlin handles application logic and UI, while a custom in-house Vulkan engine performs real-time geometric computation and rendering, driven by ARCore sensor and camera input. This design enables high-performance AR processing with a fully decoupled interface layer.",
      variant: "methodology" as const,
      image: {
        src: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&auto=format&fit=crop&q=80",
        alt: "Circuit board with glowing LEDs",
      },
    },
  ],
};

// --- Resources/Documentation Section -----------------------------------------
export const resources = {
  title: "Technical Resources.",
  description:
    "Access the project repository and distribution builds. Dive into our direct sensor-driven approach to real-world coordinate mapping and AR rendering.",
  resources: [
    {
      icon: "download",
      label: "Download APK",
      href: links.download,
      external: false,
    },
    {
      icon: "terminal",
      label: "View Source on GitHub",
      href: links.github,
      external: true,
    },
  ],
  image: {
    src: "https://lh3.googleusercontent.com/aida-public/AB6AXuBy6FKSZjnymF2DGWt4ntgCg1KKH2gjpB5U9FZRBPTUuJS4s86ImqYQTSd6GEhFD2ZYRf5ENn5WSuq0Lji3l6OhJEmcA4a2IFYPWVy0OwqOgEyUwi7Aww3BDGRLo9Mmx9r-cXv37te0bSj1aYfNXcSuQmnRLEX4AU4jHYSmqJutHR76CqflUaV06NeHemhvEhhKD6cfUF66cugWtFDB1h7-1jGK5vjZeOxu6nRfShEy0GDP3YtR7fAN4KzzOG0Fc9IVmua2hUDMA8cb",
    alt: "Technical hardware",
  },
};

// ============================================================================
// Type Exports (for components that need them)
// ============================================================================
export type SiteConfig = typeof siteConfig;
export type Links = typeof links;
export type Navigation = typeof navigation;
export type Hero = typeof hero;
export type Team = typeof team;
export type Capabilities = typeof capabilities;
export type CapabilityCard = Capabilities["cards"][number];
export type ProblemSolution = typeof problemSolution;
export type ProblemSolutionCard = ProblemSolution["cards"][number];
export type Resources = typeof resources;
export type ResourceLink = Resources["resources"][number];
