package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体（对应 Stack Overflow 的 Author）
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_account_id", columnList = "account_id"),
    @Index(name = "idx_user_user_id", columnList = "user_id")
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", unique = true, nullable = false)
    private Long accountId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "reputation")
    private Integer reputation;

    @Column(name = "user_type", length = 50)
    private String userType;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "link", length = 500)
    private String link;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionEntity> questions = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerEntity> answers = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();

    // Constructors
    public UserEntity() {
    }

    public UserEntity(Long accountId, Long userId, String displayName, Integer reputation, String userType) {
        this.accountId = accountId;
        this.userId = userId;
        this.displayName = displayName;
        this.reputation = reputation;
        this.userType = userType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getReputation() {
        return reputation;
    }

    public void setReputation(Integer reputation) {
        this.reputation = reputation;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<QuestionEntity> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionEntity> questions) {
        this.questions = questions;
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
}







