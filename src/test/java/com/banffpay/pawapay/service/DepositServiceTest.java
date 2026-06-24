package com.banffpay.pawapay.service;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.config.PawaPaySandboxConfig;
import com.banffpay.pawapay.dto.DepositRequestDTO;
import com.banffpay.pawapay.dto.PawapayDepositResponse;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.util.TransactionStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock
    private PawapayClient pawapayClient;

    @Mock
    private TransactionStore transactionStore;

    @Mock
    private CountryValidationService validationService;

    @Mock
    private PawaPaySandboxConfig sandboxConfig;

    @InjectMocks
    private DepositService depositService;

    private DepositRequestDTO validZambiaRequest;
    private DepositRequestDTO validKenyaRequest;
    private PawapayDepositResponse acceptedResponse;

    @BeforeEach
    void setUp() {
        validZambiaRequest = new DepositRequestDTO();
        validZambiaRequest.setCountry("ZM");
        validZambiaRequest.setPhoneNumber("260700000000");
        validZambiaRequest.setAmount("50");
        validZambiaRequest.setMerchantTransactionId("TEST-ZM-001");

        validKenyaRequest = new DepositRequestDTO();
        validKenyaRequest.setCountry("KE");
        validKenyaRequest.setPhoneNumber("254700000000");
        validKenyaRequest.setAmount("100");
        validKenyaRequest.setMerchantTransactionId("TEST-KE-001");

        acceptedResponse = new PawapayDepositResponse();
        acceptedResponse.setStatus("ACCEPTED");
        acceptedResponse.setDepositId("test-deposit-id");
    }

    @Test
    void testNigeriaUnsupported_ShouldThrowBadRequest() {
        DepositRequestDTO nigeriaRequest = new DepositRequestDTO();
        nigeriaRequest.setCountry("NG");
        nigeriaRequest.setPhoneNumber("2348012345678");
        nigeriaRequest.setAmount("500");
        nigeriaRequest.setMerchantTransactionId("TEST-NG-001");

        when(validationService.validateCountry("NG")).thenReturn(SupportedCountry.NIGERIA);
        when(sandboxConfig.isCountryEnabled("NG")).thenReturn(false);
        when(sandboxConfig.getUnsupportedCountryMessage("NG"))
                .thenReturn("Country NG is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(nigeriaRequest));

        assertTrue(exception.getMessage().contains("Country NG is not supported"));
        verify(sandboxConfig).isCountryEnabled("NG");
    }

    @Test
    void testSouthAfricaUnsupported_ShouldThrowBadRequest() {
        DepositRequestDTO saRequest = new DepositRequestDTO();
        saRequest.setCountry("ZA");
        saRequest.setPhoneNumber("27700000000");
        saRequest.setAmount("50");
        saRequest.setMerchantTransactionId("TEST-ZA-001");

        when(validationService.validateCountry("ZA")).thenReturn(SupportedCountry.SOUTH_AFRICA);
        when(sandboxConfig.isCountryEnabled("ZA")).thenReturn(false);
        when(sandboxConfig.getUnsupportedCountryMessage("ZA"))
                .thenReturn("Country ZA is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(saRequest));

        assertTrue(exception.getMessage().contains("Country ZA is not supported"));
        verify(sandboxConfig).isCountryEnabled("ZA");
    }

    @Test
    void testValidZambiaRequest_ShouldPassValidation() {
        when(validationService.validateCountry("ZM")).thenReturn(SupportedCountry.ZAMBIA);
        doNothing().when(validationService).validateAmount(any(BigDecimal.class), anyString());
        doNothing().when(validationService).validatePhoneNumber(anyString(), anyString());
        when(sandboxConfig.isCountryEnabled("ZM")).thenReturn(true);
        when(sandboxConfig.isNetworkBlocked(anyString())).thenReturn(false);
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(acceptedResponse);

        assertDoesNotThrow(() -> depositService.initiateDeposit(validZambiaRequest));
        verify(validationService).validateCountry("ZM");
        verify(sandboxConfig).isCountryEnabled("ZM");
    }

    @Test
    void testValidKenyaRequest_ShouldPassValidation() {
        when(validationService.validateCountry("KE")).thenReturn(SupportedCountry.KENYA);
        doNothing().when(validationService).validateAmount(any(BigDecimal.class), anyString());
        doNothing().when(validationService).validatePhoneNumber(anyString(), anyString());
        when(sandboxConfig.isCountryEnabled("KE")).thenReturn(true);
        when(sandboxConfig.isNetworkBlocked(anyString())).thenReturn(false);
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(acceptedResponse);

        assertDoesNotThrow(() -> depositService.initiateDeposit(validKenyaRequest));
        verify(validationService).validateCountry("KE");
        verify(sandboxConfig).isCountryEnabled("KE");
    }

    @Test
    void testInvalidPhoneNumber_ShouldThrowBadRequest() {
        DepositRequestDTO invalidPhoneRequest = new DepositRequestDTO();
        invalidPhoneRequest.setCountry("ZM");
        invalidPhoneRequest.setPhoneNumber("700000000");
        invalidPhoneRequest.setAmount("50");

        when(validationService.validateCountry("ZM")).thenReturn(SupportedCountry.ZAMBIA);
        doNothing().when(validationService).validateAmount(any(BigDecimal.class), anyString());
        doThrow(new IllegalArgumentException("Invalid phone number format for ZM"))
                .when(validationService).validatePhoneNumber(anyString(), anyString());
        when(sandboxConfig.isCountryEnabled("ZM")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(invalidPhoneRequest));

        assertTrue(exception.getMessage().contains("Invalid phone number format"));
    }

    @Test
    void testAmountBelowMinimum_ShouldThrowBadRequest() {
        DepositRequestDTO lowAmountRequest = new DepositRequestDTO();
        lowAmountRequest.setCountry("ZM");
        lowAmountRequest.setPhoneNumber("260700000000");
        lowAmountRequest.setAmount("1");

        when(validationService.validateCountry("ZM")).thenReturn(SupportedCountry.ZAMBIA);
        doThrow(new IllegalArgumentException("Amount below minimum for ZM"))
                .when(validationService).validateAmount(any(BigDecimal.class), anyString());
        when(sandboxConfig.isCountryEnabled("ZM")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(lowAmountRequest));

        assertTrue(exception.getMessage().contains("Amount below minimum"));
    }
}