#include "ShaderModule.h"
#include "../logicalDevice/LogicalDevice.h"
#include <android/asset_manager.h>
#include <stdexcept>

namespace ge {
  ShaderModule::ShaderModule(const std::shared_ptr<LogicalDevice>& logicalDevice,
                             AAssetManager* assetManager,
                             const char* filename,
                             VkShaderStageFlagBits stage)
    : m_logicalDevice(logicalDevice), m_stage(stage)
  {
    createShaderModule(assetManager, filename);
  }

  ShaderModule::~ShaderModule()
  {
    m_logicalDevice->destroyShaderModule(m_module);
  }

  VkPipelineShaderStageCreateInfo ShaderModule::getShaderStageCreateInfo() const
  {
    const VkPipelineShaderStageCreateInfo shaderStageCreateInfo {
      .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
      .stage = m_stage,
      .module = m_module,
      .pName = "main"
    };

    return shaderStageCreateInfo;
  }

  void ShaderModule::createShaderModule(AAssetManager* assetManager, const char* file)
  {
    const auto shaderCode = readAssetFile(assetManager, file);

    const VkShaderModuleCreateInfo shaderModuleCreateInfo {
      .sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO,
      .codeSize = shaderCode.size(),
      .pCode = reinterpret_cast<const uint32_t*>(shaderCode.data())
    };

    m_module = m_logicalDevice->createShaderModule(shaderModuleCreateInfo);
  }

  std::string ShaderModule::readAssetFile(AAssetManager* assetManager, const char* file)
  {
    AAsset* asset = AAssetManager_open(assetManager, file, AASSET_MODE_BUFFER);

    if (!asset)
    {
      throw std::runtime_error(std::string("Failed to open asset: ") + file);
    }

    const auto size = AAsset_getLength(asset);
    std::string data;
    data.resize(size);

    AAsset_read(asset, &data[0], size);
    AAsset_close(asset);

    return data;
  }
} // ge