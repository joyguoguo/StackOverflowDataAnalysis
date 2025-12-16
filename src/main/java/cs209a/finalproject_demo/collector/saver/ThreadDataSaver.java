package cs209a.finalproject_demo.collector.saver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将采集的线程数据保存为 JSON 文件
 * 格式与 Sample_SO_data 中的文件格式一致
 */
@Component
public class ThreadDataSaver {

    private static final Logger log = LoggerFactory.getLogger(ThreadDataSaver.class);
    private final ObjectMapper objectMapper;

    public ThreadDataSaver() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 保存线程数据到指定目录
     * 
     * @param outputDir 输出目录
     * @param threadData 线程数据（JSON 格式）
     * @param threadIndex 线程索引（用于生成文件名）
     * @return 保存的文件路径
     */
    public Path saveThread(String outputDir, ObjectNode threadData, int threadIndex) throws IOException {
        Path outputPath = Paths.get(outputDir);
        
        // 确保输出目录存在
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            log.info("Created output directory: {}", outputPath);
        }

        // 生成文件名：thread_01.json, thread_02.json, ...
        String fileName = String.format("thread_%02d.json", threadIndex);
        Path filePath = outputPath.resolve(fileName);

        // 写入文件（格式化为缩进的 JSON）
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), threadData);
        
        log.debug("Saved thread to: {}", filePath);
        return filePath;
    }

    /**
     * 构建完整的线程 JSON 对象（包含问题、回答、评论）
     */
    public ObjectNode buildThreadJson(com.fasterxml.jackson.databind.JsonNode questionNode,
                                     List<com.fasterxml.jackson.databind.JsonNode> answers,
                                     List<com.fasterxml.jackson.databind.JsonNode> questionComments,
                                     Map<Long, List<com.fasterxml.jackson.databind.JsonNode>> answerComments) {
        ObjectNode root = objectMapper.createObjectNode();
        
        // 问题节点（可能需要转换字段名）
        root.set("question", normalizeQuestionNode(questionNode));
        
        // 回答数组
        ArrayNode answersArray = objectMapper.createArrayNode();
        for (com.fasterxml.jackson.databind.JsonNode answer : answers) {
            answersArray.add(normalizeAnswerNode(answer));
        }
        root.set("answers", answersArray);
        
        // 问题评论数组
        ArrayNode questionCommentsArray = objectMapper.createArrayNode();
        for (com.fasterxml.jackson.databind.JsonNode comment : questionComments) {
            questionCommentsArray.add(normalizeCommentNode(comment));
        }
        root.set("question_comments", questionCommentsArray);
        
        // 回答评论对象（key 为 answer_id）
        ObjectNode answerCommentsObj = objectMapper.createObjectNode();
        answerComments.forEach((answerId, comments) -> {
            ArrayNode commentArray = objectMapper.createArrayNode();
            for (com.fasterxml.jackson.databind.JsonNode comment : comments) {
                commentArray.add(normalizeCommentNode(comment));
            }
            answerCommentsObj.set(String.valueOf(answerId), commentArray);
        });
        root.set("answer_comments", answerCommentsObj);
        
        return root;
    }

    /**
     * 规范化问题节点（确保字段名与现有格式一致）
     */
    private com.fasterxml.jackson.databind.JsonNode normalizeQuestionNode(com.fasterxml.jackson.databind.JsonNode questionNode) {
        if (questionNode == null) {
            return null;
        }
        
        ObjectNode normalized = objectMapper.createObjectNode();
        
        // 直接复制字段（API 返回的字段名通常已符合要求）
        copyField(questionNode, normalized, "question_id");
        copyField(questionNode, normalized, "title");
        copyField(questionNode, normalized, "body");
        copyField(questionNode, normalized, "tags", n -> {
            ArrayNode tagsArray = objectMapper.createArrayNode();
            if (n.isArray()) {
                n.forEach(tag -> tagsArray.add(tag.asText().toLowerCase(Locale.ROOT)));
            }
            return tagsArray;
        });
        copyField(questionNode, normalized, "owner");
        copyField(questionNode, normalized, "is_answered");
        copyField(questionNode, normalized, "view_count");
        copyField(questionNode, normalized, "closed_date");
        copyField(questionNode, normalized, "answer_count");
        copyField(questionNode, normalized, "score");
        copyField(questionNode, normalized, "last_activity_date");
        copyField(questionNode, normalized, "creation_date");
        copyField(questionNode, normalized, "link");
        copyField(questionNode, normalized, "closed_reason");
        copyField(questionNode, normalized, "content_license");
        
        return normalized;
    }

    /**
     * 规范化回答节点
     */
    private com.fasterxml.jackson.databind.JsonNode normalizeAnswerNode(com.fasterxml.jackson.databind.JsonNode answerNode) {
        if (answerNode == null) {
            return null;
        }
        
        ObjectNode normalized = objectMapper.createObjectNode();
        
        copyField(answerNode, normalized, "answer_id");
        copyField(answerNode, normalized, "question_id");
        copyField(answerNode, normalized, "body");
        copyField(answerNode, normalized, "score");
        copyField(answerNode, normalized, "is_accepted");
        copyField(answerNode, normalized, "owner");
        copyField(answerNode, normalized, "creation_date");
        copyField(answerNode, normalized, "last_activity_date");
        copyField(answerNode, normalized, "content_license");
        
        return normalized;
    }

    /**
     * 规范化评论节点
     */
    private com.fasterxml.jackson.databind.JsonNode normalizeCommentNode(com.fasterxml.jackson.databind.JsonNode commentNode) {
        if (commentNode == null) {
            return null;
        }
        
        ObjectNode normalized = objectMapper.createObjectNode();
        
        copyField(commentNode, normalized, "comment_id");
        copyField(commentNode, normalized, "post_id");
        copyField(commentNode, normalized, "body");
        copyField(commentNode, normalized, "score");
        copyField(commentNode, normalized, "owner");
        copyField(commentNode, normalized, "creation_date");
        copyField(commentNode, normalized, "content_license");
        
        return normalized;
    }

    private void copyField(com.fasterxml.jackson.databind.JsonNode source, ObjectNode target, String fieldName) {
        if (source.has(fieldName)) {
            target.set(fieldName, source.get(fieldName));
        }
    }

    private void copyField(com.fasterxml.jackson.databind.JsonNode source, ObjectNode target, String fieldName,
                          java.util.function.Function<com.fasterxml.jackson.databind.JsonNode, com.fasterxml.jackson.databind.JsonNode> transformer) {
        if (source.has(fieldName)) {
            target.set(fieldName, transformer.apply(source.get(fieldName)));
        }
    }
}
























