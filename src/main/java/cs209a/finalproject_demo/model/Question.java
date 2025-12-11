package cs209a.finalproject_demo.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record Question(
        long id,
        String title,
        String body,
        List<String> tags,
        Author owner,
        boolean answered,
        int answerCount,
        int score,
        long creationDateEpoch,
        long lastActivityDateEpoch,
        Integer acceptedAnswerId,
        int viewCount
) {
    public Instant creationInstant() {
        return Instant.ofEpochSecond(creationDateEpoch);
    }

    public LocalDate creationDate(ZoneId zoneId) {
        return creationInstant().atZone(zoneId).toLocalDate();
    }

    public String normalizedTitle() {
        return title == null ? "" : title.toLowerCase();
    }

    public String normalizedBody() {
        if (body == null || body.isEmpty()) {
            return "";
        }
        // 移除 HTML 标签，只保留文本内容（简单处理）
        return body.replaceAll("<[^>]+>", " ").toLowerCase();
    }

    public String fullText() {
        return normalizedTitle() + " " + normalizedBody();
    }
}






