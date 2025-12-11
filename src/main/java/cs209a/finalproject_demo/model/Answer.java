package cs209a.finalproject_demo.model;

public record Answer(
        long id,
        long questionId,
        Author owner,
        int score,
        boolean accepted,
        long creationDateEpoch
) {
}










