package task.privatbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the currency tracking project.
 * <p>
 * Annotations:
 * - @EnableScheduling to activate scheduled tasks.
 * - @EnableCaching to enable caching in the Spring context.
 * <p>
 * Author - Serhii Shulha
 * Test Task for Privat Bank
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class TestTaskPrivatBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestTaskPrivatBankApplication.class, args);
    }
}