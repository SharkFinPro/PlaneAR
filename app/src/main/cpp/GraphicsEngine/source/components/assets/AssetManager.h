#ifndef PLANEAR_ASSETMANAGER_H
#define PLANEAR_ASSETMANAGER_H

#include <vulkan/vulkan.h>
#include <memory>
#include <string>
#include <unordered_map>

struct AAssetManager;

namespace ge {

  class Font;
  class LogicalDevice;

  struct FontKey {
    std::string name;
    uint32_t size;

    bool operator==(const FontKey& other) const
    {
      return name == other.name && size == other.size;
    }
  };

  struct FontKeyHash {
    std::size_t operator()(const FontKey& key) const
    {
      const std::size_t h1 = std::hash<std::string>{}(key.name);
      const std::size_t h2 = std::hash<uint32_t>{}(key.size);

      return h1 ^ (h2 << 1);
    }
  };

  class AssetManager
  {
  public:
    AssetManager(std::shared_ptr<LogicalDevice> logicalDevice,
                 AAssetManager* aassetManager);

    ~AssetManager();

    [[nodiscard]] AAssetManager* getAAssetManager() const;

    void registerFont(std::string fontName,
                      std::string fontPath);

    [[nodiscard]] std::shared_ptr<Font> getFont(const std::string& fontName,
                                                uint32_t fontSize);

    [[nodiscard]] VkDescriptorSetLayout getFontDescriptorSetLayout() const;

    void registerImage(std::string imageName,
                       std::string imagePath);

    [[nodiscard]] VkDescriptorSetLayout getImageDescriptorSetLayout() const;

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    AAssetManager* m_aassetManager = nullptr;

    VkCommandPool m_commandPool = VK_NULL_HANDLE;

    VkDescriptorPool m_descriptorPool = VK_NULL_HANDLE;

    VkDescriptorSetLayout m_fontDescriptorSetLayout = VK_NULL_HANDLE;

    std::unordered_map<std::string, std::string> m_fontNames;
    std::unordered_map<FontKey, std::shared_ptr<Font>, FontKeyHash> m_fonts;

    VkDescriptorSetLayout m_imageDescriptorSetLayout = VK_NULL_HANDLE;

    std::unordered_map<std::string, std::string> m_imageNames;

    void createDescriptorSetLayouts();

    void createFontDescriptorSetLayout();

    void loadFont(const std::string& fontName,
                  uint32_t fontSize);

    void createCommandPool();

    void createDescriptorPool();
  };

} // ge

#endif //PLANEAR_ASSETMANAGER_H
