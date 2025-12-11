package cs209a.finalproject_demo.dto;

import java.util.List;
import java.util.Map;

/**
 * 话题共现响应
 */
public record TopicCooccurrenceResponse(
        List<TopicPair> pairs,
        VennDiagramData vennDiagram
) {
    public TopicCooccurrenceResponse(List<TopicPair> pairs) {
        this(pairs, null);
    }

    /**
     * 话题对
     */
    public record TopicPair(
            List<String> topics,
            long count
    ) {
    }

    /**
     * 韦恩图数据
     * 用于展示前几个最强共现对的重叠关系
     */
    public record VennDiagramData(
            List<VennSet> sets,
            List<VennIntersection> intersections
    ) {
    }

    /**
     * 韦恩图集合（单个话题）
     */
    public record VennSet(
            String name,
            long size
    ) {
    }

    /**
     * 韦恩图交集（两个话题的共现）
     */
    public record VennIntersection(
            List<String> sets,
            long size
    ) {
    }
}
