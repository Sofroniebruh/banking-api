package org.example.accountservice.accounts.records;

import java.util.List;

public record AccountTransactionsDTO(
        AccountReturnDTO account,
        List<TransactionDTO> transactions
) {
    public static AccountTransactionsDTO from(AccountReturnDTO account, List<TransactionDTO> transactions) {
        return new AccountTransactionsDTO(account, transactions);
    }
}
