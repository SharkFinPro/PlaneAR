project(GraphicsEngine)

include(FetchContent)

add_library(${PROJECT_NAME} SHARED
  GraphicsEngine.cpp
  JNIGraphicsBridge.cpp
  components/assets/AssetManager.cpp
  components/assets/fonts/Font.cpp
  components/assets/textures/CameraTexture.cpp
  components/assets/textures/GlyphTexture.cpp
  components/assets/textures/ImageTexture.cpp
  components/assets/textures/Texture.cpp
  components/commandBuffer/CommandBuffer.cpp
  components/commandBuffer/SingleUseCommandBuffer.cpp
  components/descriptorSet/DescriptorSet.cpp
  components/framebuffers/Framebuffer.cpp
  components/framebuffers/SwapchainFramebuffer.cpp
  components/instance/DebugMessenger.cpp
  components/instance/Instance.cpp
  components/logicalDevice/LogicalDevice.cpp
  components/surface/Surface.cpp
  components/physicalDevice/PhysicalDevice.cpp
  components/pipelines/GraphicsPipeline.cpp
  components/pipelines/Pipeline.cpp
  components/pipelines/PipelineManager.cpp
  components/renderingManager/renderer2D/MousePicker.cpp
  components/renderingManager/renderer2D/Renderer2D.cpp
  components/renderingManager/LegacyRenderer.cpp
  components/renderingManager/RenderingManager.cpp
  components/renderPass/RenderPass.cpp
  components/shaderModule/ShaderModule.cpp
  components/surface/Swapchain.cpp
  components/ui/Button.cpp
  components/ui/Label.cpp
)

target_link_options(${PROJECT_NAME} PRIVATE -Wl,-z,max-page-size=16384)

find_package(game-activity REQUIRED CONFIG)

find_library(VULKAN_LIBRARY vulkan)

set(FETCHCONTENT_BASE_DIR ${CMAKE_SOURCE_DIR}/_deps)

# GLM
FetchContent_Declare(
  glm
  GIT_REPOSITORY https://github.com/g-truc/glm.git
  GIT_TAG 1.0.2
)

# Freetype
FetchContent_Declare(
  freetype
  GIT_REPOSITORY https://github.com/freetype/freetype.git
  GIT_TAG VER-2-14-1
)

# STB
FetchContent_Declare(
  stb
  GIT_REPOSITORY https://github.com/nothings/stb
  GIT_TAG master
)

FetchContent_MakeAvailable(
  glm
  freetype
  stb
)

target_link_libraries(${PROJECT_NAME}
  ${VULKAN_LIBRARY}
  game-activity::game-activity_static
  android
  log
  freetype
)

target_include_directories(${PROJECT_NAME} PUBLIC
  ${glm_SOURCE_DIR}
  ${stb_SOURCE_DIR}
)

target_compile_definitions(${PROJECT_NAME} PRIVATE
  JNI_PACKAGE_PATH="edu/osu/t22/planear/graphicsEngine"
)
