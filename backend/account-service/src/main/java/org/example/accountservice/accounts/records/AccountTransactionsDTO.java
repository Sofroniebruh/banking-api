package org.example.accountservice.accounts.records;

import org.example.accountservice.accounts.Account;

import java.util.List;

public record AccountTransactionsDTO(
        Account account,
        List<TransactionDTO> transactions
) {
    public static AccountTransactionsDTO from(Account account, List<TransactionDTO> transactions) {
        return new AccountTransactionsDTO(account, transactions);
    }
}
