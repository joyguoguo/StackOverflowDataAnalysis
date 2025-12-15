package cs209a.finalproject_demo.dto;

import java.util.List;

public record SolvabilityContrastResponse(
        List<FeatureComparison> comparison_data,
        List<TagFrequencyData> tag_frequency_data,
        CommentFrequencyData comment_frequency_data
) {

    public record FeatureComparison(
            String feature_name,
            double solvable_group,
            double hard_group,
            String unit
    ) {
    }
    
    public record TagFrequencyData(
            String tag_name,
            int solvable_count,
            int hard_count
    ) {
    }
    
    public record CommentFrequencyData(
            int solvable_with_comments,
            int solvable_total,
            int hard_with_comments,
            int hard_total
    ) {
    }
}




















