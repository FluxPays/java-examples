package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * POST /paymentStatus — poll a payment you created.
 *
 * Statuses: WAIT (awaiting payment), SECURE / ACS (3-DS in progress),
 * PAID, CANCELED, REFUNDED. Prefer webhooks over polling for fulfillment —
 * see {@link WebhookServerExample}.
 */
public final class PaymentStatusExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        String orderID = args.length > 0 ? args[0] : "9fd37f19-2b3d-4424-aa90-78bdafd6ed03";
        ObjectMapper om = new ObjectMapper();

        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/paymentStatus"))
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
        System.out.println("orderID: " + payment.path("orderID").asText());
        System.out.println("status:  " + payment.path("status").asText());
        System.out.println("amount:  " + payment.path("amount").asDouble());
        System.out.println("fee:     " + payment.path("fee").asDouble());
        System.out.println("method:  " + payment.path("method").asText());
        System.out.println("demo:    " + payment.path("demo").asBoolean());
    }
}
