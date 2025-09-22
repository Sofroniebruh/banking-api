package com.example.banking_api.accounts;

import com.example.banking_api.accounts.exceptions.InitialDepositRequiredException;
import com.example.banking_api.accounts.records.AccountDTO;
import com.example.banking_api.accounts.records.CreateAccountDTO;
import com.example.banking_api.common.PaginatedResponse;
import com.example.banking_api.users.UserRepository;
import com.example.banking_api.users.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    public AccountRepository accountRepository;
    public UserRepository userRepository;
    @Value("${MINIMUM_DEPOSIT}")
    private BigDecimal MINIMUM_DEPOSIT;

    @Transactional
    public AccountDTO createAccount(CreateAccountDTO createAccountDTO) {
        Account account = new Account();
        account.setAccountType(createAccountDTO.accountType());

        validateAccount(createAccountDTO);

        account.setUser(userRepository.findUserById(createAccountDTO.id())
                .orElseThrow(() -> new UserNotFoundException("User was not found with id: " + createAccountDTO.id())));

        Account savedAccount = accountRepository.save(account);

        return AccountDTO.fromEntity(savedAccount);
    }

    private void validateAccount(CreateAccountDTO createAccountDTO) {
        if (createAccountDTO.accountType() == AccountType.SAVING) {
            BigDecimal initialDeposit = createAccountDTO.initialDeposit();

            if (createAccountDTO.initialDeposit() == null ||
            initialDeposit.compareTo(MINIMUM_DEPOSIT) < 0) {
                throw new InitialDepositRequiredException("For a saving account initial deposit required");
            }
        }
    }

    @Transactional
    public PaginatedResponse<AccountDTO> getAccountsByUserId(UUID id, Pageable pageable) {
        if (!userRepository.userExists(id)) {
            throw new UserNotFoundException("User not found");
        }

        Page<Account> page = accountRepository.getAccountsByUserId(id, pageable);

        List<AccountDTO> accounts = page.getContent()
                .stream()
                .map(AccountDTO::fromEntity)
                .toList();

        return new PaginatedResponse<>(
                accounts,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
