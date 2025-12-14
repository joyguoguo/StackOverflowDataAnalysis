package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 问题评论实体
 */
@Entity
@Table(name = "question_comments", indexes = {
    @Index(name = "idx_question_comment_question_id", columnList = "question_id"),
    @Index(name = "idx_question_comment_creation_date", columnList = "creation_date")
})
public class QuestionCommentEntity {
    @Id
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "score")
    private Integer score;

    @Column(name = "creation_date")
    private Instant creationDate;

    @Column(name = "content_license", length = 100)
    private String contentLicense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", referencedColumnName = "account_id")
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    // Constructors
    public QuestionCommentEntity() {
    }

    public QuestionCommentEntity(Long commentId, String body, Integer score, Instant creationDate) {
        this.commentId = commentId;
        this.body = body;
        this.score = score;
        this.creationDate = creationDate;
    }

    // Getters and Setters
    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
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

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
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

    public QuestionEntity getQuestion() {
        return question;
    }

    public void setQuestion(QuestionEntity question) {
        this.question = question;
    }
}

