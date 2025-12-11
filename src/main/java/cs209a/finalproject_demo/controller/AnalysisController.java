package cs209a.finalproject_demo.controller;

import cs209a.finalproject_demo.dto.ApiResponse;
import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse;
import cs209a.finalproject_demo.dto.TopicTrendResponse;
import cs209a.finalproject_demo.service.MultithreadingInsightService;
import cs209a.finalproject_demo.service.SolvabilityContrastService;
import cs209a.finalproject_demo.service.TopicCooccurrenceService;
import cs209a.finalproject_demo.service.TopicTrendService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class AnalysisController {

    private final TopicTrendService topicTrendService;
    private final TopicCooccurrenceService topicCooccurrenceService;
    private final MultithreadingInsightService multithreadingInsightService;
    private final SolvabilityContrastService solvabilityContrastService;

    public AnalysisController(TopicTrendService topicTrendService,
                              TopicCooccurrenceService topicCooccurrenceService,
                              MultithreadingInsightService multithreadingInsightService,
                              SolvabilityContrastService solvabilityContrastService) {
        this.topicTrendService = topicTrendService;
        this.topicCooccurrenceService = topicCooccurrenceService;
        this.multithreadingInsightService = multithreadingInsightService;
        this.solvabilityContrastService = solvabilityContrastService;
    }

    @GetMapping("/topic-trends")
    public ApiResponse<TopicTrendResponse> topicTrends(
            @RequestParam(required = false) List<String> topics,
            @RequestParam(defaultValue = "QUESTIONS") String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "8") @Min(1) @Max(50) int topN) {
        TopicTrendService.Metric selectedMetric = TopicTrendService.Metric.from(metric);
        TopicTrendResponse response = topicTrendService.analyze(topics, selectedMetric, from, to, topN);
        return ApiResponse.of(response);
    }

    @GetMapping("/cooccurrence")
    public ApiResponse<TopicCooccurrenceResponse> cooccurrence(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int topN) {
        TopicCooccurrenceResponse response = topicCooccurrenceService.topPairs(topN);
        return ApiResponse.of(response);
    }

    @GetMapping("/multithreading/pitfalls")
    public ApiResponse<MultithreadingPitfallResponse> multithreadingPitfalls(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topN) {
        MultithreadingPitfallResponse response = multithreadingInsightService.analyze(topN);
        return ApiResponse.of(response);
    }

    @GetMapping("/solvability/contrast")
    public ApiResponse<SolvabilityContrastResponse> solvabilityContrast(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SolvabilityContrastResponse response = solvabilityContrastService.analyze(from, to);
        return ApiResponse.of(response);
    }
}

