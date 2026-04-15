package com.jobcupid.job_cupid;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.jobcupid.job_cupid.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class JobCupidApplicationTests {

    // Mock infrastructure beans that require live connections
    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }

}
