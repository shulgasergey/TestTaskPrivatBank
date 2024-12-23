package task.privatbank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import task.privatbank.dto.CurrencyRateDTO;
import task.privatbank.dto.MonoBankRateDTO;
import task.privatbank.model.AverageRate;
import task.privatbank.repository.AverageRateRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that fetches currency rates from external APIs (PrivatBank, MonoBank),
 * calculates average rates, and stores them in the database.
 * <p>
 * Also provides methods to retrieve hourly dynamics and last-hour changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final WebClient webClient = WebClient.builder().build();

    /**
     * Lock object for synchronization during average rate saving.
     */
    private final Object lock = new Object();

    @Qualifier("averageRateRepository")
    private final AverageRateRepository averageRateRepository;

    /**
     * Saves the average rate for the specified currency,
     * computed from PrivatBank and MonoBank data.
     *
     * @param privatRates list of CurrencyRateDTO from PrivatBank
     * @param monoRates   list of CurrencyRateDTO from MonoBank
     * @param currency    "USD" or "EUR"
     */
    @CacheEvict(value = {"lastRate", "hourlyRates", "dailyRates"}, key = "#currency")
    @Transactional
    public void saveAverageRate(List<CurrencyRateDTO> privatRates, List<CurrencyRateDTO> monoRates, String currency) {
        log.info("Saving average rate for currency={}", currency);
        synchronized (lock) {
            log.debug("Entered synchronized block for currency={}", currency);

            double privatBuy = privatRates.stream()
                    .filter(rate -> rate.getCcy().equalsIgnoreCase(currency))
                    .findFirst()
                    .map(CurrencyRateDTO::getBuy)
                    .orElse(0.0);

            double monoBuy = monoRates.stream()
                    .filter(rate -> rate.getCcy().equalsIgnoreCase(currency))
                    .findFirst()
                    .map(CurrencyRateDTO::getBuy)
                    .orElse(0.0);

            double privatSell = privatRates.stream()
                    .filter(rate -> rate.getCcy().equalsIgnoreCase(currency))
                    .findFirst()
                    .map(CurrencyRateDTO::getSale)
                    .orElse(0.0);

            double monoSell = monoRates.stream()
                    .filter(rate -> rate.getCcy().equalsIgnoreCase(currency))
                    .findFirst()
                    .map(CurrencyRateDTO::getSale)
                    .orElse(0.0);

            // Calculate average
            double averageBuy = Math.round(((privatBuy + monoBuy) / 2) * 100.0) / 100.0;
            double averageSell = Math.round(((privatSell + monoSell) / 2) * 100.0) / 100.0;

            AverageRate rate = new AverageRate();
            rate.setCurrency(currency);
            rate.setBuyRate(averageBuy);
            rate.setSellRate(averageSell);
            rate.setTimestamp(LocalDateTime.now());

            averageRateRepository.save(rate);
            log.info("Average rate saved: currency={}, buyRate={}, sellRate={}",
                    currency, averageBuy, averageSell);
        }
    }

    /**
     * Returns a list of hourly changes (in percent) for today's rates.
     *
     * @param currency must be "USD" or "EUR"
     * @return list of changes with timestamps
     */
    public List<String> getHourlyDynamics(String currency) {
        log.debug("Retrieving hourly dynamics for currency={}", currency);
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<AverageRate> rates = averageRateRepository.findByCurrencyAndTimestampAfterOrderByTimestampAsc(
                currency.toUpperCase(), startOfDay);

        if (rates.size() < 2) {
            log.warn("Insufficient data for hourly dynamics per day for currency={}", currency);
            throw new RuntimeException("Insufficient data for hourly dynamics per day");
        }

        List<String> dynamics = new ArrayList<>();
        for (int i = 1; i < rates.size(); i++) {
            double previous = rates.get(i - 1).getBuyRate();
            double current = rates.get(i).getBuyRate();
            double change = Math.round(((current - previous) / previous) * 10000.0) / 100.0;

            dynamics.add("Time: " + rates.get(i).getTimestamp() + ", change: " + change + "%");
        }
        log.info("Hourly dynamics retrieved for currency={}, entries={}", currency, dynamics.size());
        return dynamics;
    }

    /**
     * Returns the change percentage for the last hour,
     * using the top 2 most recent records for the specified currency.
     *
     * @param currency "USD" or "EUR"
     * @return a Double representing the percent change (rounded to 2 decimals)
     */
    public Double getLastHourChange(String currency) {
        log.debug("Retrieving last hour change for currency={}", currency);
        List<AverageRate> rates = averageRateRepository.findTop2ByCurrencyOrderByTimestampDesc(currency.toUpperCase());

        if (rates.size() < 2) {
            log.warn("Not enough data for last hour change for currency={}", currency);
            throw new RuntimeException("There are not enough data to calculate the dynamics for the last hour");
        }

        double lastRate = rates.get(0).getBuyRate();
        double previousRate = rates.get(1).getBuyRate();

        double result = Math.round(((lastRate - previousRate) / previousRate) * 10000.0) / 100.0;
        log.info("Last hour change for currency={} is {}%", currency, result);
        return result;
    }

    /**
     * Calls PrivatBank API to retrieve currency rates.
     *
     * @return a list of CurrencyRateDTO objects
     */
    public List<CurrencyRateDTO> getPrivatBankRates() {
        log.debug("Fetching rates from PrivatBank API...");
        String PRIVATBANK_URL = "https://api.privatbank.ua/p24api/pubinfo?exchange&coursid=5";
        return webClient.get()
                .uri(PRIVATBANK_URL)
                .retrieve()
                .bodyToFlux(CurrencyRateDTO.class)
                .collectList()
                .block();
    }

    /**
     * Calls MonoBank API to retrieve currency rates (USD, EUR vs UAH).
     * Implements retry for 429 Too Many Requests.
     *
     * @return a list of CurrencyRateDTO objects
     */
    public List<CurrencyRateDTO> getMonoBankRates() {
        log.debug("Fetching rates from MonoBank API...");
        String MONOBANK_URL = "https://api.monobank.ua/bank/currency";
        List<MonoBankRateDTO> monoRates = webClient.get()
                .uri(MONOBANK_URL)
                .retrieve()
                .bodyToFlux(MonoBankRateDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 429) {
                        log.error("429 Too Many Requests from MonoBank API. Waiting before the next request.");
                    }
                    return Mono.empty();
                })
                .collectList()
                .block();

        if (monoRates == null) {
            log.warn("MonoBank returned null or empty list.");
            return List.of();
        }

        // Filter out only UAH-based rates (980) and USD/EUR
        return monoRates.stream()
                .filter(rate -> rate.getCurrencyCodeB() == 980) // UAH
                .filter(rate -> rate.getCurrencyCodeA() == 840 || rate.getCurrencyCodeA() == 978) // USD or EUR
                .map(rate -> {
                    CurrencyRateDTO dto = new CurrencyRateDTO();
                    dto.setCcy(mapCurrencyCodeToString(rate.getCurrencyCodeA()));
                    dto.setBase_ccy("UAH");
                    dto.setBuy(rate.getRateBuy());
                    dto.setSale(rate.getRateSell());
                    return dto;
                })
                .toList();
    }

    /**
     * Helper method to map numeric currency codes to string names.
     *
     * @param code The numeric currency code
     * @return "USD" if 840, "EUR" if 978, or null otherwise
     */
    private String mapCurrencyCodeToString(int code) {
        return switch (code) {
            case 840 -> "USD";
            case 978 -> "EUR";
            default -> null;
        };
    }
}
