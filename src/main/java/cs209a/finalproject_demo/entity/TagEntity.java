package cs209a.finalproject_demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签实体
 */
@Entity
@Table(name = "tags", indexes = {
    @Index(name = "idx_tag_name", columnList = "name", unique = true)
})
public class TagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private List<QuestionEntity> questions = new ArrayList<>();

    // Constructors
    public TagEntity() {
    }

    public TagEntity(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QuestionEntity> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionEntity> questions) {
        this.questions = questions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagEntity tagEntity = (TagEntity) o;
        return name != null && name.equals(tagEntity.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}




























