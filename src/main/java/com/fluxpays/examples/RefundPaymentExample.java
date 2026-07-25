package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * POST /refundPayment — refund a PAID payment in full.
 *
 * Idempotent: refunding an already-refunded payment returns the same result.
 * The response flips the payment to REFUNDED with refundStatus PROCESSING or
 * SUCCESS; the verifiable confirmation is the signed {event: "refund"} webhook
 * (refundStatus SUCCESS — money returned, ERROR — rolled back, payment is PAID
 * again). See {@link WebhookServerExample}.
 */
public final class RefundPaymentExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        String orderID = args.length > 0 ? args[0] : "9fd37f19-2b3d-4424-aa90-78bdafd6ed03";
        ObjectMapper om = new ObjectMapper();

        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/refundPayment"))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                om.writeValueAsString(Map.of("orderID", orderID))))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body());
            return;
        }

        JsonNode payment = om.readTree(res.body()).path("payment");
        System.out.println("orderID:      " + payment.path("orderID").asText());
        System.out.println("status:       " + payment.path("status").asText());
        System.out.println("refundStatus: " + payment.path("refundStatus").asText());
        System.out.println("amount:       " + payment.path("amount").asDouble());
    }
}
