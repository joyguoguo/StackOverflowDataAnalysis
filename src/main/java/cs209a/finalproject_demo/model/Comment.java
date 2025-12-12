package cs209a.finalproject_demo.model;

public record Comment(
        long id,
        long postId,
        String postType,
        Author owner,
        int score,
        long creationDateEpoch,
        String text,
        String contentLicense
) {
}










