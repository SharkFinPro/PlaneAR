<div align="center">
  <a href="https://github.com/SharkFinPro/PlaneAR">
    <picture>
      <img alt="PlaneAR Logo" src="assets/planeAR-logo.png" height="128">
    </picture>
  </a>

  <h1>PlaneAR</h1>

  <p>Real-time aircraft tracking and identification through Augmented Reality on Android devices.</p>

  [![Android CI](https://github.com/SharkFinPro/PlaneAR/actions/workflows/android-ci.yml/badge.svg)](https://github.com/SharkFinPro/PlaneAR/actions/workflows/android-ci.yml)
</div>

---

## Project Overview

PlaneAR is a high-performance Android application that leverages Augmented Reality (AR) to visualize real-time aircraft positions in the sky. By integrating live ADS-B (Automatic Dependent Surveillance-Broadcast) data with device orientation sensors, the app provides a seamless overlay that allows users to identify aircraft and view flight details simply by pointing their device toward the horizon.

The core of the application is a custom-built, native Vulkan graphics engine designed for low-latency, high-fidelity rendering of 3D spatial data on mobile hardware.

## Key Features

- **Real-time ADS-B Integration**: Fetches live flight data via public APIs to track aircraft positions, altitudes, and callsigns within a configurable radius.
- **Vulkan-Powered AR Overlay**: A custom-engineered rendering pipeline that projects global aircraft coordinates into a local 3D screen-space, providing accurate visual pointers.
- **Aviation HUD**: A professional-grade Head-Up Display featuring a smoothed compass tape and heading indicators to assist in navigation and aircraft spotting.
- **Interactive Flight Cards**: Dynamic, billboarded UI elements that follow aircraft in 3D space. Users can interact with these cards to view detailed flight information.
- **Spatial Awareness**: Utilizes device gyroscopes and accelerometers with smoothed Euler-angle filtering to ensure a stable and jitter-free AR experience.
- **Achievement System**: An integrated set of milestones that reward users for spotting rare aircraft or achieving specific tracking goals.

## System Architecture

PlaneAR employs a hybrid architecture that balances the productivity of Kotlin with the performance of native C++.

### High-Level Flow
1. **Data Acquisition**: The Kotlin layer fetches aircraft data from ADS-B repositories.
2. **Spatial Projection**: Geographic coordinates (Latitude, Longitude, Altitude) are converted into relative 3D vectors based on the user's current GPS location.
3. **Orientation Mapping**: The app tracks the device's rotation matrix to determine the camera's forward, right, and up vectors.
4. **Native Rendering**: These vectors and aircraft positions are passed via JNI to the Vulkan engine, which renders the AR labels and HUD elements.

### Core Components
- **Kotlin Application Layer**: Handles API requests, state management, user preferences, and high-level scene switching.
- **JNI Bridge**: Provides a low-overhead communication channel between the JVM and the native graphics pipeline.
- **Vulkan Graphics Engine**: A modular C++ engine implementing a full Vulkan pipeline, including:
    - **Swapchain Management**: For synchronized frame presentation.
    - **Shader Pipeline**: Custom GLSL shaders for rendering billboards and UI elements.
    - **Texture Management**: Efficient handling of fonts and image assets.
    - **2D/3D Renderer**: Optimized drawing calls for spatial overlays and HUD components.

## Technologies Used

- **Languages**: Kotlin, C++20
- **Graphics API**: Vulkan
- **Android SDK**: Jetpack, NDK
- **Build Tools**: Gradle, CMake
- **CI/CD**: GitHub Actions

## Project Structure

```text
PlaneAR/
├── app/
│   ├── src/main/java/          # Kotlin source: App logic, AR pages, and ADS-B management
│   └── src/main/cpp/
│       ├── GraphicsEngine/     # Native Vulkan engine: Pipeline, RenderPass, and Shaders
│       └── source/             # JNI bridges and Native scene management
├── .github/workflows/          # Continuous Integration pipelines
└── BUILD.md                    # Detailed build and installation guide
```

## Building the Project

For detailed instructions on setting up the development environment, configuring the NDK, and deploying the app to a Vulkan-compatible device, please refer to the [Build Documentation](BUILD.md).

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
