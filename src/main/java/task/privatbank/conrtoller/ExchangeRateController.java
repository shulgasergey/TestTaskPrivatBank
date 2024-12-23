package task.privatbank.conrtoller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import task.privatbank.service.ExchangeRateService;
import task.privatbank.model.AverageRate;
import task.privatbank.repository.AverageRateRepository;

import java.util.List;

/**
 * REST controller providing endpoints for retrieving currency rate information.
 * <p>
 * Endpoints:
 * - /dynamics/day?currency=USD|EUR: returns hourly dynamics for the current day
 * - /dynamics/hour?currency=USD|EUR: returns change for the last hour
 * - /last?currency=USD|EUR: returns the latest AverageRate entry
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("api/exchange")
@Slf4j
public class ExchangeRateController {

    @Qualifier("exchangeRateService")
    private final ExchangeRateService exchangeRateService;
    @Qualifier("averageRateRepository")
    private final AverageRateRepository averageRateRepository;

    /**
     * Returns a list of strings describing hourly change percentages
     * for the specified currency from the start of the current day.
     *
     * @param currency must be "USD" or "EUR"
     * @return list of changes in percentage, with timestamps
     */
    @GetMapping("/dynamics/day")
    public List<String> getHourlyDynamics(
            @RequestParam
            @Pattern(regexp = "USD|EUR", message = "Currency must be 'USD' or 'EUR'")
            String currency
    ) {
        log.info("Handling GET request for hourly dynamics of currency={}", currency);
        return exchangeRateService.getHourlyDynamics(currency);
    }

    /**
     * Returns the last hour change (in percent) for the specified currency.
     *
     * @param currency must be "USD" or "EUR"
     * @return a string with the last hour change percentage
     */
    @GetMapping("/dynamics/hour")
    public String getLastHourChange(
            @RequestParam
            @Pattern(regexp = "USD|EUR", message = "Currency must be 'USD' or 'EUR'")
            String currency
    ) {
        log.info("Handling GET request for last hour change of currency={}", currency);
        double change = exchangeRateService.getLastHourChange(currency);
        return "Dynamic for last hour for " + currency + ": " + change + "%";
    }

    /**
     * Retrieves the latest AverageRate record for the specified currency.
     *
     * @param currency must be "USD" or "EUR"
     * @return the most recent AverageRate, or throws an exception if not found
     */
    @GetMapping("/last")
    public AverageRate getLastRate(
            @RequestParam
            @Pattern(regexp = "USD|EUR", message = "Currency must be 'USD' or 'EUR'")
            String currency
    ) {
        log.info("Handling GET request for the latest rate of currency={}", currency);
        return averageRateRepository.findTopByCurrencyOrderByTimestampDesc(currency.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Records for currency " + currency + " not found"));
    }
}