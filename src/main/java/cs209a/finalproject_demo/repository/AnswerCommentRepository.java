package cs209a.finalproject_demo.repository;

import cs209a.finalproject_demo.entity.AnswerCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerCommentRepository extends JpaRepository<AnswerCommentEntity, Long> {
    /**
     * 根据评论ID查找回答评论（用于存在性检查）
     */
    Optional<AnswerCommentEntity> findByCommentId(Long commentId);

    /**
     * 根据回答ID查找该回答的所有评论（用于查询统计）
     */
    List<AnswerCommentEntity> findByAnswerAnswerId(Long answerId);
    
    /**
     * 批量查找多个回答的评论，避免N+1查询
     * 使用JOIN FETCH加载owner信息
     */
    @Query("SELECT DISTINCT ac FROM AnswerCommentEntity ac " +
           "LEFT JOIN FETCH ac.owner " +
           "WHERE ac.answer.answerId IN :answerIds")
    List<AnswerCommentEntity> findByAnswerAnswerIdIn(List<Long> answerIds);
}

