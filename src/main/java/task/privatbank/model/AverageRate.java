package task.privatbank.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity representing the averaged currency rate stored in the database.
 * <p>
 * Fields:
 * - currency:  The currency code (e.g., "USD", "EUR").
 * - buyRate:   The average buy rate.
 * - sellRate:  The average sell rate.
 * - timestamp: The date/time of record creation.
 */
@Entity
@Table(name = "average_rates")
@Data
public class AverageRate {

    /** The primary key (auto-generated). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The currency code, e.g., "USD" or "EUR". */
    @Column(nullable = false)
    private String currency;

    /** The computed average buy rate. */
    @Column(nullable = false)
    private double buyRate;

    /** The computed average sell rate. */
    @Column(nullable = false)
    private double sellRate;

    /** The timestamp when this record was created. */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}