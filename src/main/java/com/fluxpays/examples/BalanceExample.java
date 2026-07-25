package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GET /balance — every wallet account of the shop.
 *
 * Per account: available (credited, spendable), hold (PAID money inside the
 * 24h hold window awaiting the daily release — RUB only) and their total.
 * The top-level `balance` field mirrors RUB available for older integrations.
 */
public final class BalanceExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/balance"))
                        .header("Authorization", "Bearer " + API_KEY)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body());
            return;
        }

        JsonNode body = new ObjectMapper().readTree(res.body());
        for (JsonNode acc : body.path("accounts")) {
            System.out.printf("%-5s available %12.2f  hold %12.2f  total %12.2f%n",
                    acc.path("currency").asText(),
                    acc.path("available").asDouble(),
                    acc.path("hold").asDouble(),
                    acc.path("balance").asDouble());
        }
    }
}
