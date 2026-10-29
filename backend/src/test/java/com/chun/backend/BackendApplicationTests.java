package com.chun.backend;

import com.chun.backend.config.LeaderboardInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@MockitoBean
	private LeaderboardInitializer leaderboardInitializer;   // 啟動時不會真的去連 Redis

	@Test
	void contextLoads() {
	}

}
