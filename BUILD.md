# Building PlaneAR

This document provides detailed instructions for setting up, building, and running the PlaneAR project on an Android device.

## Prerequisites

To build PlaneAR, you will need the following tools and environment:

### Software Requirements
- **Android Studio**: The latest stable version is recommended.
- **Android NDK (Native Development Kit)**: Required for compiling the Vulkan-based `GraphicsEngine`.
- **CMake**: Required for managing the native build process.
- **JDK 17+**: Required for Gradle build scripts.

### Hardware Requirements
- **Android Device**: A physical Android device with **Vulkan API support**. Note that the Android Emulator may not reliably support all Vulkan features required by the custom graphics engine.
- **USB Debugging**: Enabled in Developer Options on the target device.

## Installation and Setup

### 1. Clone the Repository
Clone the project to your local machine:

```bash
git clone https://github.com/SharkFinPro/PlaneAR.git
cd PlaneAR
```

### 2. Import into Android Studio
1. Launch **Android Studio**.
2. Select **Open** and navigate to the `PlaneAR` root directory.
3. Allow Android Studio to import the project and sync the Gradle files.

### 3. Configure NDK and CMake
Ensure that the NDK and CMake are correctly configured in Android Studio:
- Go to `File` $\rightarrow$ `Project Structure` $\rightarrow$ `SDK Location`.
- Verify that the **Android NDK location** is set correctly.
- The project uses Gradle's integrated CMake support, so no manual CMake installation on the host OS is typically required beyond what Android Studio provides.

## Running the Application

### Build and Deploy
1. Connect your Android device via USB or WiFi.
2. Select your device from the target device dropdown in the toolbar.
3. Click the **Run** button (green arrow) or press `Shift + F10`.

Android Studio will compile the Kotlin source code, invoke CMake to build the native `GraphicsEngine` library, package the APK, and install it on your device.

## Project Architecture Overview for Developers

### Native Engine Build Process
The project utilizes a hybrid build system:
- **Gradle** manages the overall Android application lifecycle and dependencies.
- **CMake** handles the compilation of the Vulkan `GraphicsEngine`.
- The native library is linked via JNI, providing the Kotlin layer with high-performance rendering capabilities.

### Continuous Integration
The project includes a GitHub Actions workflow (`.github/workflows/android-ci.yml`) that automatically validates the build on every push to ensure stability.
