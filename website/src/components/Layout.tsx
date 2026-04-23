import React from "react";
import { ThemeProvider } from "../context/ThemeContext";
import Header from "./Header";
import Footer from "./Footer";
import type { LayoutProps } from "../types";

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <ThemeProvider>
      <div className="min-h-screen bg-background text-on-surface">
        <Header />
        <main className="pt-16 md:pt-20">{children}</main>
        <Footer />
      </div>
    </ThemeProvider>
  );
};

export default Layout;
