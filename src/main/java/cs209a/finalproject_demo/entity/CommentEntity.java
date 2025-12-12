package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 评论实体
 */
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_post_id", columnList = "post_id"),
    @Index(name = "idx_comment_post_type", columnList = "post_type")
})
public class CommentEntity {
    @Id
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_type", length = 20, nullable = false)
    private String postType; // "question" or "answer"

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
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private AnswerEntity answer;

    // Constructors
    public CommentEntity() {
    }

    public CommentEntity(Long commentId, Long postId, String postType, String body, 
                        Integer score, Instant creationDate) {
        this.commentId = commentId;
        this.postId = postId;
        this.postType = postType;
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

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
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

    public AnswerEntity getAnswer() {
        return answer;
    }

    public void setAnswer(AnswerEntity answer) {
        this.answer = answer;
    }
}







