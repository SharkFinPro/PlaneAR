#include "SceneSwitcher.h"
#include <stdexcept>

void SceneSwitcher::renderCurrentScene(const SceneInfo& sceneInfo)
{
  if (!m_scenes.contains(m_currentScene))
  {
    throw std::runtime_error("Scene with ID " + std::to_string(m_currentScene) + " does not exist!");
  }

  m_scenes.at(m_currentScene)(sceneInfo, this);

  sceneInfo.engine->render();
}

void SceneSwitcher::loadScene(uint32_t id,
                              SceneCallback scene)
{
  if (id == 0)
  {
    throw std::runtime_error("A scene's ID must be > 0!");
  }

  if (m_scenes.contains(id))
  {
    throw std::runtime_error("Scene with ID " + std::to_string(id) + " already exists!");
  }

  m_scenes.emplace(id, std::move(scene));

  if (m_currentScene == 0)
  {
    m_currentScene = id;
  }
}

void SceneSwitcher::setCurrentScene(uint32_t id)
{
  if (id == 0)
  {
    throw std::runtime_error("A scene's ID must be > 0!");
  }

  if (!m_scenes.contains(id))
  {
    throw std::runtime_error("Scene with ID " + std::to_string(id) + " does not exist!");
  }

  m_currentScene = id;
}

uint32_t SceneSwitcher::getCurrentScene() const
{
  return m_currentScene;
}
