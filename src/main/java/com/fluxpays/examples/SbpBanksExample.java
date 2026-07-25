package com.fluxpays.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GET /sbpBanks — СБП member banks accepted as payout destinations.
 *
 * Sourced live from the NSPK directory and cached on the API side.
 * `id` is the official numeric NSPK member id (e.g. Т-Банк = 100000000004,
 * Сбербанк = 100000000111) — the bank_code payout APIs expect.
 */
public final class SbpBanksExample {
    static final String BASE_URL = System.getenv().getOrDefault("FLUXPAYS_API_URL", "https://api.fluxpays.org");
    static final String API_KEY = System.getenv().getOrDefault("FLUXPAYS_API_KEY", "YOUR_API_KEY");

    public static void main(String[] args) throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/sbpBanks"))
                        .header("Authorization", "Bearer " + API_KEY)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body());
            return;
        }

        JsonNode banks = new ObjectMapper().readTree(res.body()).path("banks");
        System.out.println(banks.size() + " banks, first 10:");
        for (int i = 0; i < Math.min(10, banks.size()); i++) {
            JsonNode bank = banks.get(i);
            System.out.printf("%d  %s%n", bank.path("id").asLong(), bank.path("name").asText());
        }
    }
}
