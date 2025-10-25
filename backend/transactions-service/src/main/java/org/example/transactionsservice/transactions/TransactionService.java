package org.example.transactionsservice.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactionsByAccountId(String accountId) {
        Optional<List<Transaction>> transactions = transactionRepository.findAllByAccountId(accountId);

        return transactions.orElseGet(ArrayList::new);
    }
}
