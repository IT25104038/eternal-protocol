package com.eternalprotocol.api.service;

import com.eternalprotocol.api.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;

/**
 * Service for integrating with the PayHere payment gateway for checkout processing
 * <p>
 * Generates an auto submitting HTML form to securely POST payment details to PayHere
 * This intermediate backend redirect  {@code GET /api/orders/{id}/checkout-redirect}
 * ensures the merchant secret and computed security hash remain strictly server side, 
 * preventing exposure to the client browser
 *
 * @see <a href="https://support.payhere.lk/api-&-mobile-sdk/payhere-checkout">PayHere Checkout Documentation</a>
 */

@Service
public class PaymentService {

    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutUrl;
    private final String frontendBaseUrl;
    private final String appBaseUrl;

    /**
     * @param merchantId  PayHere merchant account ID
     * @param merchantSecret  PayHere merchant secret
     * @param checkoutUrl  PayHere hosted checkout URL
     * @param frontendBaseUrl  used for payment return and cancellation redirects
     * @param appBaseUrl   used for webhook callbacks and the checkout redirect
     */

    public PaymentService(
            @Value("${app.payhere.merchant-id}") String merchantId,
            @Value("${app.payhere.merchant-secret}") String merchantSecret,
            @Value("${app.payhere.checkout-url}") String checkoutUrl,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.base-url}") String appBaseUrl) {
        this.merchantId = merchantId;
        this.merchantSecret = merchantSecret;
        this.checkoutUrl = checkoutUrl;
        this.frontendBaseUrl = frontendBaseUrl;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Generates the backend redirect URL for the frontend to navigate to upon order creation
     * <p>
     * Form generation, including the computation of the sensitive security hash, is deferred 
     * until this URL is accessed via {@link #buildCheckoutFormHtml}
     * @param order  newly created order
     * @param amount total amount to charge
     * @return backend redirect URL for this order
     */

    public String initiatePayment(Order order, BigDecimal amount) {
        return appBaseUrl + "/api/orders/" + order.getId() + "/checkout-redirect";
    }

    /**
     * Generates an auto-submitting HTML page to redirect the client to the PayHere checkout
     * <p>
     * Configures the {@code return_url} and {@code cancel_url} with a status query parameter 
     * strictly as a UX hint. Because clients can navigate to these URLs independently of an 
     * actual transaction, the frontend must always verify the true {@code paymentStatus} 
     * via the API. The definitive payment state is only updated securely via the 
     * signature verified webhook 
     *
     * @param order order being paid for
     * @return complete auto submitting checkout HTML string
     */

    public String buildCheckoutFormHtml(Order order) {
        String orderId = String.valueOf(order.getId());
        String amount = formatAmount(order.getTotalAmount());
        String currency = "LKR";
        String hash = computeHash(orderId, amount, currency);

        String[] nameParts = splitName(order.getCustomer().getName());

        String notifyUrl = appBaseUrl + "/api/orders/" + orderId + "/confirm-payment";
        String returnUrl = frontendBaseUrl + "/order/" + orderId + "/confirmation?status=success";
        String cancelUrl = frontendBaseUrl + "/order/" + orderId + "/confirmation?status=cancelled";

        return "<!DOCTYPE html><html><head><title>Redirecting to PayHere...</title></head><body>"
                + "<form id=\"payhereForm\" method=\"post\" action=\"" + checkoutUrl + "\">"
                + hidden("merchant_id", merchantId)
                + hidden("return_url", returnUrl)
                + hidden("cancel_url", cancelUrl)
                + hidden("notify_url", notifyUrl)
                + hidden("order_id", orderId)
                + hidden("items", escape(buildItemsSummary(order)))
                + hidden("currency", currency)
                + hidden("amount", amount)
                + hidden("hash", hash)
                + hidden("first_name", escape(nameParts[0]))
                + hidden("last_name", escape(nameParts[1]))
                + hidden("email", "customer@example.com")
                + hidden("phone", escape(order.getCustomer().getPhone()))
                + hidden("address", escape(order.getCustomer().getAddress()))
                + hidden("city", "Colombo")
                + hidden("country", "Sri Lanka")
                + "</form>"
                + "<p>Redirecting to secure payment...</p>"
                + "<script>document.getElementById('payhereForm').submit();</script>"
                + "</body></html>";
    }

    /**
     * Verifies the MD5 signature of the PayHere payment webhook
     * <p>
     * This validation is strictly required to authenticate payment success claims 
     * and prevent spoofed requests to the webhook endpoint
     *
     * @param merchantId merchant ID from the webhook payload
     * @param orderId order ID from the webhook payload
     * @param payhereAmount payment amount recorded by PayHere
     * @param payhereCurrency payment currency recorded by PayHere
     * @param statusCode    payment status code returned by PayHere
     * @param md5sig     provided MD5 signature to validate against a locally computed hash
     * @return {@code true} if the signature is valid
     */

    public boolean verifyNotifySignature(String merchantId, String orderId, String payhereAmount,
                                          String payhereCurrency, String statusCode, String md5sig) {
        String merchantSecretHash = md5Upper(merchantSecret);
        String localSig = md5Upper(merchantId + orderId + payhereAmount + payhereCurrency
                + statusCode + merchantSecretHash);
        return localSig.equalsIgnoreCase(md5sig);
    }

    /**
     * Computes the security hash required for the PayHere checkout form
     * <p>
     * This hash validates the integrity of the payment request, proving it originated 
     * from the backend and preventing client side tampering 
     *
     * @param orderId order ID
     * @param amount payment amount
     * @param currency payment currency code
     * @return computed security hash
     */

    private String computeHash(String orderId, String amount, String currency) {
        String merchantSecretHash = md5Upper(merchantSecret);
        return md5Upper(merchantId + orderId + amount + currency + merchantSecretHash);
    }

   /**
     * Computes the uppercase hex encoded MD5 hash of a string, conforming to 
     * PayHere's signature specifications 
     *
     * @param input text to hash
     * @return uppercase hex encoded MD5 hash
     */

    private String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute PayHere hash", e);
        }
    }

    /**
     * Formats a monetary amount to exactly two decimal places, conforming to 
     * PayHere specifications
     *
     * @param amount amount to format
     * @return formatted amount string
     */

    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("0.00").format(amount);
    }

    /**
     * Generates a human readable summary of the order contents for the PayHere 
     * checkout interface
     * <p>
     * The output is unstructured text, capped at 200 characters to ensure optimal 
     * display on the payment gateway page
     *
     * @param order order to summarize
     * @return formatted order summary string
     */

    private String buildItemsSummary(Order order) {
        String summary = order.getItems().stream()
                .map(item -> item.getProduct().getName() + " x" + item.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "));
        return summary.length() > 200 ? summary.substring(0, 197) + "..." : summary;
    }

    /**
     * Splits a full name into discrete first and last name components to populate 
     * PayHere's respective fields
     *
     * @param fullName full name to split
     * @return two element array containing the first name and last name
     */

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], "-"};
    }

    /**
     * Generates a hidden HTML {@code <input>} field for the PayHere checkout form
     *
     * @param name field name expected by PayHere
     * @param value field value
     * @return HTML string for the hidden input field
     */

    private String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + value + "\">";
    }

   /**
     * Escapes a value for safe inclusion within an HTML attribute
     *
     * @param value text to escape 
     * @return the escaped text or an empty string if the input is null
     */

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "&quot;");
    }
}
