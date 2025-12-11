package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.ThreadFileLoader;
import cs209a.finalproject_demo.entity.*;
import cs209a.finalproject_demo.model.QuestionThread;
import cs209a.finalproject_demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据导入服务：将 JSON 文件数据导入到 PostgreSQL 数据库
 */
@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private final ThreadFileLoader fileLoader;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public DataImportService(ThreadFileLoader fileLoader,
                            UserRepository userRepository,
                            QuestionRepository questionRepository,
                            AnswerRepository answerRepository,
                            CommentRepository commentRepository,
                            TagRepository tagRepository) {
        this.fileLoader = fileLoader;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * 从指定目录导入所有 JSON 文件到数据库
     */
    public ImportResult importFromDirectory(String directoryPath) {
        log.info("Starting data import from directory: {}", directoryPath);
        
        ImportResult result = new ImportResult();
        Path folderPath = Paths.get(directoryPath);
        
        if (!Files.exists(folderPath)) {
            log.error("Directory not found: {}", directoryPath);
            result.addError("Directory not found: " + directoryPath);
            return result;
        }

        try {
            List<Path> jsonFiles = Files.list(folderPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();

            log.info("Found {} JSON files to import", jsonFiles.size());

            for (Path jsonFile : jsonFiles) {
                try {
                    Optional<QuestionThread> threadOpt = fileLoader.load(jsonFile);
                    if (threadOpt.isPresent()) {
                        importThread(threadOpt.get());
                        result.incrementSuccess();
                    } else {
                        result.incrementSkipped();
                        log.warn("Failed to load thread from: {}", jsonFile);
                    }
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError("Failed to import " + jsonFile.getFileName() + ": " + e.getMessage());
                    log.error("Error importing {}: {}", jsonFile, e.getMessage(), e);
                }
            }

            log.info("Import completed. Success: {}, Failed: {}, Skipped: {}", 
                    result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount());

        } catch (Exception e) {
            log.error("Error reading directory: {}", e.getMessage(), e);
            result.addError("Error reading directory: " + e.getMessage());
        }

        return result;
    }

    /**
     * 清空数据库中的业务数据（保留 schema）
     */
    @Transactional
    public void clearAllData() {
        // 删除顺序需遵守外键依赖：先删子表后删父表
        commentRepository.deleteAllInBatch();
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        log.info("All existing data cleared before import.");
    }

    /**
     * 导入单个线程到数据库
     */
    @Transactional
    public void importThread(QuestionThread thread) {
        var question = thread.question();
        var answers = thread.answers();
        var questionComments = thread.questionComments();
        var answerComments = thread.answerComments();

        // 1. 导入或获取用户
        UserEntity owner = importOrGetUser(question.owner());

        // 2. 导入或获取标签
        List<TagEntity> tags = importTags(question.tags());

        // 3. 创建问题实体
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setQuestionId(question.id());
        questionEntity.setTitle(question.title());
        // 样本 JSON 已包含正文，直接写入；若为空则保持空字符串
        questionEntity.setBody(question.body());
        questionEntity.setAnswered(question.answered());
        questionEntity.setAnswerCount(question.answerCount());
        questionEntity.setScore(question.score());
        questionEntity.setCreationDate(question.creationInstant());
        questionEntity.setLastActivityDate(Instant.ofEpochSecond(question.lastActivityDateEpoch()));
        questionEntity.setAcceptedAnswerId(question.acceptedAnswerId() != null ? 
                question.acceptedAnswerId().longValue() : null);
        questionEntity.setViewCount(question.viewCount());
        questionEntity.setOwner(owner);
        questionEntity.setTags(tags);

        // 保存问题
        questionRepository.save(questionEntity);

        // 4. 导入回答
        for (var answer : answers) {
            UserEntity answerOwner = importOrGetUser(answer.owner());
            AnswerEntity answerEntity = new AnswerEntity();
            answerEntity.setAnswerId(answer.id());
            answerEntity.setQuestion(questionEntity);
            // 样本回答通常无正文，保持为空。若模型扩展包含 body，再写入。
            answerEntity.setBody(null);
            answerEntity.setScore(answer.score());
            answerEntity.setAccepted(answer.accepted());
            answerEntity.setCreationDate(Instant.ofEpochSecond(answer.creationDateEpoch()));
            answerEntity.setOwner(answerOwner);
            
            answerRepository.save(answerEntity);
            questionEntity.getAnswers().add(answerEntity);

            // 导入回答的评论
            List<cs209a.finalproject_demo.model.Comment> comments = answerComments.getOrDefault(answer.id(), List.of());
            for (var comment : comments) {
                importComment(comment, questionEntity, answerEntity);
            }
        }

        // 5. 导入问题的评论
        for (var comment : questionComments) {
            importComment(comment, questionEntity, null);
        }

        // 保存更新后的问题（包含关联）
        questionRepository.save(questionEntity);
    }

    private UserEntity importOrGetUser(cs209a.finalproject_demo.model.Author author) {
        Optional<UserEntity> existing = userRepository.findByAccountId(author.accountId());
        if (existing.isPresent()) {
            return existing.get();
        }

        UserEntity user = new UserEntity();
        user.setAccountId(author.accountId());
        user.setUserId(author.userId());
        user.setDisplayName(author.displayName());
        user.setReputation(author.reputation());
        user.setUserType(author.userType());
        return userRepository.save(user);
    }

    private List<TagEntity> importTags(List<String> tagNames) {
        List<TagEntity> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            Optional<TagEntity> existing = tagRepository.findByName(tagName.toLowerCase());
            if (existing.isPresent()) {
                tags.add(existing.get());
            } else {
                TagEntity tag = new TagEntity(tagName.toLowerCase());
                tag = tagRepository.save(tag);
                tags.add(tag);
            }
        }
        return tags;
    }

    private void importComment(cs209a.finalproject_demo.model.Comment comment, 
                              QuestionEntity question, AnswerEntity answer) {
        Optional<CommentEntity> existing = commentRepository.findByCommentId(comment.id());
        if (existing.isPresent()) {
            return; // 已存在，跳过
        }

        UserEntity commentOwner = importOrGetUser(comment.owner());
        CommentEntity commentEntity = new CommentEntity();
        commentEntity.setCommentId(comment.id());
        commentEntity.setPostId(comment.postId());
        commentEntity.setPostType(comment.postType());
        commentEntity.setBody(comment.text());
        commentEntity.setScore(comment.score());
        commentEntity.setCreationDate(Instant.ofEpochSecond(comment.creationDateEpoch()));
        commentEntity.setOwner(commentOwner);
        commentEntity.setQuestion(question);
        commentEntity.setAnswer(answer);

        commentRepository.save(commentEntity);
    }

    /**
     * 导入结果统计
     */
    public static class ImportResult {
        private int successCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private final List<String> errors = new ArrayList<>();

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailed() {
            failedCount++;
        }

        public void incrementSkipped() {
            skippedCount++;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return String.format("ImportResult{success=%d, failed=%d, skipped=%d, errors=%d}",
                    successCount, failedCount, skippedCount, errors.size());
        }
    }
}

