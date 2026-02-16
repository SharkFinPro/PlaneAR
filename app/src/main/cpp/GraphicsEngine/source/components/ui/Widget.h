#ifndef PLANEAR_WIDGET_H
#define PLANEAR_WIDGET_H

#include <memory>

namespace ge {
  class Renderer2D;
}

namespace ge::ui {

  class Widget {
  public:
    Widget(float x, float y, float w, float h)
      : m_x(x), m_y(y), m_w(w), m_h(h) {}

    virtual ~Widget() = default;

    [[nodiscard]] bool contains(float mouseX, float mouseY) const {
      return mouseX >= m_x && mouseX <= (m_x + m_w) && mouseY >= m_y && mouseY <= (m_y + m_h);
    }

    virtual bool update(float mouseX, float mouseY, bool tapOccurred) = 0;

    virtual void draw(const std::shared_ptr<Renderer2D>& renderer) const = 0;

  protected:
    float m_x, m_y, m_w, m_h;
  };

} // ge::ui

#endif //PLANEAR_WIDGET_H
