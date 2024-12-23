package task.privatbank.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) for capturing the currency exchange rate data
 * from MonoBank public API.
 * <p>
 * Fields:
 * - currencyCodeA: currency code A (e.g., 840 for USD, 978 for EUR).
 * - currencyCodeB: currency code B (e.g., 980 for UAH).
 * - rateBuy:       The buy rate.
 * - rateSell:      The sell rate.
 */
@Data
public class MonoBankRateDTO {

    /** The target currency code (e.g., 840 for USD, 978 for EUR). */
    private int currencyCodeA;

    /** The base currency code (e.g., 980 for UAH). */
    private int currencyCodeB;

    /** The buy rate for the currency. */
    private double rateBuy;

    /** The sell rate for the currency. */
    private double rateSell;
}