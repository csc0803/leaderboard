package com.chun.backend.controller;

import com.chun.backend.dto.CreatePostRequest;
import com.chun.backend.entity.Post;
import com.chun.backend.exception.PostNotFoundException;
import com.chun.backend.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(PostController.class)   // 只載入 Web 層：PostController + GlobalExceptionHandler + CorsConfig，不連 MySQL / Redis
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    private Post newPost(Long id, String title, String content, Long like) {
        Post p = new Post();
        p.setId(id);
        p.setTitle(title);
        p.setContent(content);
        p.setLikeCount(like);
        return p;
    }

    // ---------- GET /api/posts ----------

    @Test
    void getPosts_shouldReturn200WithList() throws Exception {
        Post post = newPost(1L, "title", "content", 5L);
        when(postService.getAllPosts()).thenReturn(List.of(post));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("title"))
                .andExpect(jsonPath("$[0].content").value("content"))
                .andExpect(jsonPath("$[0].likeCount").value(5));
    }

    @Test
    void getPosts_shouldReturnEmptyArray_whenNoPosts() throws Exception {
        when(postService.getAllPosts()).thenReturn(List.of());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ---------- GET /api/posts/{id} ----------

    @Test
    void getPostById_shouldReturn200_whenExists() throws Exception {
        Post post = newPost(1L, "title", "content", 5L);

        when(postService.getPostById(1L)).thenReturn(post);

        mockMvc.perform(get("/api/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.likeCount").value(5));

        verify(postService).getPostById(1L);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void getPostById_shouldReturn404WithErrorBody_whenNotExists() throws Exception {
        when(postService.getPostById(1L)).thenThrow(new PostNotFoundException(1L));

        mockMvc.perform(get("/api/posts/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Post not found: 1"));

        verify(postService).getPostById(1L);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void getPostById_shouldReturn400_whenIdIsNotNumber() throws Exception {

        mockMvc.perform(get("/api/posts/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("id: invalid value abc"));

        verifyNoInteractions(postService);
    }

    // ---------- POST /api/posts ----------

    @Test
    void createPost_shouldReturn201WithCreatedPost() throws Exception {
        CreatePostRequest req = new CreatePostRequest("title", "content");

        Post created = newPost(1L, "title", "content", 0L);

        when(postService.createPost(req)).thenReturn(created);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.likeCount").value(0));

        verify(postService).createPost(req);
        verifyNoMoreInteractions(postService);


    }

    @ParameterizedTest
    @ValueSource(strings = {"", "    "})
    void createPost_shouldReturn400_whenTitleIsBlank(String title) throws Exception {
        CreatePostRequest req = new CreatePostRequest(title, "content");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(startsWith("title:")));

        verifyNoInteractions(postService);
    }

    @Test
    void createPost_shouldReturn400_whenTitleIsMissing() throws Exception {
        String jsonWithoutTitle = "{ \"content\": \"沒有標題欄位\" }"; // title key 整個不存在，跟「空字串」是不同 case

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutTitle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(startsWith("title:")));

        verifyNoInteractions(postService);
    }

    @Test
    void createPost_shouldReturn201_whenTitleIs100Chars() throws Exception {
        CreatePostRequest req = new CreatePostRequest("a".repeat(100), "content");

        Post created = newPost(1L, "a".repeat(100), "content", 0L);

        when(postService.createPost(req)).thenReturn(created);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("a".repeat(100)))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.likeCount").value(0));

        verify(postService).createPost(req);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void createPost_shouldReturn400_whenTitleIs101Chars() throws Exception {
        CreatePostRequest req = new CreatePostRequest("a".repeat(101), "content");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(startsWith("title:")));

        verifyNoInteractions(postService);
    }

    @Test
    void createPost_shouldReturn201_whenContentIsNull() throws Exception {
        CreatePostRequest req = new CreatePostRequest("title", null);

        Post created = newPost(1L, "title", null, 0L);

        when(postService.createPost(req)).thenReturn(created);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.likeCount").value(0));

        verify(postService).createPost(req);
        verifyNoMoreInteractions(postService);
    }

    // ---------- POST /api/posts/{id}/like ----------

    @Test
    void likePost_shouldReturn204_whenExists() throws Exception {

        mockMvc.perform(post("/api/posts/{id}/like", 1)
                )
                .andExpect(status().isNoContent())
                .andExpect(status().is2xxSuccessful());

        verify(postService).likePost(1L);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void likePost_shouldReturn404_whenNotExists() throws Exception {
        doThrow(new PostNotFoundException(1L)).when(postService).likePost(1L);

        mockMvc.perform(post("/api/posts/{id}/like", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Post not found: 1"));

        verify(postService).likePost(1L);
        verifyNoMoreInteractions(postService);
    }

    // ---------- GET /api/posts/leaderboard ----------

    @Test
    void getLeaderboard_shouldUseDefaultLimit10_whenLimitNotGiven() throws Exception {
        when(postService.getLeaderboard(10)).thenReturn(List.of(
                newPost(2L, "second", "content", 8L),
                newPost(1L, "first", "content", 3L)));

        mockMvc.perform(get("/api/posts/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))     // 順序由 service（Redis）決定，controller 不能打亂
                .andExpect(jsonPath("$[0].likeCount").value(8))
                .andExpect(jsonPath("$[1].id").value(1));

        verify(postService).getLeaderboard(10);
        verifyNoMoreInteractions(postService);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void getLeaderboard_shouldReturn200_whenLimitIsBoundary(int limit) throws Exception {
        when(postService.getLeaderboard(limit)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/leaderboard").param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(postService).getLeaderboard(limit);
        verifyNoMoreInteractions(postService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void getLeaderboard_shouldReturn400_whenLimitOutOfRange(int limit) throws Exception {
        mockMvc.perform(get("/api/posts/leaderboard").param("limit", String.valueOf(limit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(startsWith("limit:")));   // 後半段訊息語系跟隨 JVM，只比對欄位名

        verifyNoInteractions(postService);
    }

    @Test
    void getLeaderboard_shouldReturn400_whenLimitIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/posts/leaderboard").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("limit: invalid value abc"));

        verifyNoInteractions(postService);
    }

    @Test
    void getLeaderboard_shouldNotBeTreatedAsPostId() throws Exception {
        // /posts/leaderboard 與 /posts/{id} 都符合，Spring 應優先選字面路徑，而不是把 "leaderboard" 當 id 轉 Long 失敗回 400
        when(postService.getLeaderboard(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/leaderboard"))
                .andExpect(status().isOk());

        verify(postService).getLeaderboard(10);
        verify(postService, never()).getPostById(any());
    }

    // ---------- CORS ----------

    @Test
    void cors_shouldAllowPreflight_fromFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/posts")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));

        verifyNoInteractions(postService);   // preflight 不會進到 controller
    }

    @Test
    void cors_shouldReject_fromOtherOrigin() throws Exception {
        mockMvc.perform(options("/api/posts")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        verifyNoInteractions(postService);
    }

    @Test
    void cors_shouldReject_whenMethodNotAllowed() throws Exception {
        mockMvc.perform(options("/api/posts/{id}", 1L)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        verifyNoInteractions(postService);
    }
}
