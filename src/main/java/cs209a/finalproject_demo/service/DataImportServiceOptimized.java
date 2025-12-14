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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 优化版数据导入服务：使用批量操作提高导入速度
 * 
 * 性能优化方案：
 * 1. 批量保存：使用 saveAll 代替单个 save
 * 2. 批量事务：每批处理多个 thread，减少事务开销
 * 3. 预加载缓存：预先加载所有用户和标签到内存，避免重复查询
 * 4. 禁用级联：在批量插入时禁用不必要的级联操作
 * 5. 批量大小控制：可配置的批次大小
 */
@Service
public class DataImportServiceOptimized {

    private static final Logger log = LoggerFactory.getLogger(DataImportServiceOptimized.class);
    private static final int BATCH_SIZE = 50; // 每批处理的 thread 数量

    private final ThreadFileLoader fileLoader;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionCommentRepository questionCommentRepository;
    private final AnswerCommentRepository answerCommentRepository;
    private final TagRepository tagRepository;

    public DataImportServiceOptimized(ThreadFileLoader fileLoader,
                                    UserRepository userRepository,
                                    QuestionRepository questionRepository,
                                    AnswerRepository answerRepository,
                                    QuestionCommentRepository questionCommentRepository,
                                    AnswerCommentRepository answerCommentRepository,
                                    TagRepository tagRepository) {
        this.fileLoader = fileLoader;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionCommentRepository = questionCommentRepository;
        this.answerCommentRepository = answerCommentRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * 从指定目录导入所有 JSON 文件到数据库（优化版）
     */
    public ImportResult importFromDirectory(String directoryPath) {
        long startTime = System.currentTimeMillis();
        log.info("========================================");
        log.info("Starting OPTIMIZED data import from directory: {}", directoryPath);
        log.info("Batch size: {}", BATCH_SIZE);
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

            // 预加载所有用户和标签到内存缓存
            long cacheStartTime = System.currentTimeMillis();
            Map<Long, UserEntity> userCache = loadUserCache();
            Map<String, TagEntity> tagCache = loadTagCache();
            long cacheLoadTime = System.currentTimeMillis() - cacheStartTime;
            log.info("Preloaded {} users and {} tags into cache (took {} ms)", 
                    userCache.size(), tagCache.size(), cacheLoadTime);

            // 批量处理 threads
            List<QuestionThread> batch = new ArrayList<>();
            int processedCount = 0;
            int batchNumber = 0;
            
            for (Path jsonFile : jsonFiles) {
                try {
                    Optional<QuestionThread> threadOpt = fileLoader.load(jsonFile);
                    if (threadOpt.isPresent()) {
                        batch.add(threadOpt.get());
                        processedCount++;
                        
                        // 当批次达到指定大小时，批量导入
                        if (batch.size() >= BATCH_SIZE) {
                            batchNumber++;
                            log.info("Processing batch {} (files {}-{}/{})", 
                                    batchNumber, processedCount - BATCH_SIZE + 1, processedCount, jsonFiles.size());
                            long batchStartTime = System.currentTimeMillis();
                            importBatch(batch, userCache, tagCache, result);
                            long batchTime = System.currentTimeMillis() - batchStartTime;
                            log.info("Batch {} completed in {} ms (Success: {}, Failed: {})", 
                                    batchNumber, batchTime, result.getSuccessCount(), result.getFailedCount());
                            batch.clear();
                        }
                    } else {
                        result.incrementSkipped();
                        log.warn("Failed to load thread from: {} (file {}/{})", 
                                jsonFile.getFileName(), processedCount + 1, jsonFiles.size());
                    }
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError("Failed to load " + jsonFile.getFileName() + ": " + e.getMessage());
                    log.error("Error loading {} (file {}/{}): {}", 
                            jsonFile.getFileName(), processedCount + 1, jsonFiles.size(), e.getMessage(), e);
                }
            }

            // 处理剩余的 threads
            if (!batch.isEmpty()) {
                batchNumber++;
                log.info("Processing final batch {} ({} threads, files {}-{}/{})", 
                        batchNumber, batch.size(), processedCount - batch.size() + 1, processedCount, jsonFiles.size());
                long batchStartTime = System.currentTimeMillis();
                importBatch(batch, userCache, tagCache, result);
                long batchTime = System.currentTimeMillis() - batchStartTime;
                log.info("Final batch {} completed in {} ms", batchNumber, batchTime);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("OPTIMIZED import completed in {} seconds", duration / 1000.0);
            log.info("Summary: Success: {}, Failed: {}, Skipped: {}", 
                    result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount());
            log.info("Average time per thread: {} ms", 
                    result.getSuccessCount() > 0 ? duration / result.getSuccessCount() : 0);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Error reading directory: {}", e.getMessage(), e);
            result.addError("Error reading directory: " + e.getMessage());
        }

        return result;
    }

    /**
     * 预加载所有用户到内存缓存
     */
    private Map<Long, UserEntity> loadUserCache() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(UserEntity::getAccountId, u -> u));
    }

    /**
     * 预加载所有标签到内存缓存
     */
    private Map<String, TagEntity> loadTagCache() {
        return tagRepository.findAll().stream()
                .collect(Collectors.toMap(TagEntity::getName, t -> t));
    }

    /**
     * 批量导入 threads
     */
    @Transactional
    public void importBatch(List<QuestionThread> threads, 
                           Map<Long, UserEntity> userCache,
                           Map<String, TagEntity> tagCache,
                           ImportResult result) {
        try {
            List<UserEntity> newUsers = new ArrayList<>();
            List<TagEntity> newTags = new ArrayList<>();
            List<QuestionEntity> questions = new ArrayList<>();
            List<AnswerEntity> answers = new ArrayList<>();
            List<QuestionCommentEntity> questionComments = new ArrayList<>();
            List<AnswerCommentEntity> answerComments = new ArrayList<>();

            for (QuestionThread thread : threads) {
                try {
                    processThread(thread, userCache, tagCache, newUsers, newTags, 
                                 questions, answers, questionComments, answerComments);
                    result.incrementSuccess();
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError("Failed to process thread " + thread.question().id() + ": " + e.getMessage());
                    log.error("Error processing thread {}: {}", thread.question().id(), e.getMessage(), e);
                }
            }

            // 批量保存
            log.debug("Batch statistics - Questions: {}, Answers: {}, Question Comments: {}, Answer Comments: {}, New Users: {}, New Tags: {}", 
                     questions.size(), answers.size(), questionComments.size(), answerComments.size(), newUsers.size(), newTags.size());
            
            if (!newUsers.isEmpty()) {
                userRepository.saveAll(newUsers);
                // 更新缓存
                newUsers.forEach(u -> userCache.put(u.getAccountId(), u));
                log.debug("Saved {} new users", newUsers.size());
            }
            if (!newTags.isEmpty()) {
                tagRepository.saveAll(newTags);
                // 更新缓存
                newTags.forEach(t -> tagCache.put(t.getName(), t));
                log.debug("Saved {} new tags", newTags.size());
            }
            if (!questions.isEmpty()) {
                questionRepository.saveAll(questions);
                log.debug("Saved {} questions", questions.size());
            }
            if (!answers.isEmpty()) {
                answerRepository.saveAll(answers);
                log.debug("Saved {} answers", answers.size());
            }
            if (!questionComments.isEmpty()) {
                questionCommentRepository.saveAll(questionComments);
                log.debug("Saved {} question comments", questionComments.size());
            }
            if (!answerComments.isEmpty()) {
                answerCommentRepository.saveAll(answerComments);
                log.debug("Saved {} answer comments", answerComments.size());
            }

            log.debug("Completed batch import of {} threads", threads.size());
        } catch (Exception e) {
            log.error("Error importing batch: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理单个 thread（不立即保存，收集到批量列表）
     */
    private void processThread(QuestionThread thread,
                               Map<Long, UserEntity> userCache,
                               Map<String, TagEntity> tagCache,
                               List<UserEntity> newUsers,
                               List<TagEntity> newTags,
                               List<QuestionEntity> questions,
                               List<AnswerEntity> answers,
                               List<QuestionCommentEntity> questionComments,
                               List<AnswerCommentEntity> answerComments) {
        var question = thread.question();
        var questionAnswers = thread.answers();
        var questionCommentList = thread.questionComments();
        var answerCommentMap = thread.answerComments();

        // 1. 获取或创建用户
        UserEntity owner = getOrCreateUser(question.owner(), userCache, newUsers);

        // 2. 获取或创建标签
        List<TagEntity> tags = getOrCreateTags(question.tags(), tagCache, newTags);

        // 3. 创建问题实体
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setQuestionId(question.id());
        questionEntity.setTitle(question.title());
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
        questionEntity.setClosedDate(question.closedDateEpoch() == null ? null : 
                Instant.ofEpochSecond(question.closedDateEpoch()));
        questionEntity.setClosedReason(question.closedReason());
        questionEntity.setContentLicense(question.contentLicense());
        questionEntity.setOwner(owner);
        questionEntity.setTags(tags);
        questions.add(questionEntity);

        // 4. 创建回答实体
        Map<Long, AnswerEntity> answerMap = new HashMap<>();
        for (var answer : questionAnswers) {
            UserEntity answerOwner = getOrCreateUser(answer.owner(), userCache, newUsers);
            AnswerEntity answerEntity = new AnswerEntity();
            answerEntity.setAnswerId(answer.id());
            answerEntity.setQuestion(questionEntity);
            answerEntity.setBody(answer.body());
            answerEntity.setScore(answer.score());
            answerEntity.setAccepted(answer.accepted());
            answerEntity.setCreationDate(Instant.ofEpochSecond(answer.creationDateEpoch()));
            answerEntity.setLastActivityDate(answer.lastActivityDateEpoch() != null ? 
                    Instant.ofEpochSecond(answer.lastActivityDateEpoch()) : null);
            answerEntity.setContentLicense(answer.contentLicense());
            answerEntity.setOwner(answerOwner);
            answers.add(answerEntity);
            answerMap.put(answer.id(), answerEntity);
            questionEntity.getAnswers().add(answerEntity);
        }

        // 5. 创建问题的评论
        for (var comment : questionCommentList) {
            QuestionCommentEntity commentEntity = createQuestionCommentEntity(comment, questionEntity, userCache, newUsers);
            questionComments.add(commentEntity);
            questionEntity.getQuestionComments().add(commentEntity);
        }

        // 6. 创建回答的评论
        for (var entry : answerCommentMap.entrySet()) {
            Long answerId = entry.getKey();
            AnswerEntity answerEntity = answerMap.get(answerId);
            if (answerEntity != null) {
                for (var comment : entry.getValue()) {
                    AnswerCommentEntity commentEntity = createAnswerCommentEntity(comment, answerEntity, 
                                                                                 userCache, newUsers);
                    answerComments.add(commentEntity);
                    answerEntity.getAnswerComments().add(commentEntity);
                }
            }
        }
    }

    private UserEntity getOrCreateUser(cs209a.finalproject_demo.model.Author author,
                                      Map<Long, UserEntity> userCache,
                                      List<UserEntity> newUsers) {
        UserEntity user = userCache.get(author.accountId());
        if (user == null) {
            user = new UserEntity();
            user.setAccountId(author.accountId());
            user.setUserId(author.userId());
            user.setDisplayName(author.displayName());
            user.setReputation(author.reputation());
            user.setUserType(author.userType());
            user.setProfileImage(author.profileImage());
            user.setLink(author.link());
            newUsers.add(user);
            userCache.put(author.accountId(), user);
        }
        return user;
    }

    private List<TagEntity> getOrCreateTags(List<String> tagNames,
                                           Map<String, TagEntity> tagCache,
                                           List<TagEntity> newTags) {
        List<TagEntity> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            String normalizedName = tagName.toLowerCase();
            TagEntity tag = tagCache.get(normalizedName);
            if (tag == null) {
                tag = new TagEntity(normalizedName);
                newTags.add(tag);
                tagCache.put(normalizedName, tag);
            }
            tags.add(tag);
        }
        return tags;
    }

    private QuestionCommentEntity createQuestionCommentEntity(cs209a.finalproject_demo.model.Comment comment,
                                                             QuestionEntity question,
                                                             Map<Long, UserEntity> userCache,
                                                             List<UserEntity> newUsers) {
        UserEntity commentOwner = getOrCreateUser(comment.owner(), userCache, newUsers);
        QuestionCommentEntity commentEntity = new QuestionCommentEntity();
        commentEntity.setCommentId(comment.id());
        commentEntity.setBody(comment.text());
        commentEntity.setScore(comment.score());
        commentEntity.setCreationDate(Instant.ofEpochSecond(comment.creationDateEpoch()));
        commentEntity.setContentLicense(comment.contentLicense());
        commentEntity.setOwner(commentOwner);
        commentEntity.setQuestion(question);
        return commentEntity;
    }

    private AnswerCommentEntity createAnswerCommentEntity(cs209a.finalproject_demo.model.Comment comment,
                                                         AnswerEntity answer,
                                                         Map<Long, UserEntity> userCache,
                                                         List<UserEntity> newUsers) {
        UserEntity commentOwner = getOrCreateUser(comment.owner(), userCache, newUsers);
        AnswerCommentEntity commentEntity = new AnswerCommentEntity();
        commentEntity.setCommentId(comment.id());
        commentEntity.setBody(comment.text());
        commentEntity.setScore(comment.score());
        commentEntity.setCreationDate(Instant.ofEpochSecond(comment.creationDateEpoch()));
        commentEntity.setContentLicense(comment.contentLicense());
        commentEntity.setOwner(commentOwner);
        commentEntity.setAnswer(answer);
        return commentEntity;
    }

    /**
     * 清空数据库中的业务数据（保留 schema）
     */
    @Transactional
    public void clearAllData() {
        log.info("========================================");
        log.info("Clearing all existing data (Optimized)...");
        log.info("========================================");
        
        // 删除顺序需遵守外键依赖：先删子表后删父表
        long answerCommentCount = answerCommentRepository.count();
        long questionCommentCount = questionCommentRepository.count();
        long answerCount = answerRepository.count();
        long questionCount = questionRepository.count();
        long tagCount = tagRepository.count();
        long userCount = userRepository.count();
        
        log.info("Current data counts - Answer Comments: {}, Question Comments: {}, Answers: {}, Questions: {}, Tags: {}, Users: {}", 
                answerCommentCount, questionCommentCount, answerCount, questionCount, tagCount, userCount);
        
        answerCommentRepository.deleteAllInBatch();
        log.info("Deleted {} answer comments", answerCommentCount);
        
        questionCommentRepository.deleteAllInBatch();
        log.info("Deleted {} question comments", questionCommentCount);
        
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

