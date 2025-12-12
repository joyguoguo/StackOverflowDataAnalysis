package cs209a.finalproject_demo.dataset;

import cs209a.finalproject_demo.model.Answer;
import cs209a.finalproject_demo.model.Author;
import cs209a.finalproject_demo.model.Comment;
import cs209a.finalproject_demo.model.Question;
import cs209a.finalproject_demo.model.QuestionThread;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class ThreadFileLoader {

    private static final Logger log = LoggerFactory.getLogger(ThreadFileLoader.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<QuestionThread> load(Path filePath) {
        try {
            String content = Files.readString(filePath);
            JsonNode root = objectMapper.readTree(content);
            Question question = mapQuestion(root.path("question"));
            if (question == null) {
                return Optional.empty();
            }
            List<Answer> answers = mapAnswers(root.path("answers"));
            List<Comment> questionComments = mapComments(root.path("question_comments"), question.id(), "question");
            Map<Long, List<Comment>> answerComments = mapAnswerComments(root.path("answer_comments"));
            return Optional.of(new QuestionThread(question, answers, questionComments, answerComments));
        } catch (IOException e) {
            log.warn("Failed to parse {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    private Question mapQuestion(JsonNode node) {
        if (node.isMissingNode()) {
            return null;
        }
        Author owner = mapAuthor(node.path("owner"));
        List<String> tags = new ArrayList<>();
        node.path("tags").forEach(tag -> tags.add(tag.asText("").toLowerCase(Locale.ROOT)));
        return new Question(
                node.path("question_id").asLong(),
                node.path("title").asText(""),
                node.path("body").asText(""),
                Collections.unmodifiableList(tags),
                owner,
                node.path("is_answered").asBoolean(false),
                node.path("answer_count").asInt(0),
                node.path("score").asInt(0),
                node.path("creation_date").asLong(0),
                node.path("last_activity_date").asLong(0),
                node.hasNonNull("accepted_answer_id") ? node.path("accepted_answer_id").asInt() : null,
                node.path("view_count").asInt(0),
                node.path("link").asText(null),
                node.hasNonNull("closed_date") ? node.path("closed_date").asLong() : null,
                node.path("closed_reason").asText(null),
                node.path("content_license").asText(null)
        );
    }

    private List<Answer> mapAnswers(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<Answer> answers = new ArrayList<>();
        node.forEach(answerNode -> {
            Author owner = mapAuthor(answerNode.path("owner"));
            answers.add(new Answer(
                    answerNode.path("answer_id").asLong(),
                    answerNode.path("question_id").asLong(),
                    answerNode.path("body").asText(""),
                    owner,
                    answerNode.path("score").asInt(0),
                    answerNode.path("is_accepted").asBoolean(false),
                    answerNode.path("creation_date").asLong(0),
                    answerNode.hasNonNull("last_activity_date") ? answerNode.path("last_activity_date").asLong() : null,
                    answerNode.path("content_license").asText(null)
            ));
        });
        return answers;
    }

    private List<Comment> mapComments(JsonNode node, long postId, String postType) {
        if (!node.isArray()) {
            return List.of();
        }
        List<Comment> comments = new ArrayList<>();
        node.forEach(commentNode -> {
            Author owner = mapAuthor(commentNode.path("owner"));
            comments.add(new Comment(
                    commentNode.path("comment_id").asLong(),
                    postId,
                    postType,
                    owner,
                    commentNode.path("score").asInt(0),
                    commentNode.path("creation_date").asLong(0),
                    commentNode.path("body").asText(""),
                    commentNode.path("content_license").asText(null)
            ));
        });
        return comments;
    }

    private Map<Long, List<Comment>> mapAnswerComments(JsonNode node) {
        if (!node.isObject()) {
            return Map.of();
        }
        Map<Long, List<Comment>> comments = new HashMap<>();
        node.fieldNames().forEachRemaining(answerIdStr -> {
            long answerId = Long.parseLong(answerIdStr);
            JsonNode commentArray = node.path(answerIdStr);
            comments.put(answerId, mapComments(commentArray, answerId, "answer"));
        });
        return comments;
    }

    private Author mapAuthor(JsonNode node) {
        if (node.isMissingNode()) {
            return new Author(0L, null, "anonymous", 0, "unknown", null, null);
        }
        return new Author(
                node.path("account_id").asLong(0),
                node.hasNonNull("user_id") ? node.path("user_id").asLong() : null,
                node.path("display_name").asText(null),
                node.path("reputation").asInt(0),
                node.path("user_type").asText("unknown"),
                node.path("profile_image").asText(null),
                node.path("link").asText(null)
        );
    }
}






