# FluxPays API — Java examples

Runnable examples for the FluxPays merchant server-to-server API.
Full reference: [fluxpays.org/docs](https://fluxpays.org/docs).

Requests are authenticated with `Authorization: Bearer {token}` (the API key from
your dashboard settings). API responses are plain JSON; signing lives on
**callbacks only** — every webhook body carries a `sign` field you must verify.

## Requirements

- JDK 17+
- Maven 3.8+
- The only dependency is `jackson-databind` (JSON); HTTP is the JDK's built-in client.

## Setup

| Env var            | Meaning                                        | Default                    |
| ------------------ | ---------------------------------------------- | -------------------------- |
| `FLUXPAYS_API_KEY` | API key (dashboard → settings)                 | —                          |
| `FLUXPAYS_SALT`    | Webhook signature salt (dashboard → settings)  | —                          |
| `FLUXPAYS_API_URL` | API base URL override                          | `https://api.fluxpays.org` |

## Run

```bash
mvn -q compile exec:java -Dexec.mainClass=com.fluxpays.examples.CreatePaymentExample
```

| Example                     | Endpoint              | What it shows                                          |
| --------------------------- | --------------------- | ------------------------------------------------------ |
| `CreatePaymentExample`      | `POST /createPayment` | Create a payment, get the checkout link                |
| `PaymentStatusExample`      | `POST /paymentStatus` | Poll a payment (pass `orderID` as the first argument)  |
| `RefundPaymentExample`      | `POST /refundPayment` | Refund a PAID payment (idempotent)                     |
| `BalanceExample`            | `GET /balance`        | Wallet accounts: available / hold / total              |
| `RatesExample`              | `GET /rates`          | RUB rates (CBR, cached 1h; USDT mirrors USD)           |
| `SbpBanksExample`           | `GET /sbpBanks`       | СБП banks with numeric NSPK ids                        |
| `WebhookServerExample`      | your callback URL     | Receive + verify signed webhooks (paid/refund/payout)  |
| `Signature`                 | —                     | Signature self-test against the docs control vector    |

Pass program arguments with `-Dexec.args`, e.g.:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.fluxpays.examples.PaymentStatusExample -Dexec.args=9fd37f19-2b3d-4424-aa90-78bdafd6ed03
```

## Payment `type`

| `type` | Method             |
| ------ | ------------------ |
| `0`    | СБП                |
| `1`    | Карта МИР          |
| `2`    | Криптовалюта       |
| `3`    | Visa · Mastercard  |
| `4`    | СберПей            |
| `"all"`| Payer picks a method on the checkout page |

Payment statuses: `WAIT` → (`SECURE` / `ACS` during 3-DS) → `PAID` | `CANCELED`,
and `REFUNDED` after a refund.

## Webhooks

All callbacks are POSTed to the single callback URL from your dashboard
settings and are told apart by the `event` field: `paid`, `refund`, `payout`
(payload shapes are documented in `WebhookServerExample`). Answer HTTP 200
quickly; do heavy work after responding. Demo payments carry `demo: true` —
never fulfill them.

### Signature

Every webhook body carries a top-level `sign`:

```
canonical = payload as compact JSON, TOP-LEVEL keys sorted alphabetically,
            sign excluded, unicode/slashes unescaped
sign      = HMAC-SHA256_hex(canonical, SALT)
```

Only the **top level** is sorted — do not enable Jackson's
`ORDER_MAP_ENTRIES_BY_KEYS` / `SORT_PROPERTIES_ALPHABETICALLY`, they also sort
nested objects and produce a different sign. Compare with a constant-time
equality check (`MessageDigest.isEqual`), as `Signature.verify` does.

Control vector: payload `{"amount":1000,"email":"payer@mail.ru","type":0}`
with salt `example` →
`2a450c52c29d425f834e8c395bcf36fdbcd17a799fe0d9b47f47ea5267eed8a6`.
Check your environment with:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.fluxpays.examples.Signature
```
