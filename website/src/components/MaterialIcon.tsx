import React from "react";
import type { MaterialIconProps } from "../types";

const MaterialIcon: React.FC<MaterialIconProps> = ({
                                                     icon,
                                                     className = "",
                                                     filled = false,
                                                     ...props
                                                   }) => {
  return (
    <span
      className={`material-symbols-outlined ${className}`}
      style={{
        fontVariationSettings: filled
          ? "'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24"
          : undefined,
      }}
      {...props}
    >
      {icon}
    </span>
  );
};

export default MaterialIcon;
