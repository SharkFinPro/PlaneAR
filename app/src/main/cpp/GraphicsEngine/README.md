# Graphics Engine

A high-performance, Vulkan-based graphics library for the AR Plane Tracking Android App. Provides a complete rendering pipeline with support for 2D graphics, text rendering, and modern graphics APIs.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [File Structure](#file-structure)
- [Dependencies](#dependencies)
- [Build Information](#build-information)

## Overview

GraphicsEngine is a shared C++ library designed for the AR Plane Tracking App, built on the Vulkan graphics API. It abstracts complex graphics operations into modular components, enabling efficient rendering of 2D graphics and text while maintaining low-level control for performance-critical applications.

### Key Features

- **Vulkan-based Rendering** - Modern graphics API for optimal performance
- **Modular Architecture** - Clean separation of concerns across rendering pipeline
- **2D Graphics & Text Rendering** - Complete support via FreeType font engine
- **Native Android Integration** - Seamless integration with Android Game Activity

## Architecture

GraphicsEngine follows a layered architecture with clear separation between:

- **Low-level Vulkan abstractions** - Device, instance, and command management
- **Rendering pipeline** - Shaders, render passes, and graphics state management
- **Asset management** - Centralized resource loading and caching
- **High-level rendering API** - Simplified interface for draw calls

## File Structure

### Entry Point
- **GraphicsEngine.cpp** - Library initialization and main entry point

### Components (`/components`)

#### Instance Management (`/instance`)
- **Instance.h / .cpp** - Vulkan instance creation and lifecycle management
- **DebugMessenger.h / .cpp** - Validation layers and debug callbacks

#### Device Management
- **physicalDevice/** - GPU selection, capability queries, and properties
- **logicalDevice/** - Logical device creation and queue family management

#### Surface & Presentation (`/surface`)
- **Surface.cpp** - Window surface creation and management
- **Swapchain.cpp** - Image acquisition, presentation, and frame synchronization

#### Rendering Pipeline (`/pipelines`)
- **Pipeline.h / .cpp** - Base pipeline abstraction and lifecycle
- **GraphicsPipeline.h / .cpp** - Complete graphics pipeline implementation
- **GraphicsPipelineStates.h** - Pipeline state management and configurations
- **PipelineConfig.h** - Pipeline configuration data structures
- **PipelineManager.h / .cpp** - Pipeline creation and state management

#### Render Targets
- **renderPass/** - Render pass definitions, attachment descriptions, and layout transitions
- **framebuffers/** - Framebuffer creation and management
    - Includes swapchain framebuffer specializations

#### Shader System (`/shaderModule`)
- **ShaderModule.cpp** - Shader module creation and Vulkan module management

#### Command Recording (`/commandBuffer`)
- **CommandBuffer.cpp** - Command buffer allocation, recording, and submission

#### Descriptors (`/descriptorSet`)
- **DescriptorSet.cpp** - Descriptor set pool allocation and binding management

### Asset Management (`/assets`)

#### Fonts (`/fonts`)
- Font loading and glyph management using FreeType

#### Textures (`/textures`)
- **Texture.cpp** - Image loading and texture resource management
- **GlyphTexture.cpp** - Specialized texture handling for rendered glyphs

#### Core
- **AssetManager.h / .cpp** - Centralized asset loading, caching, and lifecycle management

### Rendering API (`/renderingManager`)

#### High-Level Renderers
- **RenderingManager.h / .cpp** - Primary rendering API and draw call orchestration
- **Renderer.h** - Abstract renderer interface
- **LegacyRenderer.h / .cpp** - Legacy rendering path support

#### 2D Rendering (`/renderer2D`)
- **Renderer2D.cpp** - 2D-specific rendering implementation and utilities

### Supporting Directories

- **`/shaders`** - GLSL shader source files (compiled to SPIR-V)
- **`/utilities`** - Helper functions, logging, and utility classes
- **`/includes`** - Public header files and API definitions

## Dependencies

### External Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **Vulkan** | 1.0+ | Graphics rendering API |
| **GLM** | v1.0.2 | Linear algebra and mathematics |
| **FreeType** | v2-14-1 | Font rasterization and rendering |
| **Android Game Activity** | Latest | Android game lifecycle integration |
| **Android NDK** | r25+ | Native development toolkit (`android`, `log` libraries) |

### Build Requirements

- **CMake** - Build configuration and compilation
- **Android NDK** - C++ cross-compilation toolchain
- **Vulkan SDK** - Headers and validation layers (development)

## Component Overview

| Component | Purpose |
|-----------|---------|
| **Assets** | Resource loading, caching, and lifecycle management |
| **Instance** | Vulkan instance initialization and validation setup |
| **Physical Device** | GPU capability detection and device selection |
| **Logical Device** | GPU interface creation and queue management |
| **Surface** | Display integration and window management |
| **Command Buffer** | Recording and submission of GPU commands |
| **Descriptor Set** | Shader resource binding and layout management |
| **Render Pass** | Rendering operations, attachments, and layout transitions |
| **Framebuffers** | Render target allocation and management |
| **Shader Module** | Shader compilation and GPU module creation |
| **Pipelines** | Graphics state management and pipeline configuration |
| **Rendering Manager** | High-level draw call API and rendering orchestration |

## Build Information

### Requirements

- **C++ Standard**: C++17 or later
- **Vulkan Support**: 1.1 or later on target device
- **Android API Level**: Compatible with Android Game Activity (API 24+)
- **Target Android SDK**: Version 36

### Output

GraphicsEngine is compiled as a **shared library** (`.so` on Android) with all components and dependencies linked.

### Compilation

```bash
# Configure with CMake (from Android NDK)
cmake -B build -S . \
  -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24

# Build
cmake --build build
```