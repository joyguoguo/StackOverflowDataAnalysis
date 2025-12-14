package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 回答评论实体
 */
@Entity
@Table(name = "answer_comments", indexes = {
    @Index(name = "idx_answer_comment_answer_id", columnList = "answer_id"),
    @Index(name = "idx_answer_comment_creation_date", columnList = "creation_date")
})
public class AnswerCommentEntity {
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
    @JoinColumn(name = "answer_id", nullable = false)
    private AnswerEntity answer;

    // Constructors
    public AnswerCommentEntity() {
    }

    public AnswerCommentEntity(Long commentId, String body, Integer score, Instant creationDate) {
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

    public AnswerEntity getAnswer() {
        return answer;
    }

    public void setAnswer(AnswerEntity answer) {
        this.answer = answer;
    }
}

