package cs209a.finalproject_demo.repository;

import cs209a.finalproject_demo.entity.QuestionCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionCommentRepository extends JpaRepository<QuestionCommentEntity, Long> {
    /**
     * 根据评论ID查找问题评论（用于存在性检查）
     */
    Optional<QuestionCommentEntity> findByCommentId(Long commentId);

    /**
     * 根据问题ID查找该问题的所有评论（用于查询统计）
     */
    List<QuestionCommentEntity> findByQuestionQuestionId(Long questionId);
    
    /**
     * 批量查找多个问题的评论，避免N+1查询
     * 使用JOIN FETCH加载owner信息
     */
    @Query("SELECT DISTINCT qc FROM QuestionCommentEntity qc " +
           "LEFT JOIN FETCH qc.owner " +
           "WHERE qc.question.questionId IN :questionIds")
    List<QuestionCommentEntity> findByQuestionQuestionIdIn(List<Long> questionIds);
}

