package org.example.transactionsservice.transactions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<List<Transaction>> findTop5ByAccountId(UUID accountId);
    Page<Transaction> findAllByAccountId(UUID accountId, Pageable pageable);
    void deleteAllByAccountId(UUID accountId);
}
