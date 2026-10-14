package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.AuthResponseDto;
import com.eternalprotocol.api.dto.CustomerProfileDto;
import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.InvalidCredentialsException;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.security.AppUserDetails;
import com.eternalprotocol.api.security.CustomUserDetailsService;
import com.eternalprotocol.api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for registering, logging in and editing a customer profile.
 *
 * The database, password encoder and token maker are all "mocks" (fakes),
 * so each test only checks the logic inside AuthService / CustomerService.
 */
@ExtendWith(MockitoExtension.class)
class AuthAndCustomerAccountTest {

    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AuthService authService;
    @InjectMocks
    private CustomerService customerService;

    @Test
    void register_withNewEmail_savesCustomerWithHashedPasswordAndReturnsToken() {
        // Given: an email nobody has used yet
        RegisterRequestDto request = new RegisterRequestDto(
                "Nimal", "nimal@test.com", "secret123", "0771234567", "Colombo");
        when(customerRepository.existsByEmail("nimal@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(customerRepository.save(any(Customer.class))).thenAnswer(call -> {
            Customer saved = call.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtUtil.generateToken("nimal@test.com", "CUSTOMER", 1L)).thenReturn("fake-token");

        // When
        AuthResponseDto result = authService.register(request);

        // Then: we get a customer token back...
        assertEquals("fake-token", result.token());
        assertEquals("CUSTOMER", result.role());
        assertEquals(1L, result.userId());

        // ...and the password was stored hashed, never as plain text
        ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(savedCustomer.capture());
        assertEquals("HASHED", savedCustomer.getValue().getPasswordHash());
    }

    @Test
    void register_withEmailAlreadyUsed_throwsAndSavesNothing() {
        // Given: this email already has an account
        RegisterRequestDto request = new RegisterRequestDto(
                "Nimal", "nimal@test.com", "secret123", "0771234567", "Colombo");
        when(customerRepository.existsByEmail("nimal@test.com")).thenReturn(true);

        // When / Then
        assertThrows(EmailAlreadyInUseException.class, () -> authService.register(request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void login_withCorrectPassword_returnsToken() {
        // Given: an enabled customer account and a matching password
        AppUserDetails user = new AppUserDetails(7L, "nimal@test.com", "HASHED", "CUSTOMER", true);
        when(userDetailsService.loadUserByUsername("nimal@test.com")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);
        when(jwtUtil.generateToken("nimal@test.com", "CUSTOMER", 7L)).thenReturn("fake-token");

        // When
        AuthResponseDto result = authService.login(new LoginRequestDto("nimal@test.com", "secret123"));

        // Then
        assertEquals("fake-token", result.token());
        assertEquals("CUSTOMER", result.role());
        assertEquals(7L, result.userId());
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        // Given: the password does NOT match the stored hash
        AppUserDetails user = new AppUserDetails(7L, "nimal@test.com", "HASHED", "CUSTOMER", true);
        when(userDetailsService.loadUserByUsername("nimal@test.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "HASHED")).thenReturn(false);

        // When / Then
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("nimal@test.com", "wrong-password")));
    }

    @Test
    void updateProfile_changesNameAddressAndPhone() {
        // Given: an existing customer with old details
        Customer customer = new Customer("Old Name", "0770000000", "Old Address");
        customer.setId(1L);
        customer.setEmail("nimal@test.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        // When
        CustomerProfileDto result = customerService.updateProfile(
                1L, new UpdateCustomerProfileRequestDto("New Name", "0771111111", "New Address"));

        // Then: the new details are returned, and the email is untouched
        assertEquals("New Name", result.name());
        assertEquals("0771111111", result.phone());
        assertEquals("New Address", result.address());
        assertEquals("nimal@test.com", result.email());
    }
}
