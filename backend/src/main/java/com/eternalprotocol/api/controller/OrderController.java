package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.OrderDetailDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.security.AppUserDetails;
import com.eternalprotocol.api.service.OrderService;
import com.eternalprotocol.api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for order processing and PayHere payments
 * <p>
 * Supports both guest and authenticated checkouts
 */

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * @param orderService   the order service
     * @param paymentService the PayHere payment verification service
     */
    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Places a new order
     * <p>
     * Supports both guest and authenticated checkouts
     *
     * @param request   the order request details
     * @param authentication the optional authentication details
     * @return the order creation response containing payment redirection details
     */

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request,
                                                          Authentication authentication) {
        Long loggedInCustomerId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails user
                && "CUSTOMER".equals(user.getRole())) {
            loggedInCustomerId = user.getId();
        }
        return ResponseEntity.ok(orderService.createOrder(request, loggedInCustomerId));
    }

    /**
     * Retrieves the details of an order
     *
     * @param id the order ID
     * @return the order details
     */

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    /**
     * Generates an auto submitting HTML page for PayHere payment redirection
     *
     * @param id the order ID
     * @return the HTML redirection page
     */

    @GetMapping(value = "/{id}/checkout-redirect", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutRedirect(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.buildCheckoutRedirectHtml(id));
    }

    /**
     * Webhook endpoint for PayHere payment notifications
     * <p>
     * Verifies the MD5 signature to authenticate the callback before processing the payment
     *
     * @param id     order ID
     * @param merchantId  PayHere merchant ID
     * @param orderId   internal order reference
     * @param payhereAmount payment amount
     * @param payhereCurrency payment currency
     * @param statusCode the payment status code
     * @param md5sig   the MD5 signature for verification
     * @return an empty response acknowledging receipt
     */

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Void> confirmPayment(
            @PathVariable Long id,
            @RequestParam("merchant_id") String merchantId,
            @RequestParam("order_id") String orderId,
            @RequestParam("payhere_amount") String payhereAmount,
            @RequestParam("payhere_currency") String payhereCurrency,
            @RequestParam("status_code") String statusCode,
            @RequestParam("md5sig") String md5sig) {

        boolean validSignature = paymentService.verifyNotifySignature(
                merchantId, orderId, payhereAmount, payhereCurrency, statusCode, md5sig);


        if (validSignature && "2".equals(statusCode)) {
            orderService.confirmPayment(id);
        }

        return ResponseEntity.ok().build();
    }
}
