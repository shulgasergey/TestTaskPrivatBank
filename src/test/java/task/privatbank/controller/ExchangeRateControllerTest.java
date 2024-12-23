package task.privatbank.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import task.privatbank.conrtoller.ExchangeRateController;
import task.privatbank.model.AverageRate;
import task.privatbank.repository.AverageRateRepository;
import task.privatbank.service.ExchangeRateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
@Import(ExchangeRateControllerTest.TestConfig.class)
class ExchangeRateControllerTest {

    @Configuration
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ExchangeRateService exchangeRateService() {
            return Mockito.mock(ExchangeRateService.class);
        }

        @Bean
        @Primary
        AverageRateRepository averageRateRepository() {
            return Mockito.mock(AverageRateRepository.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private AverageRateRepository averageRateRepository;

    @Test
    @DisplayName("GET /api/exchange/dynamics/day?currency=USD => 200 OK и возвращает список изменений за день")
    void testGetHourlyDynamics_USD() throws Exception {
        BDDMockito.given(exchangeRateService.getHourlyDynamics("USD"))
                .willReturn(List.of(
                        "Time: 2024-01-01T10:00, change: 1.20%",
                        "Time: 2024-01-01T11:00, change: -0.50%"
                ));

        mockMvc.perform(get("/api/exchange/dynamics/day")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Time: 2024-01-01T10:00, change: 1.20%"))
                .andExpect(jsonPath("$[1]").value("Time: 2024-01-01T11:00, change: -0.50%"));
    }

    @Test
    @DisplayName("GET /api/exchange/dynamics/day?currency=AAA => 400 (Bad Request, невалидная валюта)")
    void testGetHourlyDynamics_InvalidCurrency() throws Exception {
        mockMvc.perform(get("/api/exchange/dynamics/day")
                        .param("currency", "AAA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation parameters error"));
    }


    @Test
    @DisplayName("GET /api/exchange/dynamics/hour?currency=EUR => 200 OK и возвращает изменение за последний час")
    void testGetLastHourChange_EUR() throws Exception {
        BDDMockito.given(exchangeRateService.getLastHourChange("EUR"))
                .willReturn(0.35);

        mockMvc.perform(get("/api/exchange/dynamics/hour")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dynamic for last hour for EUR: 0.35%")));
    }

    @Test
    @DisplayName("GET /api/exchange/last?currency=USD => 200 OK и возвращает последний AverageRate")
    void testGetLastRate_USD() throws Exception {
        AverageRate avgRate = new AverageRate();
        avgRate.setId(1L);
        avgRate.setCurrency("USD");
        avgRate.setBuyRate(27.5);
        avgRate.setSellRate(27.8);
        avgRate.setTimestamp(LocalDateTime.now());

        BDDMockito.given(averageRateRepository.findTopByCurrencyOrderByTimestampDesc(eq("USD")))
                .willReturn(Optional.of(avgRate));

        mockMvc.perform(get("/api/exchange/last")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.buyRate").value(27.5))
                .andExpect(jsonPath("$.sellRate").value(27.8));
    }

    @Test
    @DisplayName("GET /api/exchange/last?currency=EUR => 404, если запись не найдена (EntityNotFoundException)")
    void testGetLastRate_NoData() throws Exception {
        BDDMockito.given(averageRateRepository.findTopByCurrencyOrderByTimestampDesc(anyString()))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/exchange/last")
                        .param("currency", "EUR"))
                .andExpect(status().isNotFound()) // Ожидаем статус 404
                .andExpect(jsonPath("$.error").value("Entity not found"))
                .andExpect(jsonPath("$.message").value("Records for currency EUR not found"));
    }
}