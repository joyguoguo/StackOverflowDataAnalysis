package cs209a.finalproject_demo.repository;

import cs209a.finalproject_demo.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Optional<CommentEntity> findByCommentId(Long commentId);

    List<CommentEntity> findByPostIdAndPostType(Long postId, String postType);
}










