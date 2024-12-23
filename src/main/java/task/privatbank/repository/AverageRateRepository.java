package task.privatbank.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import task.privatbank.model.AverageRate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing AverageRate entities.
 * <p>
 * Provides methods to retrieve the latest entry,
 * the top 2 entries, or entries after a certain timestamp.
 * <p>
 * Uses Caffeine cache annotations to store recent queries.
 */
@Repository
public interface AverageRateRepository extends JpaRepository<AverageRate, Long> {

    /**
     * Finds the latest AverageRate for a given currency.
     * Uses a cache named "lastRate".
     *
     * @param currency The currency code
     * @return The most recent AverageRate record if present
     */
    @Cacheable("lastRate")
    Optional<AverageRate> findTopByCurrencyOrderByTimestampDesc(String currency);

    /**
     * Finds the top 2 most recent AverageRate records for a given currency.
     * Uses a cache named "hourlyRates".
     *
     * @param currency The currency code
     * @return List of up to 2 AverageRate records
     */
    @Cacheable("hourlyRates")
    List<AverageRate> findTop2ByCurrencyOrderByTimestampDesc(String currency);

    /**
     * Finds all AverageRate records for a currency after a given timestamp.
     * Uses a cache named "dailyRates".
     *
     * @param currency  The currency code
     * @param timestamp The cutoff timestamp
     * @return List of AverageRate records ordered ascending by timestamp
     */
    @Cacheable("dailyRates")
    List<AverageRate> findByCurrencyAndTimestampAfterOrderByTimestampAsc(String currency, LocalDateTime timestamp);
}