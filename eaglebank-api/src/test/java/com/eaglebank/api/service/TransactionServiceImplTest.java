package com.eaglebank.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eaglebank.entity.BankAccountEntity;
import com.eaglebank.entity.TransactionEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.EntityNotFountException;
import com.eaglebank.exception.InSufficientBalanceException;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.TransactionResponse;
import com.eaglebank.model.enums.CurrencyType;
import com.eaglebank.model.enums.TransactionType;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionServiceImpl service;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private static final String LOGGED_IN_EMAIL = "user@example.com";
    private static final String AMOUNT_50 = "50.00";

    private BankAccountEntity accountEntity;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(LOGGED_IN_EMAIL);

        accountEntity = new BankAccountEntity();
        accountEntity.setAccountNumber("01100001");
        accountEntity.setBalance(new BigDecimal("100.00"));
        accountEntity.setCurrency(CurrencyType.GBP.toString());

        userEntity = new UserEntity();
        userEntity.setEmail(LOGGED_IN_EMAIL);
        userEntity.setAccounts(Collections.singletonList(accountEntity));
    }

    @Test
    void createTransaction_Deposit_Success() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType(TransactionType.DEPOSIT.getTransactionType());
        request.setAmount(new BigDecimal(AMOUNT_50));
        request.setCurrency(CurrencyType.GBP.toString());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(userEntity));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = service.createTransaction(accountEntity.getAccountNumber(), request);

        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT.getTransactionType(), response.getType());
        assertEquals(new BigDecimal(AMOUNT_50).setScale(2), response.getAmount());
        assertEquals(accountEntity.getCurrency(), response.getCurrency());

        // Balance should be updated
        assertEquals(new BigDecimal("150.00"), accountEntity.getBalance());
    }

    @Test
    void createTransaction_Withdraw_Success() {
        CreateTransactionRequest request =CreateTransactionRequest.builder().build();
        request.setType(TransactionType.WITHDRAW.getTransactionType());
        request.setAmount(new BigDecimal("40.00"));
        request.setCurrency(CurrencyType.GBP.toString());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(userEntity));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = service.createTransaction(accountEntity.getAccountNumber(), request);

        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAW.getTransactionType(), response.getType());
        assertEquals(new BigDecimal("40.00").setScale(2), response.getAmount());
        assertEquals(accountEntity.getCurrency(), response.getCurrency());

        // Balance should be reduced
        assertEquals(new BigDecimal("60.00"), accountEntity.getBalance());
    }

    @Test
    void createTransaction_Withdraw_InsufficientBalance_Throws() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType(TransactionType.WITHDRAW.getTransactionType());
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency(CurrencyType.GBP.toString());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(userEntity));

        InSufficientBalanceException ex = assertThrows(InSufficientBalanceException.class, () -> {
            service.createTransaction(accountEntity.getAccountNumber(), request);
        });

        assertTrue(ex.getMessage().contains("Insufficient funds to process transaction"));
    }

    @Test
    void createTransaction_UnsupportedCurrency_Throws() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType(TransactionType.DEPOSIT.getTransactionType());
        request.setAmount(new BigDecimal(AMOUNT_50));
        request.setCurrency("XYZ");

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {
            service.createTransaction(accountEntity.getAccountNumber(), request);
        });

        assertTrue(ex.getMessage().contains("Only supported currency types"));
    }

    @Test
    void createTransaction_UnsupportedTransactionType_Throws() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType("INVALID_TYPE");
        request.setAmount(new BigDecimal(AMOUNT_50));
        request.setCurrency(CurrencyType.GBP.toString());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(userEntity));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {
            service.createTransaction(accountEntity.getAccountNumber(), request);
        });

        assertTrue(ex.getMessage().contains("Only supported transaction types"));
    }

    @Test
    void createTransaction_AccountNotFound_Throws() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType(TransactionType.DEPOSIT.getTransactionType());
        request.setAmount(new BigDecimal(AMOUNT_50));
        request.setCurrency(CurrencyType.GBP.toString());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.empty());

        EntityNotFountException ex = assertThrows(EntityNotFountException.class, () -> {
            service.createTransaction(accountEntity.getAccountNumber(), request);
        });

        assertTrue(ex.getMessage().contains("Account was not found"));
    }

    @Test
    void createTransaction_AccessDenied_Throws() {
        CreateTransactionRequest request = CreateTransactionRequest.builder().build();
        request.setType(TransactionType.DEPOSIT.getTransactionType());
        request.setAmount(new BigDecimal(AMOUNT_50));
        request.setCurrency(CurrencyType.GBP.toString());

        // User does NOT own this account (empty accounts list)
        UserEntity otherUser = new UserEntity();
        otherUser.setEmail(LOGGED_IN_EMAIL);
        otherUser.setAccounts(Collections.emptyList());

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(otherUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.createTransaction(accountEntity.getAccountNumber(), request);
        });

        assertEquals("Not allowed to access another user's data", ex.getMessage());
    }

    @Test
    void fetchTransactionById_Success() {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("tan-01");
        entity.setBankAccount(accountEntity);
        entity.setAmount(new BigDecimal(AMOUNT_50));
        entity.setCurrency(CurrencyType.GBP.toString());
        entity.setType(TransactionType.DEPOSIT.getTransactionType());
        entity.setCreatedTimestamp("2025-07-05T12:00:00Z");

        UserEntity otherUser = new UserEntity();
        otherUser.setEmail(LOGGED_IN_EMAIL);
        otherUser.setAccounts(List.of(accountEntity));

        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(otherUser));
        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(transactionRepository.findByTransactionId("tan-01")).thenReturn(Optional.of(entity));

        TransactionResponse response = service.fetchTransactionById(accountEntity.getAccountNumber(), "tan-01");

        assertNotNull(response);
        assertEquals("tan-01", response.getTransactionId());
        assertEquals(TransactionType.DEPOSIT.getTransactionType(), response.getType());
        assertEquals(new BigDecimal(AMOUNT_50), response.getAmount());
        assertEquals(CurrencyType.GBP.toString(), response.getCurrency());
        assertEquals("2025-07-05T12:00:00Z", response.getCreatedTimestamp());
    }

    @Test
    void fetchTransactionById_TransactionNotFound_Throws() {
        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(transactionRepository.findByTransactionId("unknown")).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.fetchTransactionById(accountEntity.getAccountNumber(), "unknown");
        });

        assertTrue(ex.getMessage().contains("Not allowed to access another user's data"));
    }

    @Test
    void fetchTransactionById_TransactionNotForAccount_Throws() {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId("tan-01");
        entity.setBankAccount(new BankAccountEntity()); // Different account

        UserEntity otherUser = new UserEntity();
        otherUser.setEmail(LOGGED_IN_EMAIL);
        otherUser.setAccounts(List.of(accountEntity));

        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(otherUser));
        //when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));

        when(bankAccountRepository.findByAccountNumber(accountEntity.getAccountNumber())).thenReturn(Optional.of(accountEntity));
        when(transactionRepository.findByTransactionId("tan-01")).thenReturn(Optional.of(entity));

        EntityNotFountException ex = assertThrows(EntityNotFountException.class, () -> {
            service.fetchTransactionById(accountEntity.getAccountNumber(), "tan-09");
        });

        assertTrue(ex.getMessage().contains("Transaction was not found"));
    }
}
