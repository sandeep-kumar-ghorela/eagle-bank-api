package com.eaglebank.service.impl;

import com.eaglebank.entity.AddressEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.UserNotFoundException;
import com.eaglebank.model.AddressResponse;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.UserResponse;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.eaglebank.constants.EagleBankApiConstants.NOT_ALLOWED_TO_ACCESS;
import static com.eaglebank.constants.EagleBankApiConstants.ROLE_USER;
import static com.eaglebank.constants.EagleBankApiConstants.USER_NOT_FOUND;

/**
 * Service class to handle user-related operations such as creating a new user
 * and fetching user details by user ID.
 *
 * It manages user data persistence, access control to user information,
 * and assembling response DTOs.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static AtomicInteger counter = new AtomicInteger(1);

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user along with their address details.
     *
     * @param request the request object containing user data to be created
     * @return a UserResponse containing created user details
     */
    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        final UserEntity user = populateUserEntity(request);
        user.setAddress(populateAddressEntity(request));
        UserEntity savedUser = userRepository.save(user);
        return costructUserResponse(savedUser);
    }

    /**
     * Fetches a user's details by their unique user ID.
     * An apply validation
     *
     * @param userId the unique user ID
     * @return UserResponse containing user details
     */
    @Override
    public UserResponse fetchUserByID(final String userId) {
        final UserEntity user = validateAndGetAccount(userId);
        return costructUserResponse(user);
    }

    /**
     * Finds a user by ID and checks if the current user is allowed to access it and account exists
     *
     * @param userId the unique user ID
     * @return UserResponse containing user details
     * @throws UserNotFoundException if user with the given ID does not exist
     * @throws AccessDeniedException if the logged-in user tries to access another user's data
     */
    private UserEntity validateAndGetAccount(final String userId) {
        final Optional<UserEntity> user = userRepository.findByUserId(userId);
        if (!user.isPresent()) {
            throw new UserNotFoundException(USER_NOT_FOUND + userId);
        }
        else {
            shouldNotAccessOtherUser(userId);
        }
        return user.get();
    }

    /**
     * Checks if the logged-in user is allowed to access the given user's information.
     * Throws AccessDeniedException if they attempt to access another user's data.
     *
     * @param userId the user ID being accessed
     * @throws AccessDeniedException if unauthorized access is detected
     */
    private void shouldNotAccessOtherUser(final String userId) {
        String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        final Optional<UserEntity> user2 = userRepository.findByEmail(loggedInUsername);
        if (!user2.get().getUserId().equals(userId)) {
            throw new AccessDeniedException(NOT_ALLOWED_TO_ACCESS);
        }
    }

    /**
     * Maps CreateUserRequest data to a new UserEntity, sets timestamps, ID, role, and encodes password.
     *
     * @param request the user creation request
     * @return populated UserEntity
     */
    private UserEntity populateUserEntity(final CreateUserRequest request) {
        final String createdTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        final String updatedTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        final UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setCreatedTimestamp(createdTimestamp);
        user.setUpdatedTimestamp(updatedTimestamp);
        user.setUserId(generateUserId());
        user.setPassword(getEncodedPassword(request.getPassword()));
        user.setRole(ROLE_USER);
        return user;
    }

    /**
     * Maps address information from the request to a new AddressEntity.
     *
     * @param request the user creation request containing address info
     * @return populated AddressEntity
     */
    private AddressEntity populateAddressEntity(final CreateUserRequest request) {
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setLine1(request.getAddress().getLine1());
        addressEntity.setLine2(request.getAddress().getLine2());
        addressEntity.setLine3(request.getAddress().getLine3());
        addressEntity.setTown(request.getAddress().getTown());
        addressEntity.setCounty(request.getAddress().getCounty());
        addressEntity.setPostCode(request.getAddress().getPostCode());
        return addressEntity;
    }


    /**
     * Constructs a UserResponse DTO from a UserEntity.
     *
     * @param savedUser the user entity from the database
     * @return UserResponse with user data for API responses
     */
    private UserResponse costructUserResponse(final UserEntity savedUser) {
        final UserResponse response = UserResponse.builder()
                .address(constructAddressResponse(savedUser))
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .createdTimestamp(savedUser.getCreatedTimestamp())
                .updatedTimestamp(savedUser.getUpdatedTimestamp())
                .build();
        return response;
    }

    /**
     * Constructs an AddressResponse DTO from the AddressEntity inside UserEntity.
     *
     * @param savedUser the user entity containing the address
     * @return AddressResponse with address details
     */
    private AddressResponse constructAddressResponse(final UserEntity savedUser) {
        final AddressResponse addressResponse = AddressResponse.builder()
                .line1(savedUser.getAddress().getLine1())
                .line2(savedUser.getAddress().getLine2())
                .line3(savedUser.getAddress().getLine3())
                .town(savedUser.getAddress().getTown())
                .postcode(savedUser.getAddress().getPostCode())
                .county(savedUser.getAddress().getCounty())
                .build();
        return addressResponse;
    }

    /**
     * Generates a unique user ID prefixed with "usr-".
     *
     * @return generated unique user ID
     */
    private String generateUserId() {
        return String.format("usr-%02d", counter.getAndIncrement());
    }


    /**
     * Encodes the plain text password using BCrypt hashing.
     *
     * @param password the plain password string
     * @return encoded (hashed) password string
     */
    private String getEncodedPassword(final String password) {
        var passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

}
