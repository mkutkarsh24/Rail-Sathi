# Connecting a railway data provider

RailSathi does **not** call any third-party railway API directly. It calls a small data service
that you run (or have access to), using the contract below. Your service adapts a licensed
provider to this contract and keeps the provider's secret key on the server, where it cannot be
extracted from the APK.

```
RailSathi app ──HTTPS──▶ your data service ──(secret key)──▶ licensed railway data provider
```

Set the service's base URL in `local.properties`:

```properties
RAIL_API_BASE_URL=https://railsathi-api.example.com
```

Blank (or any non-`https://` value) keeps the app in labelled sample-data mode.

## Why a backend and not a direct provider call

* Providers issue **secret** keys. A key compiled into an APK can be extracted by anyone.
* RailKit's terms (read 16 Oct 2026) require API keys to stay confidential, forbid redistributing its
  data to third parties and allow its data only in applications you own or operate. A key shipped
  inside a public app conflicts with that, and so would sharing responses outside it.
* A backend lets you add per-device rate limiting, change providers without an app update, and
  enforce your provider's caching rules in one place.

## Provider survey (what I could and could not verify)

Information below was read from the providers' public pages on **16 October 2026**. It may have
changed. **I did not verify any provider's behaviour by calling it**, and for each one the pages I
could read left gaps that you must close before committing to it.

| | RailRadar (`railradar.in`) | RailKit (`railkit.in`) |
|---|---|---|
| Docs read | [railradar.in/docs](https://railradar.in/docs) | [railkit.in](https://railkit.in/), [pricing](https://railkit.in/pricing), [terms](https://railkit.in/terms) |
| Auth / base URL | `Authorization: Bearer <key>` (or `X-API-Key`); `https://api.railradar.in/v1` | Key set via SDK; REST base URL and header **not stated** on the pages I read |
| Live running status | Documented: `GET /v1/trains/{number}/live` (position, delay minutes, current halt, alerts). **Response fields are not documented** beyond a generic envelope (`success`, `data`, `meta`) | Claimed (`trackTrain`, `trackTrainV2`); no REST path or schema shown |
| Train / station search | Documented: `GET /v1/lookup/search/trains`, `/v1/lookup/search/stations`; query parameter names not shown | Claimed (`trainsByName`, `stationsByName`, …) |
| Seat availability | **Not documented** | Claimed (`getAvailability`, `fareLookup`); no REST path or schema shown |
| PNR status | **Not documented** | Claimed (`checkPNRStatus`); the FAQ mentions a REST endpoint but gives no path |
| Pricing / limits | Free sandbox: 100 requests / month; higher volume via developer console, prices not shown | Free ₹0: 50 req/month, 100 per 10 min · Pro ₹59: 5,000/month · Advance ₹89: 10,000/month · Enterprise: custom |
| Data source | Describes data as crowd-sourced telemetry plus PRS synchronisation; states it is not affiliated with Indian Railways, IRCTC or NTES | Not stated; its terms say it is not an official railway source and may rely on third-party sources |
| Terms | Linked ([terms](https://railradar.in/terms)) but I could not read their content | Read. Commercial use only on a paid plan; no redistribution; data only in apps you own or operate; caching only to improve performance and not to avoid API calls; long-term retention prohibited except for temporary application functionality; no SLA |

Other names that appeared in my search results but that I did **not** evaluate: IndianRailAPI,
RapidAPI listings, AOPAY. Official Indian Railways sources (NTES, PRS / IRCTC) are operated by
CRIS / IRCTC; I found no public, self-serve, licensed API for them. Do not scrape IRCTC or NTES.

Conclusion: no single provider I could read documents live status, availability and PNR **with
response schemas and readable terms**. Pick one, get its schema from a real sandbox response, and
write the adapter in your backend. Until then, run in sample-data mode.

**Retention caveat.** RailSathi caches the last successful running status in Room so it can be shown
offline. Whether that is "temporary application functionality" under your provider's terms (for
example RailKit's retention clause) is for you to confirm; if not, clear the cache more aggressively
(`CachedTrainStatusDao.deleteOlderThan`) or disable it.

## The contract

All endpoints are under the base URL, return JSON (`Content-Type: application/json`), and must be
served over HTTPS. Every field in every response is **optional** — omit what the provider does not
know and the app shows "Not reported". Never fill in guesses.

Date-times are ISO-8601 with offset (`2026-10-16T14:30:00+05:30`). Dates are `YYYY-MM-DD`.

### `GET /v1/trains/{number}/running-status?date=YYYY-MM-DD`

```json
{
  "train_number": "12951",
  "train_name": "Mumbai Central - New Delhi Rajdhani",
  "journey_date": "2026-10-16",
  "status_text": "Running",
  "last_reported_station": "Agra Cantt",
  "last_reported_at": "2026-10-16T09:55:00+05:30",
  "delay_minutes": 12,
  "expected_destination_arrival": "2026-10-16T16:42:00+05:30",
  "last_updated_at": "2026-10-16T10:00:00+05:30",
  "stops": [
    {
      "station_code": "AGC",
      "station_name": "Agra Cantt",
      "scheduled_arrival": "2026-10-16T08:00:00+05:30",
      "estimated_arrival": "2026-10-16T08:12:00+05:30",
      "scheduled_departure": "2026-10-16T08:05:00+05:30",
      "estimated_departure": "2026-10-16T08:17:00+05:30",
      "delay_minutes": 12,
      "state": "CURRENT"
    }
  ]
}
```

`state` is `PASSED`, `CURRENT` or `UPCOMING` (anything else is shown as "Status unknown").
`last_updated_at` should be the **provider's** timestamp for the position data; the app warns when it
is more than 30 minutes old. If it is omitted the app cannot judge staleness.

### `GET /v1/availability?from=NDLS&to=BPL&date=YYYY-MM-DD[&class=3A][&quota=GN]`

```json
{
  "last_updated_at": "2026-10-16T10:00:00+05:30",
  "trains": [
    {
      "train_number": "12951",
      "train_name": "Mumbai Central - New Delhi Rajdhani",
      "from_station_code": "NDLS",
      "to_station_code": "BPL",
      "departure": "2026-10-17T16:35:00+05:30",
      "arrival": "2026-10-18T02:35:00+05:30",
      "duration_minutes": 600,
      "classes": [
        { "travel_class": "SL", "status_text": "AVAILABLE-0042", "fare_rupees": 740 },
        { "travel_class": "3A", "status_text": "GNWL 14/WL 9" }
      ]
    }
  ]
}
```

`status_text` is shown to the user verbatim; the app only inspects it to choose a colour
(`AVAILABLE…`, `RAC…`, `…WL…`, `REGRET`). Return `{"trains": []}` when there are no matching trains.
`class` and `quota` are sent only when the user picks them. Supported classes in the UI:
`SL 3A 2A 1A CC EC 3E 2S`; quotas: `GN TQ PT LD SS`.

### `POST /v1/pnr/status`

Request body: `{"pnr": "1234567890"}` (POST so the PNR is not in a URL that proxies may log).

```json
{
  "train_number": "12951",
  "train_name": "Mumbai Central - New Delhi Rajdhani",
  "journey_date": "2026-10-20",
  "boarding_station": "New Delhi (NDLS)",
  "destination_station": "Bhopal Jn (BPL)",
  "travel_class": "3A",
  "chart_prepared": false,
  "last_updated_at": "2026-10-16T10:00:00+05:30",
  "passengers": [
    { "serial": 1, "booking_status": "CNF/B2/21", "current_status": "CNF", "coach": "B2", "berth": "21 Lower" }
  ]
}
```

Do **not** return passenger names, ages or the PNR itself; the app neither needs nor displays them.
Your service must not log the PNR either.

### `GET /v1/trains/search?q=rajdhani`

```json
{ "trains": [ { "number": "12951", "name": "Mumbai Central - New Delhi Rajdhani" } ] }
```

Called for non-numeric queries on the Home screen (minimum 2 characters).

### Errors

Use plain HTTP status codes; the body is ignored.

| Status | App shows |
|---|---|
| 400, 422 | "Please check your input and try again." |
| 401, 403 | "The app isn't authorised to use the data service…" |
| 404 | "No record was found for that request." |
| 429 | "Too many requests. Please wait a minute and try again." (never retried) |
| 5xx | "The data service is having problems…" (502/503/504 on GET are retried twice) |
| Connection failure / timeout | "Can't reach the server…" and, for running status, the last saved copy |
| 200 with unusable body (no train identity / no passengers) | "The data provider returned no information for this request." |

Fixtures that match this schema are in `app/src/test/resources/mock/`.

## Backend checklist

* Keep the provider key in server-side configuration (environment variable / secret manager).
* HTTPS only, current TLS. The app blocks cleartext.
* Rate limit per client and respect your provider's quota; return 429 rather than silently failing.
* Do not log PNRs, and do not retain provider data longer than its terms allow.
* Consider abuse protection such as Play Integrity attestation — an unauthenticated public endpoint
  invites quota theft.
* Return the provider's own timestamp in `last_updated_at`; do not stamp responses with "now".
* Test against the app with a real device or emulator using an HTTPS URL (a tunnel such as
  `cloudflared` or `ngrok` works for development).

## Publishing checklist

- [ ] Provider chosen; terms read in full (commercial use, caching, retention, redistribution, attribution).
- [ ] Adapter written from a **real** sample response, not from documentation alone.
- [ ] Backend deployed on HTTPS with the key stored server-side.
- [ ] `RAIL_API_BASE_URL` set for the release build; app verified end to end on a device.
- [ ] Privacy policy updated if your backend logs or retains anything.
- [ ] Store listing states the app is not affiliated with Indian Railways / IRCTC.
