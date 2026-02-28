#include "AssetManager.h"
#include "textures/ImageTexture.h"
#include "../logicalDevice/LogicalDevice.h"
#include "../physicalDevice/PhysicalDevice.h"

namespace ge {
  AssetManager::AssetManager(std::shared_ptr<LogicalDevice> logicalDevice,
                             AAssetManager* aassetManager)
    : m_logicalDevice(std::move(logicalDevice)), m_aassetManager(aassetManager)
  {
    createCommandPool();

    createDescriptorPool();

    createDescriptorSetLayouts();
  }

  AssetManager::~AssetManager()
  {
    m_logicalDevice->destroyDescriptorSetLayout(m_imageDescriptorSetLayout);

    m_logicalDevice->destroyDescriptorSetLayout(m_fontDescriptorSetLayout);

    m_logicalDevice->destroyDescriptorPool(m_descriptorPool);

    m_logicalDevice->destroyCommandPool(m_commandPool);
  }

  AAssetManager* AssetManager::getAAssetManager() const
  {
    return m_aassetManager;
  }

  void AssetManager::registerFont(std::string fontName,
                                  std::string fontPath,
                                  const CharsetMode charsetMode)
  {
    m_fontNames.insert({ std::move(fontName), FontRegistration{ std::move(fontPath), charsetMode } });
  }

  void AssetManager::preloadFont(const std::string& fontName,
                                 const uint32_t fontSize)
  {
    const FontKey key { fontName, fontSize };
    if (m_fonts.find(key) == m_fonts.end())
    {
      loadFont(fontName, fontSize);
    }
  }

  std::shared_ptr<Font> AssetManager::getFont(const std::string& fontName,
                                              uint32_t fontSize)
  {
    const FontKey key { fontName, fontSize };

    auto font = m_fonts.find(key);

    if (font == m_fonts.end())
    {
      loadFont(fontName, fontSize);

      font = m_fonts.find(key);
    }

    return font->second;
  }

  VkDescriptorSetLayout AssetManager::getFontDescriptorSetLayout() const
  {
    return m_fontDescriptorSetLayout;
  }

  void AssetManager::registerImage(std::string imageName,
                                   std::string imagePath)
  {
    m_imageNames.insert({ std::move(imageName), std::move(imagePath) });
  }

  std::shared_ptr<ImageTexture> AssetManager::getImage(const std::string& imageName)
  {
    auto image = m_images.find(imageName);

    if (image == m_images.end())
    {
      loadImage(imageName);

      image = m_images.find(imageName);
    }

    return image->second;
  }

  VkDescriptorSetLayout AssetManager::getImageDescriptorSetLayout() const
  {
    return m_imageDescriptorSetLayout;
  }

  void AssetManager::createDescriptorSetLayouts()
  {
    createFontDescriptorSetLayout();

    createImageDescriptorSetLayout();
  }

  void AssetManager::createFontDescriptorSetLayout()
  {
    constexpr VkDescriptorSetLayoutBinding glyphDescriptorSetLayoutBinding {
      .binding = 0,
      .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
      .descriptorCount = 1,
      .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT
    };

    constexpr std::array descriptorSetLayoutBindings {
      glyphDescriptorSetLayoutBinding
    };

    const VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
      .bindingCount = static_cast<uint32_t>(descriptorSetLayoutBindings.size()),
      .pBindings = descriptorSetLayoutBindings.data()
    };

    m_fontDescriptorSetLayout = m_logicalDevice->createDescriptorSetLayout(descriptorSetLayoutCreateInfo);
  }

  void AssetManager::createImageDescriptorSetLayout()
  {
    constexpr VkDescriptorSetLayoutBinding imageDescriptorSetLayoutBinding {
      .binding = 0,
      .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
      .descriptorCount = 1,
      .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT
    };

    constexpr std::array descriptorSetLayoutBindings {
      imageDescriptorSetLayoutBinding
    };

    const VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
      .bindingCount = static_cast<uint32_t>(descriptorSetLayoutBindings.size()),
      .pBindings = descriptorSetLayoutBindings.data()
    };

    m_imageDescriptorSetLayout = m_logicalDevice->createDescriptorSetLayout(descriptorSetLayoutCreateInfo);
  }

  void AssetManager::loadFont(const std::string& fontName,
                              uint32_t fontSize)
  {
    const auto fontEntry = m_fontNames.find(fontName);

    if (fontEntry == m_fontNames.end())
    {
      throw std::runtime_error("Font not found: " + fontName);
    }

    auto font = std::make_shared<Font>(
      m_logicalDevice,
      m_aassetManager,
      fontEntry->second.path,
      fontSize,
      m_commandPool,
      m_descriptorPool,
      m_fontDescriptorSetLayout,
      fontEntry->second.charsetMode
    );

    m_fonts.emplace(FontKey{ fontName, fontSize }, std::move(font));
  }

  void AssetManager::loadImage(const std::string& imageName)
  {
    const auto imagePath = m_imageNames.find(imageName);

    if (imagePath == m_imageNames.end())
    {
      throw std::runtime_error("Image not found: " + imageName);
    }

    auto image = std::make_shared<ImageTexture>(
      m_logicalDevice,
      m_aassetManager,
      imagePath->second,
      m_commandPool,
      m_descriptorPool,
      m_imageDescriptorSetLayout
    );

    m_images.emplace(imageName, std::move(image));
  }

  void AssetManager::createCommandPool()
  {
    const VkCommandPoolCreateInfo poolInfo {
      .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
      .queueFamilyIndex = m_logicalDevice->getPhysicalDevice()->getQueueFamilies().graphicsFamily.value()
    };

    m_commandPool = m_logicalDevice->createCommandPool(poolInfo);
  }

  void AssetManager::createDescriptorPool()
  {
    const std::array<VkDescriptorPoolSize, 2> poolSizes {{
      {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, m_logicalDevice->getMaxFramesInFlight() * 30},
      {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, m_logicalDevice->getMaxFramesInFlight() * 10}
    }};

    const VkDescriptorPoolCreateInfo poolCreateInfo {
      .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
      .maxSets = m_logicalDevice->getMaxFramesInFlight() * 1000,
      .poolSizeCount = static_cast<uint32_t>(poolSizes.size()),
      .pPoolSizes = poolSizes.data()
    };

    m_descriptorPool = m_logicalDevice->createDescriptorPool(poolCreateInfo);
  }
} // ge