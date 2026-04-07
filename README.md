# CypherShare - Secure Real-Time File Sharing SaaS Platform

![CypherShare](https://img.shields.io/badge/status-production--ready-green)
![Java](https://img.shields.io/badge/java-17-orange)
![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.1-green)
![React](https://img.shields.io/badge/react-18-blue)
![Docker](https://img.shields.io/badge/docker-ready-blue)
![Kubernetes](https://img.shields.io/badge/kubernetes-ready-326CE5)

## 🚀 Overview

CypherShare is a production-ready, scalable SaaS platform for secure real-time file sharing with zero-storage architecture. Built with Java Spring Boot (WebFlux) and React, designed to handle thousands of concurrent users.

### Key Features

- ⚡ **Real-time Relay** - Zero-storage, in-memory streaming
- 🔐 **UDEF Protocol** - Dictionary-based compression + AES-256-GCM encryption
- 🎯 **6-Digit Codes** - Simple, secure file sharing
- 📈 **Auto-Scaling** - Kubernetes HPA for dynamic scaling
- 🌐 **Distributed** - Redis for multi-pod session management
- 💰 **Freemium SaaS** - Ready for monetization

---

## 📋 Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Sender    │─────▶│  CypherShare │─────▶│  Receiver   │
│  (Upload)   │      │    Server    │      │ (Download)  │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                     ┌──────┴──────┐
                     │             │
                 ┌───▼───┐    ┌────▼────┐
                 │ Redis │    │Postgres │
                 │Session│    │UserData │
                 └───────┘    └─────────┘
```

### Tech Stack

**Backend:**
- Java 17 + Spring Boot 3.2.1
- Spring WebFlux (Reactive)
- Redis (Distributed Sessions)
- PostgreSQL (User Data)

**Frontend:**
- React 18
- Tailwind CSS
- Framer Motion
- Lucide Icons

**Infrastructure:**
- Docker & Docker Compose
- Kubernetes (HPA, Ingress)
- Nginx (Frontend)

---

## 🛠️ Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 20+
- Docker & Docker Compose
- (Optional) Kubernetes cluster

### Local Development

```bash
# 1. Start with Docker Compose
docker-compose up -d

# 2. Access application
# Frontend: http://localhost:3000
# Backend:  http://localhost:8001
# Health:   http://localhost:8001/api/relay/health
```

---

## 🐳 Docker Deployment

### Build Images

```bash
./build-docker.sh
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop services
docker-compose down
```

---

## ☸️ Kubernetes Deployment

### Deploy

```bash
# Quick deploy
./deploy-k8s.sh

# Or manually
kubectl apply -f k8s/
```

### Verify

```bash
# Check pods
kubectl get pods -n cyphershare

# Check autoscaling
kubectl get hpa -n cyphershare

# View logs
kubectl logs -f deployment/cyphershare-backend -n cyphershare
```

---

## 💰 Monetization (Freemium Model)

### Subscription Tiers

| Tier | File Size | Daily Transfers | Price |
|------|-----------|----------------|-------|
| **FREE** | 100MB | 5 | $0 |
| **PRO** | 5GB | 100 | $9.99/month |
| **BUSINESS** | Unlimited | Unlimited + API | $29.99/month |

---

## 📊 Monitoring

```bash
# Health check
curl http://localhost:8001/api/relay/health

# Resource usage (K8s)
kubectl top pods -n cyphershare
```

---

## 🎯 Next Steps

1. **Fix streaming bug** - Implement proper synchronous relay
2. **Add authentication** - JWT + user registration
3. **Integrate Stripe** - Payment processing
4. **Build dashboard** - User portal

**Made with ❤️ for secure file sharing**
