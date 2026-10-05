package com.chun.backend.config;

import com.chun.backend.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LeaderboardInitializer implements ApplicationRunner {

    private final PostService postService;
    private final StringRedisTemplate redis;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long size = redis.opsForZSet().zCard("post:likes");
        if(size != null && size > 0) {
            log.info("Leaderboard zset already has {} entries, skip rebuild", size);
            return;
        }
        postService.rebuildLeaderboard();
        log.info("Leaderboard zset rebuilt from MySQL");
    }
}
