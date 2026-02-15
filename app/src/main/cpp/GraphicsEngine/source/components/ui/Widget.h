#ifndef PLANEAR_WIDGET_H
#define PLANEAR_WIDGET_H

namespace ge {
  class Renderer2D;
}

namespace ge::ui {

  class Widget {
  public:
    Widget(float x, float y, float w, float h)
      : m_x(x), m_y(y), m_w(w), m_h(h) {}

    virtual ~Widget() = default;

    [[nodiscard]] bool contains(float mx, float my) const {
      return mx >= m_x && mx <= (m_x + m_w) && my >= m_y && my <= (m_y + m_h);
    }

    virtual bool update(float mouseX, float mouseY, bool tapOccurred) = 0;

    virtual void draw(ge::Renderer2D& renderer) const = 0;

  protected:
    float m_x, m_y, m_w, m_h;
  };

} // ge::ui

#endif //PLANEAR_WIDGET_H
