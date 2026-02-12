#ifndef PLANEAR_SCENESWITCHER_H
#define PLANEAR_SCENESWITCHER_H

#include <source/GraphicsEngine.h>
#include <functional>
#include <memory>
#include <unordered_map>

struct SceneInfo {
  const std::unique_ptr<ge::GraphicsEngine>& engine;
  struct android_app* pApp;
  float mouseX;
  float mouseY;
  bool tapOccurred;
};

class SceneSwitcher
{
public:
  using SceneCallback = std::function<void(const SceneInfo&, SceneSwitcher*)>;

  void renderCurrentScene(const SceneInfo& sceneInfo);

  void loadScene(uint32_t id,
                 SceneCallback scene);

  void setCurrentScene(uint32_t id);

  [[nodiscard]] uint32_t getCurrentScene() const;

private:
  std::unordered_map<uint32_t, SceneCallback> m_scenes;

  uint32_t m_currentScene = 0;

  void validateSceneId(uint32_t id) const;

  void validateSceneExists(uint32_t id) const;

  void validateSceneDoesNotExist(uint32_t id) const;
};


#endif //PLANEAR_SCENESWITCHER_H
