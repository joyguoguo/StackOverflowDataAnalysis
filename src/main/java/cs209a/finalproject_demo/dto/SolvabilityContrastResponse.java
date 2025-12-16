package cs209a.finalproject_demo.dto;

import java.util.List;

public record SolvabilityContrastResponse(
        List<FeatureComparison> comparison_data,
        List<TagFrequencyData> tag_frequency_data,
        CommentFrequencyData comment_frequency_data,
        DistributionData code_snippet_ratio_distribution,
        DistributionData tag_count_distribution,
        DistributionData question_length_distribution,
        DistributionData reputation_distribution,
        DistributionData comment_count_distribution,
        BoxPlotData reputation_boxplot_data
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
    
    public record DistributionData(
            List<String> bins,  // 区间标签
            List<Double> solvable_frequencies,  // 易解决组的频率（百分比）
            List<Double> hard_frequencies  // 难解决组的频率（百分比）
    ) {
    }
    
    public record BoxPlotData(
            BoxPlotStats solvable,
            BoxPlotStats hard
    ) {
    }
    
    public record BoxPlotStats(
            double min,
            double q1,      // 第一四分位数
            double median,  // 中位数
            double q3,      // 第三四分位数
            double max,
            List<Double> outliers  // 异常值列表
    ) {
    }
}




















