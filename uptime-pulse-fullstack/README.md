# UptimePulse

**Multi-Tenant API Monitoring & Uptime Status Platform**

UptimePulse is a full-stack monitoring platform for tracking the uptime and latency of any HTTP(S) endpoint. A Spring Boot backend polls registered endpoints on a scheduled background worker, persists time-series ping data to MySQL, automatically flips monitors between `UP` and `DOWN`, and sends Discord webhook alerts on state changes. A React + Tailwind dashboard visualizes live status and latency history.

---

## 1. Architecture Overview

```
uptime-pulse-fullstack/
├── docker-compose.yml        # MySQL 8.0 container
├── backend/                  # Spring Boot 3 (Java 17) REST API + scheduled worker
└── frontend/                 # React (Vite) + Tailwind CSS dashboard
```

| Layer      | Technology                                                        |
|------------|--------------------------------------------------------------------|
| Backend    | Java 17, Spring Boot 3.3, Spring Data JPA, Spring Web, Spring Scheduling, Lombok, RestTemplate |
| Frontend   | React 18 (Vite), Tailwind CSS, Recharts, Lucide Icons, Axios       |
| Database   | MySQL 8.0                                                          |
| Alerts     | Discord Incoming Webhooks                                          |

### How it works

1. You register a monitor (name + URL) via the dashboard or REST API.
2. Every **30 seconds** (configurable), `UptimeWorkerService` fans out an HTTP GET request to every active monitor concurrently.
3. Each check's status code, latency (ms), and outcome is saved as a row in `ping_logs`.
4. If a monitor's result changes from `UP` → `DOWN` or `DOWN` → `UP`, its status is updated and — if alerts are enabled — a rich embed is posted to the configured Discord webhook.
5. The React dashboard polls the API every 10 seconds and renders live status badges plus an interactive latency chart (Recharts) for the selected monitor.

---

## 2. Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+** (or use the included wrapper if you add one)
- **Node.js 18+** and **npm 9+**
- **Docker** and **Docker Compose** (for MySQL) — or a locally running MySQL 8.0 instance

---

## 3. Quick Start

### Step 1 — Start MySQL

From the project root:

```bash
docker-compose up -d
```

This starts a MySQL 8.0 container on `localhost:3306` with:

- Database: `uptimepulse`
- User: `uptimepulse`
- Password: `uptimepulse`
- Root password: `rootpassword`

Wait a few seconds for the healthcheck to pass. You can verify with:

```bash
docker-compose ps
```

> **No Docker?** Install MySQL 8.0 locally and create the database/user manually:
> ```sql
> CREATE DATABASE uptimepulse CHARACTER SET utf8mb4;
> CREATE USER 'uptimepulse'@'%' IDENTIFIED BY 'uptimepulse';
> GRANT ALL PRIVILEGES ON uptimepulse.* TO 'uptimepulse'@'%';
> FLUSH PRIVILEGES;
> ```

### Step 2 — Run the Backend

```bash
cd backend
mvn clean spring-boot:run
```

Spring Boot will:
- Connect to MySQL using the settings in `src/main/resources/application.properties`.
- Auto-create the `monitors` and `ping_logs` tables via Hibernate DDL (`spring.jpa.hibernate.ddl-auto=update`).
- Start listening on **http://localhost:8080**.
- Start the scheduled health-check worker (first run after a 5-second initial delay, then every 30 seconds).

You should see log output confirming the app started and, once monitors are registered, periodic `"Starting health-check cycle..."` log lines.

### Step 3 — Run the Frontend

In a new terminal:

```bash
cd frontend
npm install
npm run dev
```

The dashboard will be available at **http://localhost:5173**. Vite is pre-configured (see `vite.config.js`) to proxy any request to `/api/*` to `http://localhost:8080`, so no additional CORS setup is needed in development.

### Step 4 — Add your first monitor

1. Open http://localhost:5173
2. Click **Add Monitor**
3. Enter a name (e.g. `Production API`) and a URL (e.g. `https://api.github.com`)
4. Optionally toggle **Discord Alerts** and paste an [Incoming Webhook URL](#5-discord-webhook-setup)
5. Submit — the monitor is checked immediately and then every 30 seconds thereafter

---

## 4. REST API Reference

Base URL: `http://localhost:8080/api`

| Method | Endpoint                          | Description                                      |
|--------|------------------------------------|---------------------------------------------------|
| GET    | `/monitors`                        | List all monitors (optional `?tenantId=` filter)  |
| GET    | `/monitors/{id}`                   | Get a single monitor                              |
| POST   | `/monitors`                        | Create a new monitor                              |
| PUT    | `/monitors/{id}`                   | Replace a monitor's configuration                 |
| PATCH  | `/monitors/{id}`                   | Partially update fields (active, alerts, etc.)    |
| DELETE | `/monitors/{id}`                   | Delete a monitor and its ping history              |
| GET    | `/monitors/{id}/history?limit=100` | Recent ping_log entries, most recent first         |
| GET    | `/monitors/{id}/stats`             | Aggregate uptime percentage and check counts       |
| POST   | `/monitors/{id}/check-now`         | Trigger an immediate synchronous health check      |
| GET    | `/monitors/summary`                | Dashboard counters (total/up/down/pending)         |

### Example: create a monitor

```bash
curl -X POST http://localhost:8080/api/monitors \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Production API",
        "url": "https://api.github.com",
        "tenantId": "acme-corp",
        "checkIntervalSeconds": 30,
        "expectedStatusCode": 200,
        "alertsEnabled": true,
        "discordWebhookUrl": "https://discord.com/api/webhooks/XXXX/YYYY"
      }'
```

---

## 5. Discord Webhook Setup

1. In your Discord server, go to **Server Settings → Integrations → Webhooks**.
2. Click **New Webhook**, choose the target channel, and copy the **Webhook URL**.
3. Paste it into the **Discord Webhook URL** field when creating/editing a monitor, and enable **Discord Alerts**.
4. When a monitor transitions to `DOWN`, UptimePulse posts a red embed with the URL, latency, HTTP status, and timestamp. When it recovers, a green recovery embed follows.

---

## 6. Configuration Reference

All backend settings live in `backend/src/main/resources/application.properties`:

| Property                                      | Default | Description                                             |
|------------------------------------------------|---------|-----------------------------------------------------------|
| `uptimepulse.worker.fixed-delay-ms`             | `30000` | Delay between health-check sweeps                        |
| `uptimepulse.worker.connect-timeout-ms`         | `8000`  | HTTP connect timeout per check                            |
| `uptimepulse.worker.read-timeout-ms`            | `8000`  | HTTP read timeout per check                                |
| `uptimepulse.worker.failure-threshold`          | `1`     | Consecutive failures required before flipping to `DOWN`   |
| `uptimepulse.worker.retention-days`             | `14`    | Days of `ping_logs` history kept before nightly pruning    |

Frontend configuration lives in `frontend/.env` (copy from `.env.example`):

| Variable              | Default | Description                                  |
|------------------------|---------|-----------------------------------------------|
| `VITE_API_BASE_URL`    | `/api`  | Base path/URL the dashboard calls for the API |

---

## 7. Production Build

**Backend:**

```bash
cd backend
mvn clean package -DskipTests
java -jar target/uptimepulse.jar
```

**Frontend:**

```bash
cd frontend
npm run build
npm run preview
```

The build output is written to `frontend/dist/` and can be served by any static file host or reverse proxy (e.g. Nginx) in front of the Spring Boot API.

---

## 8. Troubleshooting

- **Backend fails to connect to MySQL**: confirm `docker-compose ps` shows the `uptimepulse-mysql` container as `healthy`, and that port `3306` isn't already in use by another MySQL instance.
- **Frontend shows "Backend unreachable"**: confirm the Spring Boot app is running on port 8080 and check the browser console/network tab for the failing request.
- **Discord alerts not arriving**: verify the webhook URL is correct, alerts are toggled on for the monitor, and check backend logs for `Failed to send Discord alert` entries.
- **Monitor stuck on PENDING**: the first check runs ~5 seconds after backend startup and thereafter every 30 seconds; use **Check Now** on the dashboard to force an immediate check.
