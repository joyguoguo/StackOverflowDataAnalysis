package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 问题实体
 */
@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_question_creation_date", columnList = "creation_date"),
    @Index(name = "idx_question_score", columnList = "score"),
    @Index(name = "idx_question_answered", columnList = "answered")
})
public class QuestionEntity {
    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "answered")
    private Boolean answered;

    @Column(name = "answer_count")
    private Integer answerCount;

    @Column(name = "score")
    private Integer score;

    @Column(name = "creation_date")
    private Instant creationDate;

    @Column(name = "last_activity_date")
    private Instant lastActivityDate;

    @Column(name = "accepted_answer_id")
    private Long acceptedAnswerId;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "closed_date")
    private Instant closedDate;

    @Column(name = "closed_reason", columnDefinition = "TEXT")
    private String closedReason;

    @Column(name = "content_license", length = 100)
    private String contentLicense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", referencedColumnName = "account_id")
    private UserEntity owner;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerEntity> answers = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "question_tags",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_name", referencedColumnName = "name")
    )
    private List<TagEntity> tags = new ArrayList<>();

    // Constructors
    public QuestionEntity() {
    }

    public QuestionEntity(Long questionId, String title, String body, Boolean answered, 
                         Integer answerCount, Integer score, Instant creationDate, 
                         Instant lastActivityDate, Long acceptedAnswerId, Integer viewCount) {
        this.questionId = questionId;
        this.title = title;
        this.body = body;
        this.answered = answered;
        this.answerCount = answerCount;
        this.score = score;
        this.creationDate = creationDate;
        this.lastActivityDate = lastActivityDate;
        this.acceptedAnswerId = acceptedAnswerId;
        this.viewCount = viewCount;
    }

    // Getters and Setters
    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Integer getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(Integer answerCount) {
        this.answerCount = answerCount;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(Instant lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Long getAcceptedAnswerId() {
        return acceptedAnswerId;
    }

    public void setAcceptedAnswerId(Long acceptedAnswerId) {
        this.acceptedAnswerId = acceptedAnswerId;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Instant getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Instant closedDate) {
        this.closedDate = closedDate;
    }

    public String getClosedReason() {
        return closedReason;
    }

    public void setClosedReason(String closedReason) {
        this.closedReason = closedReason;
    }

    public String getContentLicense() {
        return contentLicense;
    }

    public void setContentLicense(String contentLicense) {
        this.contentLicense = contentLicense;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public List<AnswerEntity> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerEntity> answers) {
        this.answers = answers;
    }

    public List<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(List<CommentEntity> comments) {
        this.comments = comments;
    }

    public List<TagEntity> getTags() {
        return tags;
    }

    public void setTags(List<TagEntity> tags) {
        this.tags = tags;
    }
}

