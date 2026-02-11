#ifndef PLANEAR_SCENESWITCHER_H
#define PLANEAR_SCENESWITCHER_H

#include <source/GraphicsEngine.h>
#include <functional>
#include <memory>
#include <unordered_map>

class SceneSwitcher
{
public:
  using SceneCallback = std::function<void(const std::unique_ptr<ge::GraphicsEngine>& engine)>;

  void renderCurrentScene(const std::unique_ptr<ge::GraphicsEngine>& engine);

  void loadScene(uint32_t id,
                 SceneCallback scene);

  void setCurrentScene(uint32_t id);

private:
  std::unordered_map<uint32_t, SceneCallback> m_scenes;

  uint32_t m_currentScene = 0;
};


#endif //PLANEAR_SCENESWITCHER_H
