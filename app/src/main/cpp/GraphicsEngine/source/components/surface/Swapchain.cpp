#include "Swapchain.h"
#include "Surface.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"
#include "../../utilities/Images.h"
#include <algorithm>

namespace ge {
  Swapchain::Swapchain(const std::shared_ptr<LogicalDevice>& logicalDevice,
                       const std::shared_ptr<Surface>& surface)
    : m_logicalDevice(logicalDevice), m_surface(surface)
  {
    createSwapChain();

    createImageViews();
  }

  Swapchain::~Swapchain()
  {
    destroyImageViews();

    m_logicalDevice->destroySwapchainKHR(m_swapchain);
  }

  VkFormat& Swapchain::getImageFormat()
  {
    return m_swapChainImageFormat;
  }

  VkExtent2D& Swapchain::getExtent()
  {
    return m_swapChainExtent;
  }

  VkSwapchainKHR& Swapchain::getSwapChain()
  {
    return m_swapchain;
  }

  std::vector<VkImageView>& Swapchain::getImageViews()
  {
    return m_swapChainImageViews;
  }

  std::vector<VkImage>& Swapchain::getImages()
  {
    return m_swapChainImages;
  }

  uint32_t Swapchain::getImageCount() const
  {
    return m_swapChainImages.size();
  }

  VkSurfaceFormatKHR Swapchain::chooseSwapSurfaceFormat(const std::vector<VkSurfaceFormatKHR>& availableFormats)
  {
    for (const auto& availableFormat : availableFormats)
    {
      if (availableFormat.format == VK_FORMAT_B8G8R8A8_UNORM && availableFormat.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
      {
        return availableFormat;
      }
    }

    return availableFormats[0];
  }

  VkPresentModeKHR Swapchain::chooseSwapPresentMode(const std::vector<VkPresentModeKHR>& availablePresentModes)
  {
    for (const auto& availablePresentMode : availablePresentModes)
    {
      if (availablePresentMode == VK_PRESENT_MODE_MAILBOX_KHR)
      {
        return availablePresentMode;
      }
    }

    return VK_PRESENT_MODE_FIFO_KHR;
  }

  VkExtent2D Swapchain::chooseSwapExtent(const VkSurfaceCapabilitiesKHR& capabilities) const
  {
    if (capabilities.currentExtent.width != std::numeric_limits<uint32_t>::max())
    {
      return capabilities.currentExtent;
    }

    const VkExtent2D actualExtent {
      .width = std::clamp(static_cast<uint32_t>(m_surface->getWidth()),
                          capabilities.minImageExtent.width,
                          capabilities.maxImageExtent.width),
      .height = std::clamp(static_cast<uint32_t>(m_surface->getHeight()),
                           capabilities.minImageExtent.height,
                           capabilities.maxImageExtent.height)
    };

    return actualExtent;
  }

  uint32_t Swapchain::chooseSwapImageCount(const VkSurfaceCapabilitiesKHR& capabilities)
  {
    const uint32_t imageCount = capabilities.minImageCount + 1;

    const bool imageCountExceeded = capabilities.maxImageCount && imageCount > capabilities.maxImageCount;

    return imageCountExceeded ? capabilities.maxImageCount : imageCount;
  }

  void Swapchain::createSwapChain()
  {
    const SwapChainSupportDetails swapChainSupport = m_logicalDevice->getPhysicalDevice()->getSwapChainSupport();

    VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
    const VkPresentModeKHR presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
    const VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);
    uint32_t imageCount = chooseSwapImageCount(swapChainSupport.capabilities);

    const auto indices = m_logicalDevice->getPhysicalDevice()->getQueueFamilies();
    const uint32_t queueFamilyIndices[] = {
      indices.graphicsFamily.value(),
      indices.presentFamily.value()
    };

    const VkSwapchainCreateInfoKHR createInfo {
      .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
      .surface = m_surface->getSurface(),
      .minImageCount = imageCount,
      .imageFormat = surfaceFormat.format,
      .imageColorSpace = surfaceFormat.colorSpace,
      .imageExtent = extent,
      .imageArrayLayers = 1,
      .imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
      .imageSharingMode = indices.graphicsFamily != indices.presentFamily ? VK_SHARING_MODE_CONCURRENT : VK_SHARING_MODE_EXCLUSIVE,
      .queueFamilyIndexCount = static_cast<uint32_t>(indices.graphicsFamily != indices.presentFamily ? 2 : 1),
      .pQueueFamilyIndices = indices.graphicsFamily != indices.presentFamily ? queueFamilyIndices : nullptr,
      .preTransform = swapChainSupport.capabilities.currentTransform,
      .compositeAlpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR,
      .presentMode = presentMode,
      .clipped = VK_TRUE,
      .oldSwapchain = VK_NULL_HANDLE
    };

    m_swapchain = m_logicalDevice->createSwapchain(createInfo);

    m_logicalDevice->getSwapchainImagesKHR(m_swapchain, &imageCount, nullptr);
    m_swapChainImages.resize(imageCount);
    m_logicalDevice->getSwapchainImagesKHR(m_swapchain, &imageCount, m_swapChainImages.data());

    m_swapChainImageFormat = surfaceFormat.format;
    m_swapChainExtent = extent;
  }

  void Swapchain::createImageViews()
  {
    m_swapChainImageViews.resize(m_swapChainImages.size());

    for (size_t i = 0; i < m_swapChainImages.size(); i++)
    {
      m_swapChainImageViews[i] = Images::createImageView(
        m_logicalDevice,
        m_swapChainImages[i],
        m_swapChainImageFormat,
        VK_IMAGE_ASPECT_COLOR_BIT,
        1,
        VK_IMAGE_VIEW_TYPE_2D,
        1
      );
    }
  }

  void Swapchain::destroyImageViews()
  {
    for (auto& imageView : m_swapChainImageViews)
    {
      m_logicalDevice->destroyImageView(imageView);
    }
  }
} // ge