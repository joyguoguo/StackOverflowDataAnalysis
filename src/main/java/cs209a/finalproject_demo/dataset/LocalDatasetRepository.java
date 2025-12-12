package cs209a.finalproject_demo.dataset;

import cs209a.finalproject_demo.entity.AnswerEntity;
import cs209a.finalproject_demo.entity.CommentEntity;
import cs209a.finalproject_demo.entity.QuestionEntity;
import cs209a.finalproject_demo.entity.TagEntity;
import cs209a.finalproject_demo.entity.UserEntity;
import cs209a.finalproject_demo.model.Answer;
import cs209a.finalproject_demo.model.Author;
import cs209a.finalproject_demo.model.Comment;
import cs209a.finalproject_demo.model.Question;
import cs209a.finalproject_demo.model.QuestionThread;
import cs209a.finalproject_demo.repository.QuestionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class LocalDatasetRepository {

    private final QuestionRepository questionRepository;

    public LocalDatasetRepository(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 注意：现在从数据库实时读取，而不是启动时缓存 JSON。
     */
    @Transactional(readOnly = true)
    public List<QuestionThread> findAllThreads() {
        List<QuestionEntity> questions = questionRepository.findAll();
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

        List<Comment> questionComments = questionEntity.getComments().stream()
                .map(this::mapComment)
                .toList();

        Map<Long, List<Comment>> answerComments = new LinkedHashMap<>();
        for (AnswerEntity answerEntity : questionEntity.getAnswers()) {
            List<Comment> comments = answerEntity.getComments().stream()
                    .map(this::mapComment)
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

    private Comment mapComment(CommentEntity entity) {
        Author owner = mapAuthor(entity.getOwner());
        return new Comment(
                entity.getCommentId(),
                entity.getPostId(),
                entity.getPostType(),
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

