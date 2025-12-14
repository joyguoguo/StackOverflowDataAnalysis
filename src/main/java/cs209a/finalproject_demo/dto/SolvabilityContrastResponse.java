package cs209a.finalproject_demo.dto;

import java.util.List;

public record SolvabilityContrastResponse(List<FeatureStats> features) {

    public record FeatureStats(String name, DistributionStats solvable, DistributionStats hard) {
    }

    public record DistributionStats(double average, double median, double min, double max) {
        public static DistributionStats empty() {
            return new DistributionStats(0, 0, 0, 0);
        }
    }
}




















