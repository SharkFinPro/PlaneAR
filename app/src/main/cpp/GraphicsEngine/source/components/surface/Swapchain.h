#ifndef PLANEAR_SWAPCHAIN_H
#define PLANEAR_SWAPCHAIN_H

#include <vulkan/vulkan.h>
#include <memory>
#include <vector>

namespace ge {

  class LogicalDevice;
  class PhysicalDevice;
  class Surface;

  class Swapchain
  {
  public:
    Swapchain(const std::shared_ptr<LogicalDevice>& logicalDevice,
              const std::shared_ptr<Surface>& surface);

    ~Swapchain();

    [[nodiscard]] VkFormat& getImageFormat();

    [[nodiscard]] VkExtent2D& getExtent();

    [[nodiscard]] VkSwapchainKHR& getSwapChain();

    [[nodiscard]] std::vector<VkImageView>& getImageViews();

    [[nodiscard]] std::vector<VkImage>& getImages();

    [[nodiscard]] uint32_t getImageCount() const;

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;
    std::shared_ptr<Surface> m_surface;

    VkSwapchainKHR m_swapchain = VK_NULL_HANDLE;

    std::vector<VkImage> m_swapChainImages;
    VkFormat m_swapChainImageFormat = VK_FORMAT_UNDEFINED;
    VkExtent2D m_swapChainExtent{};
    std::vector<VkImageView> m_swapChainImageViews;

    static VkSurfaceFormatKHR chooseSwapSurfaceFormat(const std::vector<VkSurfaceFormatKHR>& availableFormats);

    static VkPresentModeKHR chooseSwapPresentMode(const std::vector<VkPresentModeKHR>& availablePresentModes);

    [[nodiscard]] VkExtent2D chooseSwapExtent(const VkSurfaceCapabilitiesKHR& capabilities) const;

    static uint32_t chooseSwapImageCount(const VkSurfaceCapabilitiesKHR& capabilities);

    void createSwapChain();

    void createImageViews();

    void destroyImageViews();
  };

} // ge

#endif //PLANEAR_SWAPCHAIN_H
