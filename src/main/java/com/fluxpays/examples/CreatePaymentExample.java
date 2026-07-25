package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * POST /createPayment — create a payment and get a checkout link.
 *
 * The payer opens `link`, picks a method (unless the link is locked to one via
 * `type`) and pays. The final status arrives on your callback URL as a signed
 * webhook — see {@link WebhookServerExample}; polling is {@link PaymentStatusExample}.
 */
public final class CreatePaymentExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        ObjectMapper om = new ObjectMapper();

        // type: 0 = СБП, 1 = Карта МИР, 2 = Криптовалюта, 3 = Visa · Mastercard,
        //       4 = СберПей, "all" = the payer picks a method on the checkout page.
        // email is required — it is shown in the order details and used as the
        // debit requisite for СБП / СберПей.
        String json = om.writeValueAsString(Map.of(
                "amount", 1000,
                "type", 0,
                "email", "payer@mail.ru",
                "description", "Order #1234"));

        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/createPayment"))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body());
            return;
        }

        JsonNode payment = om.readTree(res.body());
        System.out.println("orderID: " + payment.path("orderID").asText());
        System.out.println("link:    " + payment.path("link").asText());
        System.out.println("expires: " + payment.path("expires").asLong());
    }
}
