#ifndef PLANEAR_LABEL_H
#define PLANEAR_LABEL_H

#include "Widget.h"
#include <string>
#include <glm/vec4.hpp>

namespace ge {
  class Renderer2D;
}

namespace ge::ui {

  class Label : public Widget {
  public:
    Label(std::string text, float x, float y, std::string font = "roboto", uint32_t size = 32);

    // Labels don't usually respond to touch, but we implement update to satisfy the base class
    bool update(float mouseX, float mouseY, bool tapOccurred) override { return false; }

    void draw(ge::Renderer2D& renderer) const override;

    void setText(std::string text) { m_text = std::move(text); }
    void setSize(uint32_t size) { m_size = size; }
    void setFont(std::string font) { m_font = std::move(font); }
    void setColor(float r, float g, float b, float a = 255.0f) { m_color = {r, g, b, a}; }

  private:
    std::string m_text;
    std::string m_font;
    uint32_t m_size;
    glm::vec4 m_color = {255, 255, 255, 255};
  };

} // ge::ui

#endif //PLANEAR_LABEL_H
