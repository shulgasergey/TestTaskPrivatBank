package task.privatbank.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import task.privatbank.service.ExchangeRateService;
import task.privatbank.dto.CurrencyRateDTO;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scheduler that periodically (every hour) updates average currency rates.
 * <p>
 * Uses a ReentrantLock to prevent overlapping executions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    @Qualifier("exchangeRateService")
    private final ExchangeRateService exchangeRateService;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Scheduled method that runs every hour (3600000 ms).
     * Fetches rates from PrivatBank and MonoBank, then calculates
     * and saves the average rates for USD and EUR.
     */
    @Scheduled(fixedRate = 3600000)
    public void updateAverageRates() {
        log.info("Scheduler triggered to update average rates.");
        if (lock.tryLock()) {
            try {
                log.debug("Acquired lock for scheduled task.");
                List<CurrencyRateDTO> privatRates = exchangeRateService.getPrivatBankRates();
                List<CurrencyRateDTO> monoRates = exchangeRateService.getMonoBankRates();

                exchangeRateService.saveAverageRate(privatRates, monoRates, "USD");
                exchangeRateService.saveAverageRate(privatRates, monoRates, "EUR");

                log.info("Average currency rates successfully updated.");
            } catch (Exception e) {
                log.error("Error during scheduled task execution: {}", e.getMessage(), e);
            } finally {
                lock.unlock();
                log.debug("Lock released after scheduled task.");
            }
        } else {
            log.warn("Scheduled task is already running, skipping execution.");
        }
    }
}