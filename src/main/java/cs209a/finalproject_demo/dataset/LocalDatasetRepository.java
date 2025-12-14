package cs209a.finalproject_demo.dataset;

import cs209a.finalproject_demo.entity.AnswerEntity;
import cs209a.finalproject_demo.entity.QuestionCommentEntity;
import cs209a.finalproject_demo.entity.AnswerCommentEntity;
import cs209a.finalproject_demo.entity.QuestionEntity;
import cs209a.finalproject_demo.entity.TagEntity;
import cs209a.finalproject_demo.entity.UserEntity;
import cs209a.finalproject_demo.model.Answer;
import cs209a.finalproject_demo.model.Author;
import cs209a.finalproject_demo.model.Comment;
import cs209a.finalproject_demo.model.Question;
import cs209a.finalproject_demo.model.QuestionThread;
import cs209a.finalproject_demo.repository.QuestionRepository;
import cs209a.finalproject_demo.repository.QuestionCommentRepository;
import cs209a.finalproject_demo.repository.AnswerCommentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LocalDatasetRepository {

    private final QuestionRepository questionRepository;
    private final QuestionCommentRepository questionCommentRepository;
    private final AnswerCommentRepository answerCommentRepository;

    public LocalDatasetRepository(QuestionRepository questionRepository,
                                  QuestionCommentRepository questionCommentRepository,
                                  AnswerCommentRepository answerCommentRepository) {
        this.questionRepository = questionRepository;
        this.questionCommentRepository = questionCommentRepository;
        this.answerCommentRepository = answerCommentRepository;
    }

    /**
     * 注意：现在从数据库实时读取，而不是启动时缓存 JSON。
     * 使用JOIN FETCH优化查询，避免N+1查询问题。
     * 使用批量查询加载comments，避免MultipleBagFetchException。
     */
    @Transactional(readOnly = true)
    public List<QuestionThread> findAllThreads() {
        // 1. 加载questions、answers、owner（使用JOIN FETCH，只fetch一个List集合）
        List<QuestionEntity> questions = questionRepository.findAllWithAssociations();
        
        if (questions.isEmpty()) {
            return List.of();
        }
        
        List<Long> questionIds = questions.stream()
                .map(QuestionEntity::getQuestionId)
                .toList();
        
        // 2. 批量加载tags（通过question_tags关联表）
        List<Object[]> questionTags = questionRepository.findQuestionTagsByQuestionIds(questionIds);
        Map<Long, List<TagEntity>> tagsMap = new java.util.HashMap<>();
        for (Object[] row : questionTags) {
            Long qId = (Long) row[0];
            TagEntity tag = (TagEntity) row[1];
            tagsMap.computeIfAbsent(qId, k -> new java.util.ArrayList<>()).add(tag);
        }
        
        // 3. 批量加载questionComments
        List<QuestionCommentEntity> allQuestionComments = questionCommentRepository.findByQuestionQuestionIdIn(questionIds);
        Map<Long, List<QuestionCommentEntity>> questionCommentsMap = allQuestionComments.stream()
                .collect(Collectors.groupingBy(c -> c.getQuestion().getQuestionId()));
        
        // 4. 批量加载answerComments
        List<Long> answerIds = questions.stream()
                .flatMap(q -> q.getAnswers().stream())
                .map(AnswerEntity::getAnswerId)
                .toList();
        List<AnswerCommentEntity> allAnswerComments = answerIds.isEmpty() ? List.of() :
                answerCommentRepository.findByAnswerAnswerIdIn(answerIds);
        Map<Long, List<AnswerCommentEntity>> answerCommentsMap = allAnswerComments.stream()
                .collect(Collectors.groupingBy(c -> c.getAnswer().getAnswerId()));
        
        // 5. 手动关联tags和comments到entities
        for (QuestionEntity question : questions) {
            // 关联tags
            question.getTags().clear();
            question.getTags().addAll(tagsMap.getOrDefault(question.getQuestionId(), List.of()));
            
            // 关联questionComments
            question.getQuestionComments().clear();
            question.getQuestionComments().addAll(
                    questionCommentsMap.getOrDefault(question.getQuestionId(), List.of())
            );
            
            // 关联answerComments
            for (AnswerEntity answer : question.getAnswers()) {
                answer.getAnswerComments().clear();
                answer.getAnswerComments().addAll(
                        answerCommentsMap.getOrDefault(answer.getAnswerId(), List.of())
                );
            }
        }
        
        // 6. 映射为QuestionThread
        return questions.stream()
                .map(this::mapQuestionThread)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Question> findAllQuestions() {
        return findAllThreads().stream().map(QuestionThread::question).toList();
    }

    @Transactional(readOnly = true)
    public Optional<QuestionThread> findByQuestionId(long questionId) {
        return questionRepository.findById(questionId).map(this::mapQuestionThread);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> minCreationInstant() {
        return findAllQuestions().stream()
                .map(Question::creationInstant)
                .min(Instant::compareTo);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> maxCreationInstant() {
        return findAllQuestions().stream()
                .map(Question::creationInstant)
                .max(Instant::compareTo);
    }

    @Transactional(readOnly = true)
    public int totalAnswerCount() {
        return findAllThreads().stream().mapToInt(t -> t.answers().size()).sum();
    }

    @Transactional(readOnly = true)
    public int totalCommentCount() {
        return (int) findAllThreads().stream()
                .mapToLong(t -> t.questionComments().size() +
                        t.answerComments().values().stream().mapToInt(List::size).sum())
                .sum();
    }

    private QuestionThread mapQuestionThread(QuestionEntity questionEntity) {
        Question question = mapQuestion(questionEntity);
        List<Answer> answers = questionEntity.getAnswers().stream()
                .map(this::mapAnswer)
                .toList();

        List<Comment> questionComments = questionEntity.getQuestionComments().stream()
                .map(c -> mapQuestionComment(c, questionEntity.getQuestionId()))
                .toList();

        Map<Long, List<Comment>> answerComments = new LinkedHashMap<>();
        for (AnswerEntity answerEntity : questionEntity.getAnswers()) {
            List<Comment> comments = answerEntity.getAnswerComments().stream()
                    .map(c -> mapAnswerComment(c, answerEntity.getAnswerId()))
                    .toList();
            answerComments.put(answerEntity.getAnswerId(), comments);
        }

        return new QuestionThread(question, answers, questionComments, answerComments);
    }

    private Question mapQuestion(QuestionEntity entity) {
        List<String> tags = entity.getTags().stream()
                .map(TagEntity::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
        Author owner = mapAuthor(entity.getOwner());
        return new Question(
                entity.getQuestionId(),
                entity.getTitle(),
                entity.getBody(),
                tags,
                owner,
                Boolean.TRUE.equals(entity.getAnswered()),
                entity.getAnswerCount() == null ? 0 : entity.getAnswerCount(),
                entity.getScore() == null ? 0 : entity.getScore(),
                entity.getCreationDate() == null ? 0 : entity.getCreationDate().getEpochSecond(),
                entity.getLastActivityDate() == null ? 0 : entity.getLastActivityDate().getEpochSecond(),
                entity.getAcceptedAnswerId() == null ? null : entity.getAcceptedAnswerId().intValue(),
                entity.getViewCount() == null ? 0 : entity.getViewCount(),
                entity.getLink(),
                entity.getClosedDate() == null ? null : entity.getClosedDate().getEpochSecond(),
                entity.getClosedReason(),
                entity.getContentLicense()
        );
    }

    private Answer mapAnswer(AnswerEntity entity) {
        Author owner = mapAuthor(entity.getOwner());
        return new Answer(
                entity.getAnswerId(),
                entity.getQuestion().getQuestionId(),
                entity.getBody(),
                owner,
                entity.getScore() == null ? 0 : entity.getScore(),
                Boolean.TRUE.equals(entity.getAccepted()),
                entity.getCreationDate() == null ? 0 : entity.getCreationDate().getEpochSecond(),
                entity.getLastActivityDate() == null ? null : entity.getLastActivityDate().getEpochSecond(),
                entity.getContentLicense()
        );
    }

    private Comment mapQuestionComment(QuestionCommentEntity entity, Long questionId) {
        Author owner = mapAuthor(entity.getOwner());
        return new Comment(
                entity.getCommentId(),
                questionId,
                "question",
                owner,
                entity.getScore() == null ? 0 : entity.getScore(),
                entity.getCreationDate() == null ? 0 : entity.getCreationDate().getEpochSecond(),
                entity.getBody(),
                entity.getContentLicense()
        );
    }

    private Comment mapAnswerComment(AnswerCommentEntity entity, Long answerId) {
        Author owner = mapAuthor(entity.getOwner());
        return new Comment(
                entity.getCommentId(),
                answerId,
                "answer",
                owner,
                entity.getScore() == null ? 0 : entity.getScore(),
                entity.getCreationDate() == null ? 0 : entity.getCreationDate().getEpochSecond(),
                entity.getBody(),
                entity.getContentLicense()
        );
    }

    private Author mapAuthor(UserEntity user) {
        if (user == null) {
            return new Author(0L, null, "anonymous", 0, "unknown", null, null);
        }
        return new Author(
                user.getAccountId() == null ? 0L : user.getAccountId(),
                user.getUserId(),
                user.getDisplayName(),
                user.getReputation() == null ? 0 : user.getReputation(),
                user.getUserType(),
                user.getProfileImage(),
                user.getLink()
        );
    }
}

