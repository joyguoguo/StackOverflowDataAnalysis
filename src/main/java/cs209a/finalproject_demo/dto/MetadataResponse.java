package cs209a.finalproject_demo.dto;

import java.time.LocalDate;

public record MetadataResponse(
        int threadCount,
        int answerCount,
        int commentCount,
        LocalDate earliestQuestion,
        LocalDate latestQuestion
) {
}































