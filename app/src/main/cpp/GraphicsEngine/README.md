# Graphics Engine

A high-performance, Vulkan-based graphics library for the PlaneAR Android App. Provides a complete rendering pipeline with support for 2D graphics, text rendering, and modern graphics APIs.

## Key Features

- **Vulkan-based Rendering** - Modern, low-overhead graphics API for optimal performance
- **Modular Architecture** - Clean separation of concerns with well-defined component boundaries
- **2D Graphics Support** - Efficient rendering of rectangles, ellipses, and triangles
- **Text Rendering** - High-quality font rendering via FreeType integration
- **Native Android Integration** - Seamless integration with Android Game Activity lifecycle

## Architecture

GraphicsEngine follows a layered architecture that separates concerns across distinct functional areas:

**Foundation Layer**
- Vulkan instance and device management
- Surface creation and presentation
- Debug and validation tooling

**Pipeline Layer**
- Graphics pipeline configuration and state management
- Shader module compilation and loading
- Render pass and framebuffer management

**Resource Layer**
- Command buffer recording and submission
- Descriptor set management for shader resources
- Asset loading and caching (fonts, textures)

**Application Layer**
- High-level rendering API
- 2D rendering utilities
- Draw call orchestration

## Library Structure

### Entry Point & Core

| Directory/File | Purpose |
|----------------|---------|
| `GraphicsEngine.cpp/h` | Main library interface and initialization |
| `main.cpp` | Entry point for testing/standalone execution |
| `Logger.h` | Logging utilities and debug output |

### Shaders

| Directory | Purpose |
|-----------|---------|
| `shaders/` | GLSL shader source files (`.vert`, `.frag`) compiled to SPIR-V for Vulkan |

**Included Shaders:**
- `ellipse` - Ellipse/circle rendering
- `font` - Text glyph rendering
- `rect` - Rectangle rendering
- `triangle` - Triangle primitive rendering

### Utilities

| Directory | Purpose |
|-----------|---------|
| `utilities/` | Helper functions for buffers, images, and common operations |

## Component Overview

### Core Vulkan Setup

| Component | Directory | Responsibilities |
|-----------|-----------|------------------|
| **Instance** | `instance/` | Vulkan instance initialization, validation layers, and debug messaging |
| **Physical Device** | `physicalDevice/` | GPU detection, capability queries, and device selection |
| **Logical Device** | `logicalDevice/` | Device interface creation and queue family management |
| **Surface** | `surface/` | Window surface creation, swapchain management, and presentation |

### Rendering Pipeline

| Component | Directory | Responsibilities |
|-----------|-----------|------------------|
| **Pipelines** | `pipelines/` | Graphics pipeline configuration, state management, and pipeline caching |
| **Shader Module** | `shaderModule/` | Shader compilation and Vulkan module creation |
| **Render Pass** | `renderPass/` | Render pass definitions, attachments, and layout transitions |
| **Framebuffers** | `framebuffers/` | Render target creation and swapchain framebuffer management |

### Command & Resource Binding

| Component | Directory | Responsibilities |
|-----------|-----------|------------------|
| **Command Buffer** | `commandBuffer/` | Command recording, allocation, and submission to GPU queues |
| **Descriptor Set** | `descriptorSet/` | Shader resource binding and descriptor pool management |

### Asset Management

| Component | Directory | Responsibilities |
|-----------|-----------|------------------|
| **Asset Manager** | `assets/` | Centralized loading, caching, and lifecycle management for all resources |
| **Fonts** | `assets/fonts/` | Font loading and glyph management via FreeType |
| **Textures** | `assets/textures/` | Image loading, texture creation, and glyph texture handling |

### High-Level Rendering

| Component | Directory | Responsibilities |
|-----------|-----------|------------------|
| **Rendering Manager** | `renderingManager/` | Primary rendering API and draw call orchestration |
| **Renderer 2D** | `renderingManager/renderer2D/` | 2D graphics rendering and utilities |
