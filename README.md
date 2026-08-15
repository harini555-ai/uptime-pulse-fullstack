# 🚀 UptimePulse – Multi-Tenant API & Website Monitoring Platform

UptimePulse is a **Full-Stack Website and API Uptime Monitoring Platform** built to monitor the availability, health, and response performance of web services in real time.

The platform allows users to configure monitoring endpoints and continuously track their **HTTP status, uptime state, and response latency** through a centralized dashboard.

The application is built using **React.js, Spring Boot, MySQL, Docker, Vercel, and Render**, following a modern client-server architecture.

---

## 🌐 Live Demo

| Service        | URL                                                    |
| -------------- | ------------------------------------------------------ |
| 🌐 Frontend    | https://uptime-pulse-fullstack.vercel.app              |
| ⚙️ Backend API | https://uptime-pulse-fullstack-1.onrender.com          |
| 💻 Source Code | https://github.com/harini555-ai/uptime-pulse-fullstack |

---

# ✨ Features

### 🔍 Automated Health Monitoring

* Periodically checks configured website/API endpoints.
* Records HTTP response status codes.
* Measures response/round-trip latency.
* Detects endpoint availability and downtime.

### 📊 Live Monitoring Dashboard

The dashboard provides a centralized view of monitored services.

Each endpoint is classified as:

* 🟢 **UP** – Endpoint is responding successfully.
* 🔴 **DOWN** – Endpoint is unavailable or returning an error.
* 🟡 **PENDING** – Endpoint has not completed its first health check.

### ⚡ Response Time Monitoring

Track the response performance of monitored endpoints and identify slow or unhealthy services.

### 🛡️ Resilient Error Handling

The backend uses centralized exception handling to provide consistent API error responses and improve application reliability.

### ☁️ Cloud Deployment

The application is deployed using modern cloud infrastructure:

* Frontend → Vercel
* Backend → Render
* Database → Aiven Cloud MySQL
* Containerization → Docker

---

# 🏗️ System Architecture

```text
                    ┌──────────────────────┐
                    │      User / Client   │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   React.js Frontend  │
                    │      Vite + UI       │
                    └──────────┬───────────┘
                               │
                            Axios
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Spring Boot REST   │
                    │        API           │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
      ┌──────────────────┐          ┌──────────────────┐
      │   Monitoring     │          │   JPA / Hibernate│
      │    Services      │          │   Data Access    │
      └────────┬─────────┘          └────────┬─────────┘
               │                             │
               ▼                             ▼
      ┌──────────────────┐          ┌──────────────────┐
      │ Website / API    │          │  Aiven MySQL     │
      │ Health Checks    │          │    Database      │
      └──────────────────┘          └──────────────────┘
```

---

# 🛠️ Technology Stack

## Frontend

* React.js
* Vite
* Tailwind CSS
* Axios
* Lucide React

## Backend

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Hibernate
* Maven

## Database

* MySQL
* Aiven Cloud MySQL
* SSL-secured database connection

## DevOps & Deployment

* Docker
* Render
* Vercel
* GitHub

---

# 📁 Project Structure

```text
uptime-pulse-fullstack/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── uptimepulse/
│   │       │           ├── controllers/
│   │       │           ├── models/
│   │       │           ├── repositories/
│   │       │           └── services/
│   │       │
│   │       └── resources/
│   │
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── App.jsx
│   │   └── ...
│   │
│   ├── package.json
│   └── vite.config.js
│
├── README.md
└── .gitignore
```

---

# 🔄 Application Workflow

```text
1. User opens the monitoring dashboard
              ↓
2. React frontend communicates with Spring Boot API
              ↓
3. Backend retrieves configured monitoring data
              ↓
4. Monitoring service performs health checks
              ↓
5. HTTP status and response latency are recorded
              ↓
6. Monitoring result is stored/processed
              ↓
7. Dashboard displays current endpoint status
              ↓
8. User can identify UP / DOWN / PENDING services
```

---

# ⚙️ Environment Configuration

## Backend

Configure the following environment variables in the backend deployment environment:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://<aiven-host>:<port>/defaultdb?sslmode=require
SPRING_DATASOURCE_USERNAME=<db-username>
SPRING_DATASOURCE_PASSWORD=<db-password>
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

> **Security:** Never commit real database credentials, passwords, or other secrets to GitHub.

---

## Frontend

Create a `.env` file in the frontend directory:

```env
VITE_API_BASE_URL=https://uptime-pulse-fullstack-1.onrender.com/api
```

For local development:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

# 🚀 Getting Started

## Prerequisites

Make sure the following are installed:

* Java 17+
* Maven
* Node.js
* npm
* MySQL (for local database development)
* Git
* Docker (optional)

---

# 1️⃣ Clone the Repository

```bash
git clone https://github.com/harini555-ai/uptime-pulse-fullstack.git
cd uptime-pulse-fullstack
```

---

# 2️⃣ Start the Backend

Navigate to the backend:

```bash
cd backend
```

Build the Spring Boot application:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```

The backend will be available at:

```text
http://localhost:8080
```

---

# 3️⃣ Start the Frontend

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will be available at the URL displayed by Vite, typically:

```text
http://localhost:5173
```

---

# 🐳 Docker Deployment

The backend includes a Dockerfile for containerized deployment.

Build the backend image:

```bash
docker build -t uptimepulse-backend ./backend
```

Run the container:

```bash
docker run -p 8080:8080 uptimepulse-backend
```

Docker provides a consistent runtime environment for deploying the Spring Boot backend.

---

# ☁️ Deployment

## Frontend

The React/Vite frontend is deployed on:

**Vercel**

Live application:

https://uptime-pulse-fullstack.vercel.app

---

## Backend

The Spring Boot REST API is deployed on:

**Render**

Backend:

https://uptime-pulse-fullstack-1.onrender.com

---

## Database

The application uses:

**Aiven Cloud MySQL**

The database connection is configured through environment variables to keep credentials secure.

---

# 🔌 Backend API

The frontend communicates with the Spring Boot backend through REST APIs.

Base URL:

```text
/api
```

The API is responsible for:

* Endpoint management
* Health monitoring
* Monitoring status
* Response-time tracking
* Database operations
* Error handling

API endpoints can be extended as the monitoring platform grows.

---

# 📈 Monitoring Status

UptimePulse uses three primary monitoring states:

| Status     | Meaning                                      |
| ---------- | -------------------------------------------- |
| 🟢 UP      | Endpoint is reachable and responding         |
| 🔴 DOWN    | Endpoint is unavailable or returned an error |
| 🟡 PENDING | Initial health check is still pending        |

---

# 🧩 Key Engineering Concepts

This project demonstrates practical implementation of:

* Full-Stack Web Development
* RESTful API Development
* React Component Architecture
* Spring Boot
* Spring Data JPA
* Hibernate ORM
* MySQL Database Integration
* HTTP Health Monitoring
* Response Time Measurement
* Exception Handling
* Axios API Integration
* Docker Containerization
* Cloud Deployment
* Environment-based Configuration
* Frontend/Backend Separation

---

# 🎯 Project Objective

The primary objective of UptimePulse is to provide a centralized platform for monitoring the health and performance of websites and APIs.

Instead of manually checking whether services are available, UptimePulse automates health checks and presents the results through an easy-to-use dashboard.

---

# 🔮 Future Enhancements

Possible future improvements include:

* 🔔 Email notifications for downtime
* 📱 SMS / push notifications
* 📊 Historical uptime analytics
* 📈 Response-time graphs
* 🔐 User authentication and authorization
* 👥 Advanced multi-tenant organization management
* ⏱️ Configurable monitoring intervals
* 📋 Monitoring history
* 🚨 Alert rules and thresholds
* 📉 SLA and uptime percentage reports
* 🌍 Multi-region monitoring
* 📦 Kubernetes-based deployment

---

# 🧪 Project Status

**Status:** Production-style Full-Stack Application

The project currently demonstrates:

* React frontend
* Spring Boot REST backend
* MySQL persistence
* Automated endpoint monitoring
* Cloud deployment
* Docker-based backend packaging

---

# 👩‍💻 Author

**Harini M**

Computer Science Engineering Student

GitHub:
https://github.com/harini555-ai

---

# ⭐ Contributing

Contributions, suggestions, and improvements are welcome.

To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Commit your changes.
5. Push the branch.
6. Create a Pull Request.

---

# 📄 License

This project is intended for educational, portfolio, and software-development purposes.
