# Contributing to PlaneAR

First off, thank you for considering contributing to PlaneAR! It is wonderful to have community members interested in improving real-time aircraft tracking and augmented reality visualization.

To maintain a high standard of code quality and project stability, we ask all contributors to follow these guidelines.

## 🚀 How to Contribute

### Reporting Bugs
If you encounter a bug, please open an [Issue](https://github.com/SharkFinPro/PlaneAR/issues) on GitHub. To help us resolve the issue quickly, please include:
- **A clear description** of the bug.
- **Steps to reproduce** the behavior (including device model and Android version).
- **Expected behavior** vs. actual behavior.
- **Screenshots or logs** if applicable.

### Suggesting Enhancements
We welcome ideas for new features or improvements! Please open an issue and label it as `enhancement`. Be as specific as possible about the problem you are solving or the value the feature adds to the user experience.

### Contributing Code
If you'd like to contribute code, please follow the process below:

1. **Fork the Repository**: Create your own fork of the project.
2. **Create a Feature Branch**: Use a descriptive name for your branch (e.g., `feature/improve-hud-smoothing` or `fix/jni-memory-leak`).
3. **Implement Your Changes**:
    - Ensure your code follows the existing project style.
    - For native changes in the `GraphicsEngine`, ensure you maintain the Vulkan abstraction layer.
    - For Kotlin changes, follow standard Android development patterns.
4. **Test Your Work**: Verify that your changes work on a physical Vulkan-compatible Android device.
5. **Submit a Pull Request**: Open a PR against the `main` branch.

## 🛠 Pull Request Guidelines

To ensure your PR is reviewed and merged efficiently, please adhere to the following:

- **Atomic Commits**: Keep commits small and focused on a single logical change.
- **Clear Descriptions**: Explain *what* changed and *why*. If the PR fixes an issue, reference it using `Closes #issue_number`.
- **No Regressions**: Ensure that existing functionality remains intact.
- **Clean Code**: Remove any debug logs or commented-out code before submitting.

All pull requests require at least one review and approval from a maintainer before being merged.

## 📐 Coding Standards

### Native (C++/Vulkan)
- **C++20**: We utilize C++20 features for the graphics engine.
- **Resource Management**: Strict adherence to Vulkan resource ownership and lifecycle management is required to prevent memory leaks on mobile hardware.

### Kotlin/Android
- **Modern Android**: We target modern Android API levels. Use Jetpack libraries where appropriate.
- **Performance**: Be mindful of the main thread, especially when interfacing with the JNI bridge or processing ADS-B data.

## 📚 Setup and Development

For detailed instructions on how to set up your development environment and build the project, please refer to the [Build Documentation](BUILD.md).

---

Thank you for helping make PlaneAR better for everyone!
