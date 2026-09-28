package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The request used when a customer updates their delivery details
 * <p>
 * This intentionally excludes the email and password. Updating login credentials 
 * requires extra security checks , so those are handled separately
 *
 * @param name    updated name
 * @param phone   updated contact number
 * @param address updated delivery address
 */

public record UpdateCustomerProfileRequestDto(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Address is required") String address
) {}
