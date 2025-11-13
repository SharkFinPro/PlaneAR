project(GraphicsEngine)

add_library(${PROJECT_NAME} SHARED
  main.cpp
  GraphicsEngine.cpp
  components/commandBuffer/CommandBuffer.cpp
  components/framebuffers/Framebuffer.cpp
  components/framebuffers/SwapchainFramebuffer.cpp
  components/instance/DebugMessenger.cpp
  components/instance/Instance.cpp
  components/logicalDevice/LogicalDevice.cpp
  components/surface/Surface.cpp
  components/physicalDevice/PhysicalDevice.cpp
  components/renderingManager/LegacyRenderer.cpp
  components/renderingManager/RenderingManager.cpp
  components/renderPass/RenderPass.cpp
  components/surface/Swapchain.cpp
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