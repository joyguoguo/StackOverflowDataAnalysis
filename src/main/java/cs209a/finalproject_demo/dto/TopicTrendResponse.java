package cs209a.finalproject_demo.dto;

import java.time.LocalDate;
import java.util.List;

public record TopicTrendResponse(
        List<TopicSeries> series,
        String metric,
        String bucket,
        LocalDate from,
        LocalDate to
) {
    public static TopicTrendResponse empty(String metric) {
        return new TopicTrendResponse(List.of(), metric, "MONTH", LocalDate.now(), LocalDate.now());
    }

    public record TopicSeries(String topic, List<DataPoint> points) {
    }

    public record DataPoint(String bucket, double value) {
    }
}

