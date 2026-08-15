UptimePulse – Multi-Tenant API & Website Monitoring Platform
UptimePulse is a full-stack website and API uptime monitoring platform. It continuously tracks service availability, response times, and HTTP status codes, alerting users in real time when endpoints go down.

🚀 Live Demo
Frontend (Vercel): https://uptime-pulse-fullstack.vercel.app

Backend API (Render): https://uptime-pulse-fullstack-1.onrender.com

🛠️ Tech Stack
Frontend: React.js, Vite, Tailwind CSS, Axios, Lucide Icons

Backend: Java 17, Spring Boot 3, Spring Data JPA, Spring Web

Database: Aiven MySQL (Cloud Managed)

Containerization & Deployment: Docker, Render (Backend), Vercel (Frontend)

⚙️ Architecture & Features
Real-time Uptime Checks: Periodic automated polling of endpoints for status codes and latency.

Status Analytics: Visual dashboard categorizing services as UP, DOWN, or PENDING.

Cloud Native: Containerized with multi-stage Docker builds and connected to an enterprise-grade cloud MySQL instance via SSL.

Responsive UI: Dark-mode optimized dashboard for monitoring status indicators and latency trends.

📦 Project Structure
Plaintext
uptime-pulse-fullstack/
├── backend/
│   ├── src/main/java/com/uptimepulse/
│   │   ├── controllers/
│   │   ├── models/
│   │   ├── repositories/
│   │   └── services/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   └── App.jsx
│   ├── package.json
│   └── vite.config.js
└── README.md
🔧 Environment Variables
Backend (application.properties / Render Environment)
Properties
SPRING_DATASOURCE_URL=jdbc:mysql://<aiven-host>:<port>/defaultdb?sslmode=require
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
SPRING_JPA_HIBERNATE_DDL_AUTO=update
Frontend (.env / Vercel Environment)
Code snippet
VITE_API_BASE_URL=https://uptime-pulse-fullstack-1.onrender.com/api
💻 Local Development Setup
1. Backend
Bash
cd uptime-pulse-fullstack/backend
mvn clean install
mvn spring-boot:run
2. Frontend
Bash
cd uptime-pulse-fullstack/frontend
npm install
npm run dev
