package com.eaglebank.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eaglebank.entity.BankAccountEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.EntityNotFountException;
import com.eaglebank.exception.UserNotFoundException;
import com.eaglebank.model.BankAccountResponse;
import com.eaglebank.model.CreateBankAccountRequest;
import com.eaglebank.model.enums.AccountType;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.impl.BankAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BankAccountServiceImpl service;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private static final String LOGGED_IN_EMAIL = "user@example.com";
    private static final String ACCOUNT_NUMBER = "01100000";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(LOGGED_IN_EMAIL);
    }

    @Test
    void createAccount_Success() {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .build();
        request.setAccountType(AccountType.SAVING.getAccountType());
        request.setName("My Account");

        UserEntity user = new UserEntity();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setAccounts(new ArrayList<>());

        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(user));

        ArgumentCaptor<BankAccountEntity> captor = ArgumentCaptor.forClass(BankAccountEntity.class);

        when(accountRepository.save(any(BankAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = service.createAccount(request);

        assertNotNull(response);
        assertEquals(request.getAccountType(), response.getAccountType());
        assertEquals(request.getName(), response.getName());
        assertEquals(new BigDecimal("500.00"), response.getBalance()); // Assuming ACCOUNT_INITIAL_BALANCE is 100.0

        verify(accountRepository).save(captor.capture());
        BankAccountEntity savedEntity = captor.getValue();
        assertEquals(LOGGED_IN_EMAIL, savedEntity.getUser().getEmail());
        assertNotNull(savedEntity.getAccountNumber());
    }

    @Test
    void createAccount_UnsupportedAccountType_Throws() {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .build();
        request.setAccountType("invalid-type");
        request.setName("My Account");

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {
            service.createAccount(request);
        });

        assertTrue(ex.getMessage().contains("Only supported account types"));
    }

    @Test
    void fetchAccountByAccountNumber_Success() {
        BankAccountEntity account = new BankAccountEntity();
        account.setAccountNumber(ACCOUNT_NUMBER);

        UserEntity user = new UserEntity();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setAccounts(Collections.singletonList(account));

        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(user));

        BankAccountResponse response = service.fetchAccountByAccountNumber(ACCOUNT_NUMBER);

        assertNotNull(response);
        assertEquals(ACCOUNT_NUMBER, response.getAccountNumber());
    }

    @Test
    void fetchAccountByAccountNumber_AccountNotFound_Throws() {
        when(accountRepository.findByAccountNumber("nonexistent")).thenReturn(Optional.empty());

        EntityNotFountException ex = assertThrows(EntityNotFountException.class, () -> {
            service.fetchAccountByAccountNumber("nonexistent");
        });

        assertTrue(ex.getMessage().contains("Account was not found"));
    }

    @Test
    void fetchAccountByAccountNumber_AccessDenied_Throws() {
        BankAccountEntity account = new BankAccountEntity();
        account.setAccountNumber(ACCOUNT_NUMBER);

        UserEntity user = new UserEntity();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setAccounts(Collections.emptyList()); // User does not own this account

        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.fetchAccountByAccountNumber(ACCOUNT_NUMBER);
        });

        assertEquals("Not allowed to access another user's data", ex.getMessage());
    }

    @Test
    void populateEntity_UserNotFound_Throws() {
        when(userRepository.findByEmail(LOGGED_IN_EMAIL)).thenReturn(Optional.empty());

        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .build();
        request.setAccountType(AccountType.SAVING.getAccountType());
        request.setName("My Account");

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> {
            service.createAccount(request);
        });

        assertTrue(ex.getMessage().contains("User was not found"));
    }
}
