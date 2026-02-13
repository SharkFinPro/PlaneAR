#include "SceneSwitcher.h"
#include <source/components/renderingManager/RenderingManager.h>
#include <source/components/renderingManager/renderer2D/Renderer2D.h>
#include <stdexcept>

void SceneSwitcher::renderCurrentScene(const SceneInfo& sceneInfo)
{
  validateSceneExists(m_currentScene);

  sceneInfo.engine->getRenderingManager()->getRenderer2D()->createNewFrame();

  m_scenes.at(m_currentScene)(sceneInfo, this);

  sceneInfo.engine->render();
}

void SceneSwitcher::loadScene(uint32_t id,
                              SceneCallback scene)
{
  validateSceneId(id);
  validateSceneDoesNotExist(id);

  m_scenes.emplace(id, std::move(scene));

  if (m_currentScene == 0)
  {
    m_currentScene = id;
  }
}

void SceneSwitcher::setCurrentScene(uint32_t id)
{
  validateSceneId(id);
  validateSceneExists(id);
  m_currentScene = id;
}

uint32_t SceneSwitcher::getCurrentScene() const
{
  return m_currentScene;
}

void SceneSwitcher::validateSceneId(uint32_t id) const
{
  if (id == 0)
  {
    throw std::runtime_error("A scene's ID must be > 0!");
  }
}

void SceneSwitcher::validateSceneExists(uint32_t id) const
{
  if (!m_scenes.contains(id))
  {
    throw std::runtime_error("Scene with ID " + std::to_string(id) + " does not exist!");
  }
}

void SceneSwitcher::validateSceneDoesNotExist(uint32_t id) const
{
  if (m_scenes.contains(id))
  {
    throw std::runtime_error("Scene with ID " + std::to_string(id) + " already exists!");
  }
}
