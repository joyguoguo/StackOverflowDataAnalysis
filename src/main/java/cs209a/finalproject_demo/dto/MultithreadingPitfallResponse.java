package cs209a.finalproject_demo.dto;

import java.util.List;

/**
 * Flat response for multithreading pitfalls.
 * Each entry represents one fixed category (P1–P9) with its question count.
 */
public record MultithreadingPitfallResponse(List<PitfallStat> pitfalls) {

    public record PitfallStat(
            String code,      // e.g. "P1"
            String label,     // e.g. "Race Condition"
            long count,       // number of questions classified into this category
            List<Long> examples // sample question ids
    ) {
    }
}

