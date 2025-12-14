package cs209a.finalproject_demo.repository;

import cs209a.finalproject_demo.entity.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {
    Optional<AnswerEntity> findByAnswerId(Long answerId);

    List<AnswerEntity> findByQuestionQuestionId(Long questionId);
}


















