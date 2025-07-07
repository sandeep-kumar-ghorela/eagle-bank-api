package com.eaglebank.api.service;

import com.eaglebank.entity.AddressEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.UserNotFoundException;
import com.eaglebank.model.Address;
import com.eaglebank.model.AddressResponse;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.UserResponse;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static com.eaglebank.constants.EagleBankApiConstants.NOT_ALLOWED_TO_ACCESS;
import static com.eaglebank.constants.EagleBankApiConstants.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup SecurityContextHolder mock for tests
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void testCreateUser_Success() {
        // Prepare input
        Address addressRequest = Address.builder()
                .county("Surrey")
                .town("Redhill")
                .line1("123 Main St")
                .line2("lin1")
                .line3("top floor")
                .postCode("ZIP123")
                .build();

        CreateUserRequest request = new CreateUserRequest();
        request.setName("User User");
        request.setEmail("test_user@example.com");
        request.setPhoneNumber("1234567890");
        request.setPassword("password");
        request.setAddress(addressRequest);

        // Mock repository save
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertEquals("User User", savedUser.getName());
        assertEquals("test_user@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getUserId());
        assertNotNull(savedUser.getPassword()); // password should be encoded

        AddressEntity savedAddress = savedUser.getAddress();
        assertEquals("123 Main St", savedAddress.getLine1());
        assertEquals("lin1", savedAddress.getLine2());
        assertEquals("top floor", savedAddress.getLine3());
        assertEquals("Redhill", savedAddress.getTown());
        assertEquals("Surrey", savedAddress.getCounty());
        assertEquals("ZIP123", savedAddress.getPostCode());

        // Assert response fields
        assertEquals(savedUser.getUserId(), response.getUserId());
        assertEquals("User User", response.getName());
        assertEquals("test_user@example.com", response.getEmail());
        assertEquals("1234567890", response.getPhoneNumber());

        AddressResponse addrResp = response.getAddress();
        assertEquals("123 Main St", addrResp.getLine1());
        assertEquals("lin1", addrResp.getLine2());
        assertEquals("top floor", addrResp.getLine3());
        assertEquals("Redhill", addrResp.getTown());
        assertEquals("Surrey", addrResp.getCounty());
        assertEquals("ZIP123", addrResp.getPostcode());
    }

    @Test
    void testFetchUserByID_Success() {
        String userId = "usr-01";
        String loggedInUserEmail = "loggedin@example.com";

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setEmail(loggedInUserEmail);
        userEntity.setName("Jane");
        userEntity.setPhoneNumber("9876543210");

        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setLine1("Addr1");
        addressEntity.setLine2("Addr2");
        addressEntity.setLine3("Addr3");
        addressEntity.setTown("Town");
        addressEntity.setCounty("County");
        addressEntity.setPostCode("PC123");
        userEntity.setAddress(addressEntity);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(userEntity));
        when(authentication.getName()).thenReturn(loggedInUserEmail);
        when(userRepository.findByEmail(loggedInUserEmail)).thenReturn(Optional.of(userEntity));

        UserResponse response = userService.fetchUserByID(userId);

        assertEquals(userId, response.getUserId());
        assertEquals("Jane", response.getName());
        assertEquals("9876543210", response.getPhoneNumber());
        assertEquals("Addr1", response.getAddress().getLine1());
    }

    @Test
    void testFetchUserByID_UserNotFound() {
        String userId = "usr-99";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                                                () -> userService.fetchUserByID(userId));

        assertTrue(ex.getMessage().contains(USER_NOT_FOUND));
    }

    @Test
    void testFetchUserByID_AccessDenied() {
        String userId = "usr-01";
        String loggedInUserEmail = "loggedin@example.com";

        UserEntity requestedUser = new UserEntity();
        requestedUser.setUserId(userId);
        requestedUser.setEmail("someone@example.com");

        UserEntity loggedInUser = new UserEntity();
        loggedInUser.setUserId("usr-02");
        loggedInUser.setEmail(loggedInUserEmail);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(requestedUser));
        when(authentication.getName()).thenReturn(loggedInUserEmail);
        when(userRepository.findByEmail(loggedInUserEmail)).thenReturn(Optional.of(loggedInUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                                                () -> userService.fetchUserByID(userId));

        assertEquals(NOT_ALLOWED_TO_ACCESS, ex.getMessage());
    }
}
