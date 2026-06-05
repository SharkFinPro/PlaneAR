# PlaneAR Graphics Engine

A high-performance, Vulkan-based graphics library specifically engineered for the PlaneAR augmented reality system. The engine provides a complete, low-overhead rendering pipeline optimized for mobile hardware, supporting spatial 3D overlays, 2D UI elements, and high-fidelity text rendering.

## Key Features

- **Vulkan-Native Rendering**: Leverages a modern, low-overhead graphics API to maximize frame rates and minimize CPU overhead on Android devices.
- **Modular Architecture**: A strictly decoupled design with clear boundaries between instance management, pipeline configuration, and resource handling.
- **Spatial 2D Graphics**: Optimized primitives for rendering aircraft markers, HUD elements, and interactive billboard cards.
- **Advanced Text Pipeline**: High-quality font rendering integrated via FreeType for clear, legible aircraft callsigns and data.
- **Android Game Activity Integration**: Seamlessly integrated with the Android Game Activity lifecycle for low-latency input and display.

## System Architecture

The `GraphicsEngine` utilizes a layered architecture to ensure maintainability and hardware abstraction:

### Foundation Layer
- **Vulkan Instance & Device**: Manages the physical GPU selection, logical device creation, and queue family management.
- **Surface & Swapchain**: Handles window surface creation and the double/triple buffering mechanism for smooth frame presentation.
- **Debug Tooling**: Integrated Vulkan validation layers and custom logging for rapid development and debugging.

### Pipeline Layer
- **Pipeline State**: Manages graphics pipeline states, including vertex input, rasterization, and color blending.
- **Shader Management**: Handles the compilation and loading of GLSL shaders (compiled to SPIR-V).
- **Render Pass Logic**: Defines the render pass structure, attachments, and image layout transitions.

### Resource Layer
- **Command Orchestration**: Efficient recording and submission of command buffers to GPU queues.
- **Descriptor Management**: Handles the binding of shader resources, such as uniforms and textures.
- **Asset Pipeline**: A centralized system for loading and caching fonts and image assets.

### Application Layer
- **High-Level API**: Provides a simplified interface for the Kotlin layer to issue draw calls.
- **2D Utility Suite**: Optimized functions for rendering the AR markers and the aviation HUD.

## Library Structure

### Core Entry Points
| Component | Path | Responsibility |
|-----------|------|-----------------|
| **Engine Interface** | `GraphicsEngine.cpp/h` | Primary library API and system initialization |
| **Test Entry** | `main.cpp` | Standalone entry point for engine verification |
| **Logging** | `Logger.h` | System-wide debug and error reporting |

### Rendering Pipeline
| Component | Directory | Responsibility |
|-----------|-----------|-----------------|
| **Instance** | `instance/` | Vulkan initialization and debug messenger setup |
| **Surface** | `surface/` | Swapchain management and window surface handling |
| **Pipelines** | `pipelines/` | Pipeline state objects and configuration |
| **Shader Module** | `shaderModule/` | SPIR-V module creation and loading |
| **Render Pass** | `renderPass/` | Definition of render targets and layout transitions |

### Asset & Resource Management
| Component | Directory | Responsibility |
|-----------|-----------|-----------------|
| **Asset Manager** | `assets/` | Lifecycle management for textures and fonts |
| **Fonts** | `assets/fonts/` | FreeType glyph generation and caching |
| **Textures** | `assets/textures/` | Image loading and Vulkan image view creation |
| **Buffers** | `utilities/` | Helper classes for vertex and index buffer management |

### High-Level API
| Component | Directory | Responsibility |
|-----------|-----------|-----------------|
| **Rendering Manager** | `renderingManager/` | Orchestration of the frame rendering loop |
| **Renderer 2D** | `renderingManager/renderer2D/` | 2D primitive and text rendering utilities |
