project(GraphicsEngine)

add_library(${PROJECT_NAME} SHARED
  main.cpp
  GraphicsEngine.cpp
  components/instance/DebugMessenger.cpp
  components/instance/Instance.cpp
)

find_package(game-activity REQUIRED CONFIG)

find_library(VULKAN_LIBRARY vulkan)

set(CMAKE_SHARED_LINKER_FLAGS
  "${CMAKE_SHARED_LINKER_FLAGS} -u Java_com_google_androidgamesdk_GameActivity_initializeNativeCode")

target_link_libraries(${PROJECT_NAME}
  ${VULKAN_LIBRARY}
  game-activity::game-activity_static
  android
  log)