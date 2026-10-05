package com.chun.backend.service;

import com.chun.backend.dto.CreatePostRequest;
import com.chun.backend.entity.Post;
import com.chun.backend.exception.PostNotFoundException;
import com.chun.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final String LIKES_KEY = "post:likes";

    private final PostRepository postRepository;
    private final StringRedisTemplate redis;

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    @Override
    @Transactional
    public Post createPost(CreatePostRequest req) {
        Post post = new Post();
        post.setTitle(req.title());
        post.setContent(req.content());
        Post saved = postRepository.save(post);
        redis.opsForZSet().add(LIKES_KEY, saved.getId().toString(), 0);
        return saved;
    }

    @Override
    public List<Post> getLeaderboard(int limit) {
        Set<String> ids =  redis.opsForZSet().reverseRange(LIKES_KEY, 0, limit - 1);
        if(ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> rankIds = ids.stream().map(Long::valueOf).toList();

        Map<Long, Post> postsById = postRepository.findAllById(rankIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        return rankIds.stream().map(postsById::get).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional
    public void likePost(Long id) {
        if (postRepository.incrementLikeCount(id) == 0) {
            throw new PostNotFoundException(id);
        }

        redis.opsForZSet().incrementScore(LIKES_KEY, id.toString(), 1);
    }

    @Override
    public void rebuildLeaderboard() {
        List<Post> posts = postRepository.findAll();
        if(posts.isEmpty()) {
            return;
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = posts.stream()
                .map(p -> ZSetOperations.TypedTuple.of(p.getId().toString(), p.getLikeCount().doubleValue()))
                .collect(Collectors.toSet());
        redis.opsForZSet().add(LIKES_KEY, tuples);
    }
}
