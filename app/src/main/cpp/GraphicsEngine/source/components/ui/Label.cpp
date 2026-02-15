#include "Label.h"
#include "../../components/renderingManager/renderer2D/Renderer2D.h"

namespace ge::ui {

  Label::Label(std::string text, float x, float y, std::string font, uint32_t size)
    : Widget(x, y, 0, 0), m_text(std::move(text)), m_font(std::move(font)), m_size(size) {}

  void Label::draw(ge::Renderer2D& renderer) const {
    renderer.fill(m_color.r, m_color.g, m_color.b, m_color.a);
    renderer.textFont(m_font, m_size);
    renderer.text(m_text, m_x, m_y);
  }

} // ge::ui
