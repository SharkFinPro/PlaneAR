project(GraphicsEngine)

include(FetchContent)

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
  components/pipelines/implementations/QuadPipeline.cpp
  components/pipelines/GraphicsPipeline.cpp
  components/pipelines/Pipeline.cpp
  components/renderingManager/LegacyRenderer.cpp
  components/renderingManager/RenderingManager.cpp
  components/renderPass/RenderPass.cpp
  components/shaderModule/ShaderModule.cpp
  components/surface/Swapchain.cpp
)

find_package(game-activity REQUIRED CONFIG)

find_library(VULKAN_LIBRARY vulkan)

# Freetype
set(FETCHCONTENT_BASE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../_deps)

FetchContent_Declare(
  freetype
  GIT_REPOSITORY https://github.com/freetype/freetype.git
  GIT_TAG VER-2-14-1
)

FetchContent_MakeAvailable(freetype)

set(CMAKE_SHARED_LINKER_FLAGS
  "${CMAKE_SHARED_LINKER_FLAGS} -u Java_com_google_androidgamesdk_GameActivity_initializeNativeCode")

target_link_libraries(${PROJECT_NAME}
  ${VULKAN_LIBRARY}
  game-activity::game-activity_static
  android
  log
  freetype
)
