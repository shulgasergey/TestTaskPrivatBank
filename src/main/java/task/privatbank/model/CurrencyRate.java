package task.privatbank.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity representing a single currency rate entry from a particular source
 * before averaging.
 * <p>
 * Fields:
 * - currency:  The currency code (e.g., "USD", "EUR").
 * - buyRate:   The buy rate from the source.
 * - sellRate:  The sell rate from the source.
 * - timestamp: The date/time of record creation.
 */
@Table(name = "currency_rates")
@Entity
@Data
public class CurrencyRate {

    /** The primary key (auto-generated). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The currency code, e.g., "USD" or "EUR". */
    @Column(nullable = false)
    private String currency;

    /** The buy rate from the source. */
    @Column(nullable = false)
    private double buyRate;

    /** The sell rate from the source. */
    @Column(nullable = false)
    private double sellRate;

    /** The timestamp when this record was created. */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}