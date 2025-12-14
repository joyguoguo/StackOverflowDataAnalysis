package cs209a.finalproject_demo.dto;

import java.util.List;

public record MultithreadingPitfallResponse(List<CategoryGroup> categories) {
    /**
     * 大类分组，包含大类名称和其下的细项
     */
    public record CategoryGroup(String category, long totalCount, List<PitfallDetail> pitfalls) {
    }
    
    /**
     * 细项详情，包含细项名称、数量和示例问题ID
     */
    public record PitfallDetail(String label, long count, List<Long> examples) {
    }
}
