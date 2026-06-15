# TCC — Empirical Comparison: Monolithic vs Microservices Architecture

> Undergraduate thesis (TCC) — UERJ, Faculdade de Engenharia, Departamento de Engenharia de Sistemas e Computação  
> Student: Vitor Santos Pereira (matricula: 201510170911) | Advisor: Robert Mota Oliveira  
> Submission target: June/July 2026

This repository contains **two functionally equivalent implementations** of a simplified social network, built to measure and compare the runtime performance of a **monolithic** and a **microservices** architecture under controlled load.

![Grafana Dashboard](docs/grafana-screenshot.png)

---

## Prerequisites

| Tool | Minimum Version | macOS | Windows | Linux |
|------|-----------------|-------|---------|-------|
| Docker & Docker Compose | 24+ | [docs.docker.com/desktop/install/mac-install](https://docs.docker.com/desktop/install/mac-install/) | [docs.docker.com/desktop/install/windows-install](https://docs.docker.com/desktop/install/windows-install/) | [docs.docker.com/engine/install](https://docs.docker.com/engine/install/) |
| Java | 21+ | `brew install openjdk@21` | [adoptium.net](https://adoptium.net/) | `apt install openjdk-21-jdk` |
| Maven | 3.9+ | `brew install maven` | [maven.apache.org](https://maven.apache.org/download.cgi) | `apt install maven` |
| K6 | latest | `brew install k6` | [k6.io/docs/get-started/installation](https://k6.io/docs/get-started/installation/) | [k6.io/docs/get-started/installation](https://k6.io/docs/get-started/installation/) |

---

## Repository Structure

```
Microsservice/                       # Git repository root
│
├── microsservice/                   # Microservices implementation
│   ├── user-ms/                     #   User domain (port 18081)
│   ├── post-ms/                     #   Post domain (port 18082)
│   ├── comment-ms/                  #   Comment domain (port 18083)
│   ├── like-ms/                     #   Like domain (port 18084)
│   ├── friendship-ms/               #   Friendship domain (port 18085)
│   ├── eureka-server-ms/            #   Netflix Eureka service discovery (port 8761)
│   ├── gateway-service-ms/          #   Spring Cloud Gateway entry point (port 18765)
│   ├── scripts/
│   │   ├── k6-load-test.js          #   K6 load test script (all scenarios)
│   │   ├── manage.sh                #   Legacy management script (superseded by start.sh)
│   │   └── test-results/            #   K6 JSON output
│   ├── docker-compose.yml           #   Microservices + databases
│   ├── docker-compose.db.yml        #   Databases only (alternative)
│   └── pom.xml                      #   Parent Maven POM
│
├── monolith/                        # Monolithic implementation
│   ├── src/main/java/com/mstcc/monolith/
│   │   ├── user/                    #   User domain (entity, dto, repository, service, controller)
│   │   ├── post/                    #   Post domain
│   │   ├── comment/                 #   Comment domain
│   │   ├── like/                    #   Like domain
│   │   ├── friendship/              #   Friendship domain
│   │   ├── config/                  #   CacheConfig, ObservabilityConfig
│   │   └── exception/               #   GlobalExceptionHandler, ErrorResponse, ValidationException
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   ├── docker-compose.yml           #   Monolith app + PostgreSQL
│   └── pom.xml
│
├── monitoring/                      # Shared Prometheus + Grafana (scrapes both stacks)
│   ├── prometheus.yml               #   Scrape config — microservices + monolith
│   └── grafana/
│       ├── datasources/
│       │   └── datasource.yml
│       └── dashboards/
│           ├── dashboard.yml        #   Grafana provisioning config
│           ├── microservices-dashboard.json
│           └── monolith-dashboard.json
│
├── docker-compose.monitoring.yml    # Prometheus + Grafana compose file
├── start.sh                         # Unified management console (entry point)
└── README.md
```

---

## How to Run

### 1. Clone the repository

```bash
git clone https://github.com/Vitor-Hub/TCC_Microservice.git
cd TCC_Microservice
```

### 2. Make the management console executable

```bash
chmod +x start.sh
```

### 3. Launch the console

```bash
./start.sh
```

The main menu appears:

```
TCC — MANAGEMENT CONSOLE

  [micro] Microsservices    [mono] Monolith
  [mon]   Monitoring        [0]    Exit
```

### 4. Start monitoring first

Select `mon` → `1) Start Monitoring`.  
Prometheus and Grafana start and begin scraping whichever stacks are running.

### 5. Deploy the stack you want to test

**Microsservices:**
1. `micro` → `1) Build` — compiles all 7 Maven projects
2. `micro` → `2) Deploy` — starts Docker Compose
3. Wait ~90 s, then `4) Health Check`

**Monolith:**
1. `mono` → `1) Build`
2. `mono` → `2) Deploy`
3. Wait ~60 s, then `4) Health Check`

> **Tip:** Use `3) Fresh Start` to wipe existing data and do a clean build + deploy in one step (recommended for first run).

### 6. Run a load test

Select `6) Stress Test (K6)` from either stack's sub-menu.  
Results are saved to `microsservice/scripts/test-results/`.

---

## K6 Test Scenarios

| Scenario | Duration | Virtual Users | Description |
|----------|----------|---------------|-------------|
| Baseline | 2 min | 5 | Low constant load — latency baseline |
| Steady Load | 3 min | 20 | Medium constant load |
| Stress Test | ~8 min | 0 → 30 → 60 → 100 → 150 → 0 | Progressive ramp to system limit |
| Spike Test | ~3 min | 10 → 200 → 10 | Sudden traffic burst |
| Read-Heavy | 2 min | 30 | Read-only operations (no writes) |
| Full Suite | ~18 min | mixed | All scenarios run sequentially |

All scenarios exercise the full user journey: register, create posts, browse the feed, comment, like, and add friends. Both stacks receive identical request shapes.

---

## Dashboards

| Dashboard | URL | Key panels |
|-----------|-----|-----------|
| Grafana home | http://localhost:3000 (admin/admin) | — |
| TCC — Microservices | http://localhost:3000/d/tcc-microservices | RPS per service, P95/P99 latency, success rate, JVM memory, active threads, **circuit breaker state** |
| TCC — Monolith | http://localhost:3000/d/tcc-monolith | RPS, P95/P99 latency, success rate, JVM memory, active threads, avg latency by endpoint |
| Prometheus | http://localhost:9090 | Raw metric explorer |

> The **Circuit Breaker panel** exists only on the Microservices dashboard because Resilience4j is not present in the monolith.

---

## Running Both Stacks Simultaneously

Both stacks are designed to coexist on the same host with no port conflicts:

| Resource | Microservices | Monolith |
|----------|---------------|----------|
| Entry point | `http://localhost:18765` (gateway) | `http://localhost:8080` |
| Individual service ports | 18081 – 18085 | — |
| PostgreSQL host ports | 5433 – 5437 (per service) | 5438 |
| Docker network | `mstcc-net` | `mono-net` |
| Compose project name | `microsservice` | `mstcc-mono` |
| Container prefix | `mstcc_` | `mono_` |

Steps to run both simultaneously:
1. `mono` → `Deploy`
2. `micro` → `Deploy`
3. `mon` → `Start Monitoring`

Prometheus scrapes both stacks via `host.docker.internal` using the `stack` label (`microservices` or `monolith`), so you can compare them side-by-side in Grafana.

> **Linux note:** `host.docker.internal` is not available by default on Linux. `start.sh` detects the OS and automatically passes `--add-host=host.docker.internal:host-gateway` to Docker Compose.

---

## Architecture Comparison

### Microservices
- 5 independent Spring Boot services + API Gateway + Eureka
- Each service owns its own PostgreSQL database (database-per-service pattern)
- Inter-service communication via Spring Cloud OpenFeign over HTTP/REST
- Parallel upstream calls via `CompletableFuture` to avoid additive latency
- Circuit breakers via Resilience4j (closed / open / half-open states)
- Service discovery via Netflix Eureka

### Monolith
- Single Spring Boot application, Java 21
- Shared PostgreSQL database — all 5 domains in one schema
- Inter-domain communication via direct Spring `@Service` injection — zero network overhead
- Same Caffeine cache TTLs and cache names as the microservices stack
- Same Micrometer / Prometheus observability setup — metrics are comparable out of the box
- No Feign, no Eureka, no Resilience4j
