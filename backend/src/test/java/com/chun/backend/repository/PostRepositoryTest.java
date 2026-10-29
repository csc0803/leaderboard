package com.chun.backend.repository;

import com.chun.backend.entity.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_shouldGenerateIdAndApplyPrePersistDefaults() {
        Post post = new Post();
        post.setTitle("post1");
        post.setContent("content1");
        Post saved = postRepository.save(post);

        entityManager.flush();
        entityManager.clear();
        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(0L);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void incrementLikeCount_shouldReturn1_whenExists(){
        Post post = new Post();
        post.setTitle("post1");
        post.setContent("content1");
        Post saved = postRepository.save(post);
        int updated = postRepository.incrementLikeCount(saved.getId());

        entityManager.flush();
        entityManager.clear();
        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(found.getLikeCount()).isEqualTo(1L);
    }

    @Test
    void incrementLikeCount_shouldAccumulate_whenCalledMultipleTimes(){
        Post post = new Post();
        post.setTitle("post1");
        post.setContent("content1");
        Post saved = postRepository.save(post);
        postRepository.incrementLikeCount(saved.getId());
        postRepository.incrementLikeCount(saved.getId());
        postRepository.incrementLikeCount(saved.getId());

        entityManager.flush();
        entityManager.clear();
        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(3L);
    }

    @Test
    void incrementLikeCount_shouldReturn0_whenNotExists(){
        int updated = postRepository.incrementLikeCount(-1L);
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void incrementLikeCount_shouldOnlyAffectTargetPost(){
        Post post1 = new Post();
        post1.setTitle("post1");
        post1.setContent("content1");
        Post saved1 = postRepository.save(post1);

        Post post2 = new Post();
        post2.setTitle("post2");
        post2.setContent("content2");
        Post saved2 = postRepository.save(post2);

        postRepository.incrementLikeCount(saved1.getId());

        entityManager.flush();
        entityManager.clear();

        Post found1 = postRepository.findById(saved1.getId()).orElseThrow();
        Post found2 = postRepository.findById(saved2.getId()).orElseThrow();

        assertThat(found1.getLikeCount()).isEqualTo(1L);
        assertThat(found2.getLikeCount()).isEqualTo(0L);

    }

    @Test
    void save_shouldResetLikeCountToZero_evenIfSet(){
        Post post1 = new Post();
        post1.setTitle("post1");
        post1.setContent("content1");
        post1.setLikeCount(5L);                 // ① 存檔「前」設定

        Post saved = postRepository.save(post1); // ② @PrePersist 在這裡把它改回 0

        entityManager.flush();                  // ③ 確保寫進 DB
        entityManager.clear();                  //    清掉快取，逼 findById 真的去查 DB

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(0L);
    }
}
