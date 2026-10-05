package com.chun.backend.controller;

import com.chun.backend.dto.CreatePostRequest;
import com.chun.backend.entity.Post;
import com.chun.backend.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public List<Post> getPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/posts/{id}")
    public Post getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Post createPost(@Valid @RequestBody CreatePostRequest req) {
        return postService.createPost(req);
    }

    @PostMapping("/posts/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likePost(@PathVariable Long id) {
        postService.likePost(id);
    }

    @GetMapping("/posts/leaderboard")
    public List<Post> getPostsByLeaderboard(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return postService.getLeaderboard(limit);
    }

}
