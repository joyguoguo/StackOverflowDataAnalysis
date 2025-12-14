package cs209a.finalproject_demo.dto;

import java.util.List;

/**
 * 话题共现响应
 * 用于力导向图可视化
 */
public record TopicCooccurrenceResponse(
        List<TopicPair> pairs
) {
    /**
     * 话题对
     * 符合要求的 JSON 结构：{ "topic_pair": [...], "frequency": ... }
     */
    public record TopicPair(
            List<String> topic_pair,  // 话题对列表
            long frequency             // 共现频率
    ) {
    }
}
