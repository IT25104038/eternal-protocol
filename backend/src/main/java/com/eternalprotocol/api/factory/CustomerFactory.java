package com.eternalprotocol.api.factory;

import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.springframework.stereotype.Component;

/**
 * Factory for resolving the {@link Customer} for a new order
 * <p>
 * Handles customer resolution for both authenticated and guest checkouts, 
 * abstracting this logic from the order creation process
 */

@Component
public class CustomerFactory {

    private final CustomerRepository customerRepository;

    /**
    * @param customerRepository the customer repository
    */
    
    public CustomerFactory(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

   /**
     * Resolves the {@link Customer} for an order checkout
     *
     * @param loggedInCustomerId ID of the authenticated customer
     * @param request   order request details
     * @return resolved customer
     * @throws ResourceNotFoundException if the authenticated customer is not found
     */

    public Customer resolveForCheckout(Long loggedInCustomerId, OrderRequestDto request) {
        if (loggedInCustomerId != null) {
            return updateExistingCustomer(loggedInCustomerId, request);
        }
        return createGuestCustomer(request);
    }

    /**
     * Updates an existing customer's delivery details using the checkout request
     *
     * @param customerId customer ID
     * @param request order request details
     * @return updated customer
     * @throws ResourceNotFoundException if the customer is not found
     */
    
    private Customer updateExistingCustomer(Long customerId, OrderRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        
        customer.setName(request.customerName());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return customerRepository.save(customer);
    }

    /**
     * Creates a new customer for a guest checkout
     *
     * @param request order request details
     * @return created customer
     */

    private Customer createGuestCustomer(OrderRequestDto request) {
        return customerRepository.save(
                new Customer(request.customerName(), request.phone(), request.address()));
    }
}
