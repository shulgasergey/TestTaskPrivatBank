package task.privatbank.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import task.privatbank.dto.CurrencyRateDTO;
import task.privatbank.model.AverageRate;
import task.privatbank.repository.AverageRateRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private AverageRateRepository averageRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("saveAverageRate() saves average rate correctly")
    void testSaveAverageRate() {
        // Arrange
        List<CurrencyRateDTO> privatRates = List.of(
                createCurrencyRate("USD", 27.0, 27.3),
                createCurrencyRate("EUR", 30.0, 30.5)
        );
        List<CurrencyRateDTO> monoRates = List.of(
                createCurrencyRate("USD", 27.1, 27.4),
                createCurrencyRate("EUR", 30.1, 30.6)
        );

        // Act
        exchangeRateService.saveAverageRate(privatRates, monoRates, "USD");

        // Assert
        ArgumentCaptor<AverageRate> captor = ArgumentCaptor.forClass(AverageRate.class);
        verify(averageRateRepository, times(1)).save(captor.capture());

        AverageRate savedRate = captor.getValue();
        assertEquals("USD", savedRate.getCurrency());
        assertEquals(27.05, savedRate.getBuyRate());
        assertEquals(27.35, savedRate.getSellRate());
    }

    @Test
    @DisplayName("getHourlyDynamics() throws exception when data is insufficient")
    void testGetHourlyDynamics_NotEnoughData() {
        // Arrange
        when(averageRateRepository.findByCurrencyAndTimestampAfterOrderByTimestampAsc(eq("USD"), any()))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> exchangeRateService.getHourlyDynamics("USD"));
    }

    @Test
    @DisplayName("getHourlyDynamics() calculates hourly dynamics correctly")
    void testGetHourlyDynamics_ValidData() {
        // Arrange
        AverageRate r1 = createAverageRate("USD", 27.0, LocalDateTime.now().minusHours(2));
        AverageRate r2 = createAverageRate("USD", 27.3, LocalDateTime.now().minusHours(1));

        when(averageRateRepository.findByCurrencyAndTimestampAfterOrderByTimestampAsc(eq("USD"), any()))
                .thenReturn(List.of(r1, r2));

        // Act
        List<String> dynamics = exchangeRateService.getHourlyDynamics("USD");

        // Assert
        assertEquals(1, dynamics.size());
        assertTrue(dynamics.get(0).contains("change: 1.11%")); // Example: ((27.3 - 27.0) / 27.0) * 100
    }

    @Test
    @DisplayName("getLastHourChange() throws exception when not enough data")
    void testGetLastHourChange_NotEnoughData() {
        // Arrange
        when(averageRateRepository.findTop2ByCurrencyOrderByTimestampDesc(eq("USD")))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> exchangeRateService.getLastHourChange("USD"));
    }

    @Test
    @DisplayName("getLastHourChange() calculates last hour change correctly")
    void testGetLastHourChange_ValidData() {
        // Arrange
        AverageRate latest = createAverageRate("USD", 27.5, LocalDateTime.now());
        AverageRate previous = createAverageRate("USD", 27.0, LocalDateTime.now().minusHours(1));

        when(averageRateRepository.findTop2ByCurrencyOrderByTimestampDesc(eq("USD")))
                .thenReturn(List.of(latest, previous));

        // Act
        Double change = exchangeRateService.getLastHourChange("USD");

        // Assert
        assertNotNull(change);
        assertEquals(1.85, change); // Example: ((27.5 - 27.0) / 27.0) * 100
    }

    // Helper method to create CurrencyRateDTO
    private CurrencyRateDTO createCurrencyRate(String ccy, double buy, double sale) {
        CurrencyRateDTO dto = new CurrencyRateDTO();
        dto.setCcy(ccy);
        dto.setBase_ccy("UAH");
        dto.setBuy(buy);
        dto.setSale(sale);
        return dto;
    }

    // Helper method to create AverageRate
    private AverageRate createAverageRate(String currency, double buyRate, LocalDateTime timestamp) {
        AverageRate rate = new AverageRate();
        rate.setCurrency(currency);
        rate.setBuyRate(buyRate);
        rate.setTimestamp(timestamp);
        return rate;
    }
}
