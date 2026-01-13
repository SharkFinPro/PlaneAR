# Graphics Engine

## Overview

GraphicsEngine is a Vulkan-based graphics library for the AR Plane Tracking App, built as a shared C++ library. It provides a complete rendering pipeline with support for 2D graphics, text rendering, and modern graphics APIs.

## Architecture

The GraphicsEngine is organized into modular components, each handling specific aspects of the graphics pipeline. The library integrates with Vulkan for low-level graphics operations and uses FreeType for font rendering.

## File Structure

### Vulkan Core Components (`/components`)

#### Entry Point (`/`)
- **GraphicsEngine.cpp** - Library entry point

#### Instance Management (`/instance`)
- **Instance.h / .cpp** - Vulkan instance creation and management
- **DebugMessenger.h / .cpp** - Debug validation layers and message callbacks

#### Physical & Logical Devices
- **physicalDevice/** - Physical device selection and properties
- **logicalDevice/** - Logical device creation and queue management

#### Surface & Swapchain (`/surface`)
- **Surface.cpp** - Window surface creation and management
- **Swapchain.cpp** - Swapchain image and presentation management

#### Rendering Pipeline (`/pipelines`)
- **Pipeline.h / .cpp** - Base pipeline abstraction
- **GraphicsPipeline.h / .cpp** - Graphics pipeline implementation
- **GraphicsPipelineStates.h** - Pipeline state configurations
- **PipelineConfig.h** - Pipeline configuration structures
- **PipelineManager.h / .cpp** - Pipeline lifecycle management

#### Render Targets
- **renderPass/** - Render pass definitions and attachments
- **framebuffers/** - Framebuffer creation and management
    - Includes specialized swapchain framebuffer handling

#### Shader System (`/shaderModule`)
- **ShaderModule.cpp** - Shader compilation and Vulkan module management

#### Command Recording (`/commandBuffer`)
- **CommandBuffer.cpp** - Command buffer allocation and recording

#### Descriptors (`/descriptorSet`)
- **DescriptorSet.cpp** - Descriptor set and pool management

### Asset Management (`/assets`)

#### Fonts (`/fonts`)
- Font loading and rendering support

#### Textures (`/textures`)
- **Texture.cpp** - Texture image management
- **GlyphTexture.cpp** - Specialized glyph texture handling

#### Core
- **AssetManager.h / .cpp** - Unified asset loading and caching system

## Rendering API (`/renderingManager`)

### High-Level Renderers
- **RenderingManager.h / .cpp** - Main rendering API and orchestration
- **Renderer.h** - Renderer interface definition
- **LegacyRenderer.h / .cpp** - Legacy rendering implementation

### 2D Rendering (`/renderer2D`)
- **Renderer2D.cpp** - 2D-specific rendering functionality

## Additional Directories

- **`/shaders`** - GLSL shader source files
- **`/utilities`** - Helper functions and utility classes


## Dependencies

### External Libraries

- **Vulkan** - Low-level graphics API
- **GLM** (v1.0.2) - Mathematics library for graphics
- **FreeType** (v2-14-1) - Font rasterization engine
- **Android Game Activity** - Android-specific game integration
- **Android NDK Libraries** - `android`, `log`

### Build System

- CMake (for native build configuration)
- Android NDK (for compilation)

## Component Description

| Component | Purpose |
|-----------|---------|
| **Assets** | Manages textures, fonts, and other game resources |
| **Instance** | Initializes Vulkan instance and debug infrastructure |
| **Physical Device** | Selects appropriate GPU for rendering |
| **Logical Device** | Creates logical device for GPU communication |
| **Surface** | Manages window surface and presentation |
| **Command Buffer** | Records rendering commands |
| **Descriptor Set** | Manages shader resource bindings |
| **Render Pass** | Defines rendering operations and attachments |
| **Framebuffers** | Manages render targets and swapchain buffers |
| **Shader Module** | Loads and compiles shaders |
| **Pipelines** | Manages graphics pipeline states |
| **Rendering Manager** | High-level API for submitting draw calls |

## Build Information

The library is compiled as a **shared library** (`.so` on Android) including all components and dependencies. It requires:

- C++17 or later
- Vulkan 1.0+ support on the target device
- Android API level compatible with Game Activity

---

**Note**: This is a native C++ graphics library designed for high-performance Android game development with modern Vulkan rendering capabilities.