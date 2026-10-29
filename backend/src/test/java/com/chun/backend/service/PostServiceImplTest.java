package com.chun.backend.service;

import com.chun.backend.dto.CreatePostRequest;
import com.chun.backend.entity.Post;
import com.chun.backend.exception.PostNotFoundException;
import com.chun.backend.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // 純單元測試：不啟動 Spring，不連 MySQL / Redis
class PostServiceImplTest {

    private static final String LIKES_KEY = "post:likes";

    @Mock
    private PostRepository postRepository;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Captor
    private ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> tuplesCaptor;

    @InjectMocks                       // 用上面兩個 mock 呼叫 PostServiceImpl 的建構子
    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        // service 每次都是 redis.opsForZSet().xxx()，所以先讓 opsForZSet() 回傳 mock 的 zSetOps。
        // lenient()：有些測試根本不會碰 Redis，Mockito 預設的 strict stubs 會報 UnnecessaryStubbingException
        lenient().when(redis.opsForZSet()).thenReturn(zSetOps);
    }

    private Post post(Long id, String title) {
        Post p = new Post();
        p.setId(id);
        p.setTitle(title);
        p.setContent("content");
        p.setLikeCount(0L);
        return p;
    }

    // ---------- likePost ----------

    @Test
    void likePost_shouldIncrementRedisScore_whenPostExists() {
        when(postRepository.incrementLikeCount(1L)).thenReturn(1);

        postService.likePost(1L);

        verify(zSetOps).incrementScore(LIKES_KEY, "1", 1);
    }

    @Test
    void likePost_shouldThrowAndNotTouchRedis_whenPostNotExists() {
        when(postRepository.incrementLikeCount(99L)).thenReturn(0);

        assertThatThrownBy(() -> postService.likePost(99L))
                .isInstanceOf(PostNotFoundException.class);

        // MySQL 沒更新到就不能動 Redis，否則兩邊讚數會不一致
        verifyNoInteractions(redis);
    }

    // ---------- getLeaderboard ----------

    @Test
    void getLeaderboard_shouldKeepRedisOrder_andSkipIdsMissingInMySQL() {
        // Redis 排名：3 > 1 > 2 > 4（用 LinkedHashSet 保留順序，模擬 ZREVRANGE 的回傳）
        when(zSetOps.reverseRange(LIKES_KEY, 0, 9))
                .thenReturn(new LinkedHashSet<>(List.of("3", "1", "2", "4")));
        // MySQL 回傳順序故意打亂，而且沒有 id=4（模擬 Redis 有、MySQL 已不存在的資料）
        when(postRepository.findAllById(List.of(3L, 1L, 2L, 4L)))
                .thenReturn(List.of(post(1L, "a"), post(2L, "b"), post(3L, "c")));

        List<Post> result = postService.getLeaderboard(10);

        assertThat(result).extracting(Post::getId).containsExactly(3L, 1L, 2L);
    }

    @Test
    void getLeaderboard_shouldReturnEmptyWithoutQueryingDb_whenRedisIsEmpty() {
        when(zSetOps.reverseRange(LIKES_KEY, 0, 9)).thenReturn(null);

        List<Post> result = postService.getLeaderboard(10);

        assertThat(result).isEmpty();
        verify(postRepository, never()).findAllById(any());
    }

    @Test
    void getLeaderboard_shouldReturnEmptyWithoutQueryingDb_whenRedisReturnsEmptySet() {
        when(zSetOps.reverseRange(LIKES_KEY, 0, 9)).thenReturn(Set.of());

        List<Post> result = postService.getLeaderboard(10);

        assertThat(result).isEmpty();
        verify(postRepository, never()).findAllById(any());
    }

    // ---------- getPost ----------

    @Test
    void getPostById_shouldReturnPost_whenPostExists(){

        when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L, "a")));

        Post res = postService.getPostById(1L);
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getTitle()).isEqualTo("a");

    }

    @Test
    void getPostById_shouldThrow_whenNotExists(){

        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L)).isInstanceOf(PostNotFoundException.class);

    }

    // ---------- createPost ----------
    @Test
    void createPost_shouldReturnPost() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        CreatePostRequest req = new CreatePostRequest("a", "content");
        Post res = postService.createPost(req);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getTitle()).isEqualTo("a");
        assertThat(res.getContent()).isEqualTo("content");

        verify(zSetOps).add(LIKES_KEY, "1", 0);
    }

    // ---------- Leaderboard ----------

    @Test
    void rebuildLeaderboard_shouldWriteAllPostsWithLikeCountAsScore(){

        Post p1 =  post(1L, "a");
        p1.setLikeCount(3L);
        Post p2 = post(2L, "b");
        when(postRepository.findAll()).thenReturn(List.of(p1, p2));

        postService.rebuildLeaderboard();

        verify(zSetOps).add(eq(LIKES_KEY), tuplesCaptor.capture());
        assertThat(tuplesCaptor.getValue())
                .extracting(ZSetOperations.TypedTuple::getValue, ZSetOperations.TypedTuple::getScore)
                .containsExactlyInAnyOrder(
                        tuple("1", 3.0),
                        tuple("2", 0.0));
    }

    @Test
    void rebuildLeaderboard_shouldNotTouchRedis_whenNoPosts() {
        when(postRepository.findAll()).thenReturn(List.of());

        postService.rebuildLeaderboard();

        verifyNoInteractions(redis);
    }

    @Test
    void getAllPosts_shouldReturnAllPosts() {
        Post p1 =  post(1L, "a");
        Post p2 = post(2L, "b");
        when(postRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Post> res = postService.getAllPosts();

        assertThat(res).containsExactly(p1, p2);
        verifyNoInteractions(redis);   // 查全部只讀 MySQL，不應該碰 Redis
    }
}
