package cs209a.finalproject_demo.model;

public record Author(
        long accountId,
        Long userId,
        String displayName,
        int reputation,
        String userType,
        String profileImage,
        String link
) {
    public String safeDisplayName() {
        return displayName == null ? "anonymous" : displayName;
    }
}










