package com.gymtracker.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @Test
    void aiTaskExecutorUsesExpectedThreadPoolSettings() {
        AsyncConfig config = new AsyncConfig();

        TaskExecutor executor = config.aiTaskExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPool.getCorePoolSize()).isEqualTo(2);
        assertThat(threadPool.getMaxPoolSize()).isEqualTo(5);
        assertThat(threadPool.getThreadNamePrefix()).isEqualTo("ai-handoff-");
    }
}

