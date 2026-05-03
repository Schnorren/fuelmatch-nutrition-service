package com.fuelmatch.nutrition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de execução assíncrona.
 *
 * <p>Define um pool dedicado {@code offPersistenceExecutor} para os jobs de
 * persistência assíncrona dos alimentos vindos da Open Food Facts.
 * Isso garante que picos de busca não bloqueiem as threads de request handling.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool dedicado para persistência assíncrona de alimentos OFF.
     *
     * <p>Configuração:
     * <ul>
     *   <li>Core: 2 threads (operações I/O bound)</li>
     *   <li>Max: 5 threads</li>
     *   <li>Queue: 100 tarefas pendentes</li>
     *   <li>Timeout de idle: 60s</li>
     *   <li>Graceful shutdown: aguarda tasks em curso ao desligar</li>
     * </ul>
     */
    @Bean(name = "offPersistenceExecutor")
    public Executor offPersistenceExecutor(
            @Value("${async.off-persistence.core-pool-size:2}") int coreSize,
            @Value("${async.off-persistence.max-pool-size:5}") int maxSize,
            @Value("${async.off-persistence.queue-capacity:100}") int queueCapacity) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("off-persist-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
