package com.eternalprotocol.api.exception;

/**
 * Thrown when someone tries to add or buy more of a product than is currently available
 * The {@link GlobalExceptionHandler} catches this and returns a 409 Conflict response
 */

public class InsufficientStockException extends RuntimeException {

    /**
     * @param message message the error message returned to the user
     */
    
    public InsufficientStockException(String message) {
        super(message);
    }
}
