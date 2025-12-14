package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 回答实体
 */
@Entity
@Table(name = "answers", indexes = {
    @Index(name = "idx_answer_question_id", columnList = "question_id"),
    @Index(name = "idx_answer_accepted", columnList = "accepted"),
    @Index(name = "idx_answer_score", columnList = "score")
})
public class AnswerEntity {
    @Id
    @Column(name = "answer_id")
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "score")
    private Integer score;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "creation_date")
    private Instant creationDate;

    @Column(name = "last_activity_date")
    private Instant lastActivityDate;

    @Column(name = "content_license", length = 100)
    private String contentLicense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", referencedColumnName = "account_id")
    private UserEntity owner;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerCommentEntity> answerComments = new ArrayList<>();

    // Constructors
    public AnswerEntity() {
    }

    public AnswerEntity(Long answerId, String body, Integer score, Boolean accepted, Instant creationDate) {
        this.answerId = answerId;
        this.body = body;
        this.score = score;
        this.accepted = accepted;
        this.creationDate = creationDate;
    }

    // Getters and Setters
    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public void setQuestion(QuestionEntity question) {
        this.question = question;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
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

    public List<AnswerCommentEntity> getAnswerComments() {
        return answerComments;
    }

    public void setAnswerComments(List<AnswerCommentEntity> answerComments) {
        this.answerComments = answerComments;
    }
}









