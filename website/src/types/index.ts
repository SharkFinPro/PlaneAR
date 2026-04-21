// Theme types
import React from "react";

export type Theme = 'light' | 'dark';

export interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
  isDark: boolean;
}

// Component prop types
export interface MaterialIconProps {
  icon: string;
  className?: string;
  filled?: boolean;
  [key: string]: unknown;
}

// Layout types
export interface LayoutProps {
  children: React.ReactNode;
}
