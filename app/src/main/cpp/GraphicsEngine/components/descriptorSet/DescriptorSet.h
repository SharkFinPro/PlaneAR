#ifndef PLANEAR_DESCRIPTORSET_H
#define PLANEAR_DESCRIPTORSET_H

#include <vulkan/vulkan.h>
#include <functional>
#include <memory>
#include <vector>

namespace ge {

  class LogicalDevice;

  class DescriptorSet
  {
  public:
    DescriptorSet(const std::shared_ptr<LogicalDevice>& logicalDevice,
                  VkDescriptorPool descriptorPool,
                  const std::vector<VkDescriptorSetLayoutBinding>& layoutBindings);

    DescriptorSet(std::shared_ptr<LogicalDevice> logicalDevice,
                  VkDescriptorPool descriptorPool,
                  VkDescriptorSetLayout descriptorSetLayout);

    ~DescriptorSet();

    void updateDescriptorSets(const std::function<std::vector<VkWriteDescriptorSet>(VkDescriptorSet descriptorSet, size_t frame)>& getWriteDescriptorSets) const;

    [[nodiscard]] VkDescriptorSetLayout getDescriptorSetLayout() const;

    [[nodiscard]] VkDescriptorSet& getDescriptorSet(size_t frame);

  protected:
    std::shared_ptr<LogicalDevice> m_logicalDevice;

    VkDescriptorSetLayout m_descriptorSetLayout = VK_NULL_HANDLE;

    std::vector<VkDescriptorSet> m_descriptorSets;

    bool m_ownsLayout = false;

    void createDescriptorSetLayout(const std::vector<VkDescriptorSetLayoutBinding>& layoutBindings);

    void allocateDescriptorSets(VkDescriptorPool descriptorPool);
  };

} // ge

#endif //PLANEAR_DESCRIPTORSET_H
