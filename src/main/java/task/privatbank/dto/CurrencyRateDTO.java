package task.privatbank.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) for capturing the currency exchange rate data
 * from PrivatBank public API.
 * <p>
 * Fields:
 * - ccy:      The currency code (e.g., USD, EUR).
 * - base_ccy: The base currency code (usually UAH).
 * - buy:      The buy rate.
 * - sale:     The sale (sell) rate.
 */
@Data
public class CurrencyRateDTO {

    /** The currency code, e.g. "USD" or "EUR". */
    private String ccy;

    /** The base currency code, e.g. "UAH". */
    private String base_ccy;

    /** The buy rate for the currency. */
    private double buy;

    /** The sell rate for the currency. */
    private double sale;
}