#ifndef PLANEAR_BUTTON_H
#define PLANEAR_BUTTON_H

#include "Widget.h"
#include <string>

namespace ge {
  class Renderer2D;
}

namespace ge::ui {

  class Button : public Widget {
  public:
    Button(std::string label, float x, float y, float w, float h);

    bool update(float mouseX, float mouseY, bool tapOccurred) override;

    void draw(ge::Renderer2D& renderer) const override;

    void setActive(bool active) { m_isActive = active; }

  private:
    std::string m_label;
    bool m_isHovered = false;
    bool m_isActive = false;
  };

} // ge::ui

#endif //PLANEAR_BUTTON_H
