#include "Button.h"
#include "../../components/renderingManager/renderer2D/Renderer2D.h"

namespace ge::ui {

  Button::Button(std::string label, float x, float y, float w, float h)
    : Widget(x, y, w, h), m_label(std::move(label)) {}

  bool Button::update(float mouseX, float mouseY, bool tapOccurred) {
    m_isHovered = contains(mouseX, mouseY);
    
    if (m_isHovered && tapOccurred) {
      return true;
    }
    
    return false;
  }

  void Button::draw(const std::shared_ptr<Renderer2D>& renderer) const {
    // Background shadow/border
    renderer->fill(0, 0, 0, 50);
    renderer->rect(m_x + 4, m_y + 4, m_w, m_h);

    // Main background
    if (m_isActive) {
      renderer->fill(102, 178, 102); // Active (Greenish)
    } else if (m_isHovered) {
      renderer->fill(220, 220, 220); // Hover (Lighter gray)
    } else {
      renderer->fill(204, 204, 204); // Default (Gray)
    }
    
    renderer->rect(m_x, m_y, m_w, m_h);

    // Label text
    renderer->fill(40, 40, 40);
    renderer->textSize(32);
    renderer->text(m_label, m_x + 20, m_y + (m_h / 2.0f) + 10);
  }

} // ge::ui
