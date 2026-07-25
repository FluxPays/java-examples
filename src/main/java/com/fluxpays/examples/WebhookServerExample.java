package com.fluxpays.examples;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Webhook receiver for FluxPays callbacks (plain JDK HttpServer — swap the
 * handler body into your Spring/Jakarta controller as needed).
 *
 * All callbacks go to the single callback URL from your dashboard settings and
 * are told apart by the `event` field:
 *
 *   paid    {event, orderID, status: "PAID", amount, fee, method, demo, sign}
 *   refund  {event, orderID, status, refundStatus, amount, demo, sign}
 *           status is the CURRENT payment status; refundStatus is
 *           PROCESSING | SUCCESS (money returned) | ERROR (rolled back, PAID again)
 *   payout  {event, payoutID, status: "SUCCESS" | "ERROR", amount, currency,
 *            destination, error?, sign}
 *
 * Verify `sign` before trusting anything (see {@link Signature}) and answer
 * HTTP 200 quickly — do heavy work after responding.
 */
public final class WebhookServerExample {
    static final String SALT = System.getenv().getOrDefault("FLUXPAYS_SALT", "YOUR_SALT");
    static final ObjectMapper OM = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/webhook", WebhookServerExample::handle);
        server.start();
        System.out.println("Listening on http://localhost:" + port + "/webhook");
    }

    static void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405);
            return;
        }
        try {
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode event = OM.readTree(raw);

            if (!Signature.verify(event, SALT)) {
                respond(exchange, 403);
                return;
            }

            switch (event.path("event").asText()) {
                case "paid" -> {
                    // Fulfill only real money — demo payments carry demo: true.
                    if (!event.path("demo").asBoolean()) {
                        fulfillOrder(event.path("orderID").asText());
                    }
                }
                case "refund" -> System.out.printf("refund %s: payment %s, refundStatus %s%n",
                        event.path("orderID").asText(),
                        event.path("status").asText(),
                        event.path("refundStatus").asText());
                case "payout" -> System.out.printf("payout #%d to %s: %s%n",
                        event.path("payoutID").asLong(),
                        event.path("destination").asText(),
                        event.path("status").asText());
                default -> System.out.println("unknown event: " + raw);
            }
            respond(exchange, 200);
        } catch (Exception e) {
            respond(exchange, 400);
        }
    }

    static void fulfillOrder(String orderID) {
        System.out.println("PAID — fulfilling order " + orderID);
    }

    static void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
