package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link CartItem} rows. Spring generates the
 * implementation of every method below automatically from its name.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Gets every cart line belonging to one customer
     *
     * @param customerId customer's ID
     * @return that customer's cart lines
     */
    List<CartItem> findByCustomerId(Long customerId);

    /**
     * Looks up one cart line by its own ID, but only if it belongs to the
     * given customer  used so a customer can never edit another
     * customer's cart line
     *
     * @param id         the unique ID of the cart item itself
     * @param customerId the ID of the customer who supposedly owns it
     * @return the matching cart line, or empty if not found or not owned by this customer
     */
    Optional<CartItem> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * Finds an existing cart line for the exact same product/size/colour
     * combination, so adding the same item twice increases quantity
     * instead of creating a duplicate row
     *
     * @param customerId the ID of the customer shopping
     * @param productId  the ID of the product they're adding
     * @param size       the selected size
     * @param colour     the selected color
     * @return the matching cart line, or empty if none exists yet
     */
    Optional<CartItem> findByCustomerIdAndProductIdAndSizeAndColour(
            Long customerId, Long productId, String size, String colour);

    /**
     * Deletes every cart line belonging to one customer
     *
     * @param customerId owning customer's ID
     */
    void deleteByCustomerId(Long customerId);

    /**
     * Cleans up any cart items linked to a specific product right before we 
     * delete that product from the system. If we didn't clear these out first, 
     * the database would yell at us with a foreign key error. Since cart items 
     * are just temporary shopping states 
     *
     * @param productId the ID of the product we're about to delete
     */
    void deleteByProductId(Long productId);
}
