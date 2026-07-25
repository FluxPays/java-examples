package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GET /rates — RUB per 1 unit of every supported currency.
 *
 * Central Bank of Russia source, cached for 1 hour on the API side;
 * USDT mirrors the USD rate the wallet uses for Tether operations.
 */
public final class RatesExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/rates"))
                        .header("Authorization", "Bearer " + API_KEY)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body());
            return;
        }

        JsonNode rates = new ObjectMapper().readTree(res.body());
        for (String code : new String[] { "USD", "EUR", "KZT", "USDT" }) {
            System.out.printf("%-4s = %.2f RUB%n", code, rates.path(code).asDouble());
        }
    }
}
