package com.fluxpays.examples;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * FluxPays webhook signature.
 *
 * Every callback body carries a `sign` field:
 *   canonical = the payload as compact JSON with TOP-LEVEL keys sorted
 *               alphabetically, `sign` excluded (unicode and slashes unescaped —
 *               Jackson's default, same as JS JSON.stringify);
 *   sign      = HMAC-SHA256_hex(canonical, SALT).
 *
 * Only the top level is sorted. Do NOT enable ORDER_MAP_ENTRIES_BY_KEYS or
 * SORT_PROPERTIES_ALPHABETICALLY — they also sort nested objects and break
 * the signature. Nested values keep the order they arrived in, which this
 * class preserves by copying the parsed JsonNode values as-is.
 */
public final class Signature {
    private static final ObjectMapper OM = new ObjectMapper();

    private Signature() {}

    /** Compact JSON with top-level keys sorted, `sign` excluded. */
    public static String canonical(JsonNode payload) throws Exception {
        List<String> names = new ArrayList<>();
        payload.fieldNames().forEachRemaining(names::add);
        Collections.sort(names);
        ObjectNode sorted = OM.createObjectNode();
        for (String name : names) {
            if (!name.equals("sign")) sorted.set(name, payload.get(name));
        }
        return OM.writeValueAsString(sorted);
    }

    public static String hmacHex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Verify the `sign` field of a parsed webhook payload (constant-time compare). */
    public static boolean verify(JsonNode payload, String salt) throws Exception {
        String sign = payload.path("sign").asText("");
        String expected = hmacHex(canonical(payload), salt);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8));
    }

    /** Self-test against the control vector published in the docs. */
    public static void main(String[] args) throws Exception {
        ObjectNode payload = OM.createObjectNode();
        payload.put("email", "payer@mail.ru");
        payload.put("type", 0);
        payload.put("amount", 1000);

        String canonical = canonical(payload);
        String sign = hmacHex(canonical, "example");
        System.out.println("canonical: " + canonical);
        System.out.println("sign:      " + sign);

        String expected = "2a450c52c29d425f834e8c395bcf36fdbcd17a799fe0d9b47f47ea5267eed8a6";
        System.out.println(sign.equals(expected)
                ? "OK — matches the docs control vector"
                : "MISMATCH — expected " + expected);
    }
}
