#ifndef PLANEAR_SHADERMODULE_H
#define PLANEAR_SHADERMODULE_H

#include <vulkan/vulkan.h>
#include <memory>
#include <vector>

struct AAssetManager;

namespace ge {

  class LogicalDevice;

  class ShaderModule
  {
  public:
    ShaderModule(const std::shared_ptr<LogicalDevice>& logicalDevice,
                 AAssetManager* assetManager,
                 const char* filename,
                 VkShaderStageFlagBits stage);

    ~ShaderModule();

    ShaderModule(ShaderModule&& other) noexcept
      : m_logicalDevice(other.m_logicalDevice),
        m_stage(other.m_stage),
        m_module(other.m_module)
    {
      other.m_module = VK_NULL_HANDLE;
    }

    [[nodiscard]] VkPipelineShaderStageCreateInfo getShaderStageCreateInfo() const;

  private:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkShaderStageFlagBits m_stage{};
    VkShaderModule m_module = VK_NULL_HANDLE;

    void createShaderModule(AAssetManager* assetManager, const char* file);

    [[nodiscard]] static std::string readAssetFile(AAssetManager* assetManager, const char* file);
  };

} // ge

#endif //PLANEAR_SHADERMODULE_H
