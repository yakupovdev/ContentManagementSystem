package com.github.yakupovdev.cms.repository;

import com.github.yakupovdev.cms.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    Post findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}