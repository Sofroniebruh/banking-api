package org.example.accountservice.services;

import org.example.accountservice.accounts.AccountCurrency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CurrencyConversionService {
    
    @Value("${exchange.eur-to-usd:1.16}")
    private BigDecimal eurToUsdRate;
    
    @Value("${exchange.usd-to-eur:0.86}")
    private BigDecimal usdToEurRate;
    
    public BigDecimal convert(BigDecimal amount, AccountCurrency from, AccountCurrency to) {
        if (from == to) {
            return amount;
        }
        
        return switch (from) {
            case EUR -> {
                if (to == AccountCurrency.USD) {
                    yield amount.multiply(eurToUsdRate);
                }
                yield amount;
            }
            case USD -> {
                if (to == AccountCurrency.EUR) {
                    yield amount.multiply(usdToEurRate);
                }
                yield amount;
            }
        };
    }
}