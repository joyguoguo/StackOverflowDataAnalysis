package cs209a.finalproject_demo.model;

public record Answer(
        long id,
        long questionId,
        String body,
        Author owner,
        int score,
        boolean accepted,
        long creationDateEpoch,
        Long lastActivityDateEpoch,
        String contentLicense
) {
}










