import React from "react";
import { useTheme } from "../context/ThemeContext";
import MaterialIcon from "./MaterialIcon";

const ThemeToggle: React.FC = () => {
  const { toggleTheme, isDark } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className="w-10 h-10 p-2 rounded-lg bg-surface-container-high hover:bg-surface-container-highest transition-colors duration-200"
      aria-label={`Switch to ${isDark ? "light" : "dark"} mode`}
      title={`Switch to ${isDark ? "light" : "dark"} mode`}
    >
      {isDark ? (
        <MaterialIcon icon="light_mode" className="text-on-surface" />
      ) : (
        <MaterialIcon icon="dark_mode" className="text-on-surface" />
      )}
    </button>
  );
};

export default ThemeToggle;
