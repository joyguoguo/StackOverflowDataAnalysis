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
        long startTime = System.currentTimeMillis();
        log.info("========================================");
        log.info("Starting data import from directory: {}", directoryPath);
        log.info("========================================");
        
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
            log.info("Starting import process...");

            int processedCount = 0;
            for (Path jsonFile : jsonFiles) {
                processedCount++;
                try {
                    Optional<QuestionThread> threadOpt = fileLoader.load(jsonFile);
                    if (threadOpt.isPresent()) {
                        QuestionThread thread = threadOpt.get();
                        log.debug("Processing file {}/{}: {}", processedCount, jsonFiles.size(), jsonFile.getFileName());
                        importThread(thread);
                        result.incrementSuccess();
                        
                        // 每10个文件输出一次进度
                        if (processedCount % 10 == 0) {
                            log.info("Progress: {}/{} files processed (Success: {}, Failed: {}, Skipped: {})", 
                                    processedCount, jsonFiles.size(), 
                                    result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount());
                        }
                    } else {
                        result.incrementSkipped();
                        log.warn("Failed to load thread from: {} (file {}/{})", jsonFile.getFileName(), processedCount, jsonFiles.size());
                    }
                } catch (Exception e) {
                    result.incrementFailed();
                    String errorMsg = "Failed to import " + jsonFile.getFileName() + ": " + e.getMessage();
                    result.addError(errorMsg);
                    log.error("Error importing {} (file {}/{}): {}", jsonFile.getFileName(), processedCount, jsonFiles.size(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("Import completed in {} seconds", duration / 1000.0);
            log.info("Summary: Success: {}, Failed: {}, Skipped: {}", 
                    result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount());
            log.info("========================================");

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
        log.info("========================================");
        log.info("Clearing all existing data...");
        log.info("========================================");
        
        // 删除顺序需遵守外键依赖：先删子表后删父表
        long commentCount = commentRepository.count();
        long answerCount = answerRepository.count();
        long questionCount = questionRepository.count();
        long tagCount = tagRepository.count();
        long userCount = userRepository.count();
        
        log.info("Current data counts - Comments: {}, Answers: {}, Questions: {}, Tags: {}, Users: {}", 
                commentCount, answerCount, questionCount, tagCount, userCount);
        
        commentRepository.deleteAllInBatch();
        log.info("Deleted {} comments", commentCount);
        
        answerRepository.deleteAllInBatch();
        log.info("Deleted {} answers", answerCount);
        
        questionRepository.deleteAllInBatch();
        log.info("Deleted {} questions", questionCount);
        
        tagRepository.deleteAllInBatch();
        log.info("Deleted {} tags", tagCount);
        
        userRepository.deleteAllInBatch();
        log.info("Deleted {} users", userCount);
        
        log.info("All existing data cleared successfully.");
        log.info("========================================");
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

        long questionId = question.id();
        log.debug("Importing thread: question_id={}, title={}", questionId, question.title());

        // 1. 导入或获取用户
        UserEntity owner = importOrGetUser(question.owner());
        log.trace("Imported/retrieved user: account_id={}", owner.getAccountId());

        // 2. 导入或获取标签
        List<TagEntity> tags = importTags(question.tags());
        log.trace("Imported {} tags for question {}", tags.size(), questionId);

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
        questionEntity.setLink(question.link());
        questionEntity.setClosedDate(question.closedDateEpoch() == null ? null : Instant.ofEpochSecond(question.closedDateEpoch()));
        questionEntity.setClosedReason(question.closedReason());
        questionEntity.setContentLicense(question.contentLicense());
        questionEntity.setOwner(owner);
        questionEntity.setTags(tags);

        // 保存问题
        questionRepository.save(questionEntity);
        log.debug("Saved question: question_id={}", questionId);

        // 4. 导入回答
        int answerCount = 0;
        int answerCommentCount = 0;
        for (var answer : answers) {
            answerCount++;
            UserEntity answerOwner = importOrGetUser(answer.owner());
            AnswerEntity answerEntity = new AnswerEntity();
            answerEntity.setAnswerId(answer.id());
            answerEntity.setQuestion(questionEntity);
            // 写入回答正文（若数据为空则保存为空字符串）
            answerEntity.setBody(answer.body());
            answerEntity.setScore(answer.score());
            answerEntity.setAccepted(answer.accepted());
            answerEntity.setCreationDate(Instant.ofEpochSecond(answer.creationDateEpoch()));
            answerEntity.setLastActivityDate(answer.lastActivityDateEpoch() != null ? 
                    Instant.ofEpochSecond(answer.lastActivityDateEpoch()) : null);
            answerEntity.setContentLicense(answer.contentLicense());
            answerEntity.setOwner(answerOwner);
            
            answerRepository.save(answerEntity);
            questionEntity.getAnswers().add(answerEntity);
            log.trace("Saved answer: answer_id={} for question_id={}", answer.id(), questionId);

            // 导入回答的评论
            List<cs209a.finalproject_demo.model.Comment> answerCommentList = answerComments.getOrDefault(answer.id(), List.of());
            if (!answerCommentList.isEmpty()) {
                log.debug("Importing {} comments for answer {} (question_id={})", answerCommentList.size(), answer.id(), questionId);
            }
            for (var comment : answerCommentList) {
                importComment(comment, questionEntity, answerEntity);
                answerCommentCount++;
            }
        }

        // 5. 导入问题的评论
        int questionCommentCount = questionComments.size();
        if (questionCommentCount > 0) {
            log.debug("Importing {} comments for question {}", questionCommentCount, questionId);
        }
        for (var comment : questionComments) {
            importComment(comment, questionEntity, null);
        }

        // 保存更新后的问题（包含关联）
        questionRepository.save(questionEntity);
        
        log.debug("Completed importing thread: question_id={}, answers={}, question_comments={}, answer_comments={}", 
                questionId, answerCount, questionCommentCount, answerCommentCount);
    }

    private UserEntity importOrGetUser(cs209a.finalproject_demo.model.Author author) {
        Optional<UserEntity> existing = userRepository.findByAccountId(author.accountId());
        if (existing.isPresent()) {
            UserEntity user = existing.get();
            // 更新可为空的补充信息
            user.setDisplayName(author.displayName());
            user.setReputation(author.reputation());
            user.setUserId(author.userId());
            user.setUserType(author.userType());
            user.setProfileImage(author.profileImage());
            user.setLink(author.link());
            return userRepository.save(user);
        }

        UserEntity user = new UserEntity();
        user.setAccountId(author.accountId());
        user.setUserId(author.userId());
        user.setDisplayName(author.displayName());
        user.setReputation(author.reputation());
        user.setUserType(author.userType());
        user.setProfileImage(author.profileImage());
        user.setLink(author.link());
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
        // 检查是否已存在（使用comment_id作为主键）
        Optional<CommentEntity> existing = commentRepository.findByCommentId(comment.id());
        if (existing.isPresent()) {
            log.trace("Comment {} already exists, skipping", comment.id());
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
        commentEntity.setContentLicense(comment.contentLicense());
        commentEntity.setOwner(commentOwner);
        commentEntity.setQuestion(question);
        commentEntity.setAnswer(answer);

        try {
            commentRepository.save(commentEntity);
            log.trace("Successfully imported comment {} for post {} (type: {}, question_id: {})", 
                     comment.id(), comment.postId(), comment.postType(), 
                     question != null ? question.getQuestionId() : "N/A");
        } catch (Exception e) {
            log.error("Failed to save comment {} for post {} (type: {}): {}", 
                     comment.id(), comment.postId(), comment.postType(), e.getMessage(), e);
            throw e;
        }
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

