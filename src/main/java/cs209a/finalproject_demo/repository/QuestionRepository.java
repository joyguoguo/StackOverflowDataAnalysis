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

    /**
     * 使用JOIN FETCH加载问题和答案，避免N+1查询问题
     * 注意：不能同时fetch多个List集合（MultipleBagFetchException）
     * 所以只fetch answers和owner，tags和comments会在后续批量加载
     */
    @Query("SELECT DISTINCT q FROM QuestionEntity q " +
           "LEFT JOIN FETCH q.answers " +
           "LEFT JOIN FETCH q.owner")
    List<QuestionEntity> findAllWithAssociations();
    
    /**
     * 批量查询问题的tags（通过question_tags关联表）
     * 返回questionId和对应的TagEntity列表的映射
     */
    @Query("SELECT q.questionId, t FROM QuestionEntity q " +
           "JOIN q.tags t " +
           "WHERE q.questionId IN :questionIds")
    List<Object[]> findQuestionTagsByQuestionIds(List<Long> questionIds);
    
    /**
     * 查找潜在的多线程相关问题（数据库初筛）
     * 通过标签进行初步筛选，同时加载答案和所有者信息，避免N+1查询
     */
    @Query("SELECT DISTINCT q FROM QuestionEntity q " +
           "LEFT JOIN FETCH q.answers " +
           "LEFT JOIN FETCH q.owner " +
           "JOIN q.tags t " +
           "WHERE LOWER(t.name) IN ('multithreading', 'concurrency', 'thread', 'thread-safety', 'synchronization', 'executor', 'locks', 'parallel', 'async') " +
           "OR LOWER(t.name) LIKE '%thread%' " +
           "OR LOWER(t.name) LIKE '%concurrent%'")
    List<QuestionEntity> findPotentialMultithreadingQuestions();
    
    /**
     * 查找可解决的问题（初步筛选）
     * 仅按是否有被接受答案且未关闭进行粗筛，
     * 具体时间间隔等规则在服务层 refineSolvableQuestions 中处理。
     * 同时加载答案和所有者信息，避免N+1查询
     */
    @Query("SELECT DISTINCT q FROM QuestionEntity q " +
           "LEFT JOIN FETCH q.answers a " +
           "LEFT JOIN FETCH q.owner " +
           "WHERE q.acceptedAnswerId IS NOT NULL " +
           "  AND q.closedDate IS NULL")
    List<QuestionEntity> findSolvableQuestions();
    
    /**
     * 查找难解决的问题（初步筛选）
     * 仅按“无被接受答案且未关闭”进行粗筛，
     * 具体高分答案与长期无人回答规则在服务层 refineHardQuestions 中处理。
     * 同时加载答案和所有者信息，避免N+1查询
     */
    @Query("SELECT DISTINCT q FROM QuestionEntity q " +
           "LEFT JOIN FETCH q.answers a " +
           "LEFT JOIN FETCH q.owner " +
           "WHERE q.acceptedAnswerId IS NULL " +
           "  AND q.closedDate IS NULL")
    List<QuestionEntity> findHardToSolveQuestions();
}










