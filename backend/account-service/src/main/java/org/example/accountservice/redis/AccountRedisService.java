package org.example.accountservice.redis;

import lombok.RequiredArgsConstructor;
import org.example.accountservice.accounts.Account;
import org.example.accountservice.accounts.AccountCurrency;
import org.example.accountservice.accounts.records.AccountReturnDTO;
import org.example.accountservice.configs.exceptions.TransactionsMessageFailedResponseException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AccountRedisService {
    private final RedisService redisService;
    private static final String ACCOUNT_KEY_PREFIX = "account:";

    public void addAccountToRedis(Account account) {
        String key = ACCOUNT_KEY_PREFIX + account.getId();
        
        Map<String, Object> accountData = assembleAccountData(account);
        
        redisService.setHash(key, accountData);
    }

    public void updateAccountFromRedis(Account account) {
        String key = ACCOUNT_KEY_PREFIX + account.getId();

        Map<String, Object> accountData = assembleAccountData(account);
        redisService.delValue(key);
        redisService.setHash(key, accountData);
    }

    public AccountReturnDTO getAccountById(String id) {
        try {
            String key = ACCOUNT_KEY_PREFIX + id;

            Object userId = redisService.getHashValue(key, "userId");

            if (userId == null) {
                return null;
            }

            String createdAt = redisService.getHashValue(key, "createdAt").toString();
            String updatedAt = redisService.getHashValue(key, "updatedAt").toString();
            String balance = getBalance(id);
            AccountCurrency currency = getCurrency(id);

            return AccountReturnDTO.from(UUID.fromString(id), userId.toString(), Double.parseDouble(balance), LocalDateTime.parse(createdAt), LocalDateTime.parse(updatedAt), currency);
        } catch (ClassCastException e) {
            throw new TransactionsMessageFailedResponseException("Redis returned different types than expected");
        } catch (NullPointerException e) {
            throw new TransactionsMessageFailedResponseException("Redis returned null values");
        }
    }
    
    public AccountCurrency getCurrency(String accountId) {
        String key = ACCOUNT_KEY_PREFIX + accountId;

        String currencyStr = (String) redisService.getHashValue(key, "currency");
        return AccountCurrency.valueOf(currencyStr);
    }
    
    public String getBalance(String accountId) {
        String key = ACCOUNT_KEY_PREFIX + accountId;

        return (String) redisService.getHashValue(key, "balance");
    }

    private Map<String, Object> assembleAccountData(Account account) {
        return Map.of(
                "userId", account.getUserId(),
                "createdAt", account.getCreatedAt(),
                "updatedAt", account.getUpdatedAt(),
                "balance", account.getBalance().toString(),
                "currency", account.getCurrency().toString()
        );
    }
}
