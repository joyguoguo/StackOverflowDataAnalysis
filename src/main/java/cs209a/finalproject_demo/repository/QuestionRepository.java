package cs209a.finalproject_demo.repository;

import cs209a.finalproject_demo.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    Optional<QuestionEntity> findByQuestionId(Long questionId);

    @Query("SELECT q FROM QuestionEntity q WHERE q.creationDate BETWEEN :fromDate AND :toDate")
    List<QuestionEntity> findByCreationDateBetween(Instant fromDate, Instant toDate);

    @Query("SELECT q FROM QuestionEntity q JOIN q.tags t WHERE t.name = :tagName")
    List<QuestionEntity> findByTagName(String tagName);

    @Query("SELECT COUNT(q) FROM QuestionEntity q")
    long countAll();
}










