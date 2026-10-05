package com.chun.backend.service;

import com.chun.backend.dto.CreatePostRequest;
import com.chun.backend.entity.Post;

import java.util.List;

public interface PostService {

    List<Post> getAllPosts();

    Post getPostById(Long id);

    Post createPost(CreatePostRequest req);

    List<Post> getLeaderboard(int limit);

    void likePost(Long id);

    void rebuildLeaderboard();
}
