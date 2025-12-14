package cs209a.finalproject_demo.dto;

import java.util.List;

public record MultithreadingPitfallResponse(List<PitfallInsight> pitfalls) {
    public record PitfallInsight(String label, long count, List<Long> examples) {
    }
}


















