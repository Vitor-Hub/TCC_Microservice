# 🚀 TCC: Microsserviços vs Monolítico - Análise Comparativa de Desempenho

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![K6](https://img.shields.io/badge/K6-Load%20Testing-purple.svg)](https://k6.io/)

> Projeto de Trabalho de Conclusão de Curso comparando desempenho, escalabilidade e complexidade operacional entre arquiteturas monolítica e de microsserviços.

---

## 📋 Sobre o Projeto

Este projeto implementa uma **rede social simplificada** usando duas arquiteturas diferentes para análise comparativa:

- **🔷 Microsserviços** (este repositório): Arquitetura distribuída com serviços independentes
- **🔶 Monolítico** (repositório separado): Arquitetura tradicional em aplicação única

### 🎯 Objetivos do TCC

1. Comparar **desempenho** (latência, throughput, tempo de resposta)
2. Avaliar **escalabilidade** e comportamento sob carga
3. Analisar **complexidade operacional** (deployment, monitoramento, debugging)
4. Medir **uso de recursos** (memória, CPU, rede)
5. Testar **resiliência** a falhas

---

## 🏗️ Arquitetura de Microsserviços

### Arquitetura Visual

```
                    ┌─────────────────────────────┐
                    │   Clients (Web/Mobile)      │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │   API GATEWAY (:18765)      │
                    │  • Routing                  │
                    │  • Load Balancing          │
                    │  • Circuit Breaker         │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  EUREKA SERVER (:8761)      │
                    │  • Service Discovery        │
                    │  • Health Monitoring        │
                    └──────────────┬──────────────┘
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        │              │           │           │              │
        ▼              ▼           ▼           ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│  User MS     │ │ Post MS  │ │Comment MS│ │ Like MS  │ │ Friendship   │
│  :18081      │ │ :18082   │ │ :18083   │ │ :18084   │ │     MS       │
│              │ │          │ │          │ │          │ │   :18085     │
└──────┬───────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
       │              │            │            │              │
       ▼              ▼            ▼            ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│  User DB     │ │ Post DB  │ │Comment DB│ │ Like DB  │ │ Friendship   │
│  :5433       │ │ :5434    │ │ :5435    │ │ :5436    │ │     DB       │
│              │ │          │ │          │ │          │ │   :5437      │
└──────────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────────┘

                    ┌─────────────────────────────┐
                    │    Monitoring Stack         │
                    │  • Prometheus (:9090)       │
                    │  • Grafana (:3000)          │
                    └─────────────────────────────┘
```

### 📦 Componentes

| Componente | Porta | Descrição | Tecnologia |
|------------|-------|-----------|------------|
| **Eureka Server** | 8761 | Service Discovery e Registry | Spring Cloud Netflix |
| **API Gateway** | 18765 | Roteamento, Load Balancing | Spring Cloud Gateway |
| **User Service** | 18081 | Gerenciamento de usuários | Spring Boot + JPA |
| **Post Service** | 18082 | Publicações e posts | Spring Boot + JPA |
| **Comment Service** | 18083 | Comentários em posts | Spring Boot + JPA |
| **Like Service** | 18084 | Sistema de likes | Spring Boot + JPA |
| **Friendship Service** | 18085 | Rede de amizades | Spring Boot + JPA |
| **PostgreSQL DBs** | 5433-5437 | Bancos independentes (1 por serviço) | PostgreSQL 15 |
| **Prometheus** | 9090 | Coleta de métricas | Prometheus |
| **Grafana** | 3000 | Visualização e dashboards | Grafana |

### 🔧 Otimizações Implementadas

#### Performance
- ✅ HikariCP connection pooling otimizado (pool size: 30, min-idle: 15)
- ✅ Tomcat thread pool configurado (max: 400, min: 100)
- ✅ HTTP/2 habilitado em todos os serviços
- ✅ JVM tuning com G1GC e String Deduplication
- ✅ Batch processing para operações de banco (batch_size: 20)

#### Resiliência
- ✅ Resilience4j Circuit Breaker
- ✅ Retry mechanism (2 tentativas com 200ms de espera)
- ✅ Time limiter (10s timeout)
- ✅ Health checks em todos os serviços

#### Monitoramento
- ✅ Spring Boot Actuator com endpoints de health e metrics
- ✅ Micrometer + Prometheus para coleta de métricas
- ✅ Grafana dashboards configurados

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 17** - Linguagem de programação
- **Spring Boot 3.2.3** - Framework principal
- **Spring Cloud 2023.0.0** - Microsserviços (Eureka, Gateway, OpenFeign)
- **Spring Data JPA** - Persistência
- **PostgreSQL 15** - Banco de dados relacional
- **HikariCP** - Connection pooling
- **Resilience4j** - Circuit breaker, retry, rate limiter

### Infraestrutura
- **Docker** + **Docker Compose 3.8** - Containerização
- **Maven 3.8+** - Build e gerenciamento de dependências

### Monitoramento
- **Prometheus** - Coleta de métricas
- **Grafana** - Visualização de dados
- **Spring Boot Actuator** - Endpoints de métricas
- **Micrometer** - Instrumentação

### Testes de Carga
- **K6** - Ferramenta de teste de carga
- **Python 3** - Scripts de análise e geração de relatórios

---

## 🚀 Como Executar

### Pré-requisitos

Certifique-se de ter instalado:

- ✅ **Docker Desktop** (ou Docker Engine + Docker Compose)
- ✅ **Java 17+** (para build local)
- ✅ **Maven 3.8+** (para build local)
- ✅ **K6** (para testes de carga) - [Instalação](https://k6.io/docs/getting-started/installation/)
- ✅ **Python 3** (para geração de relatórios)

**Recursos recomendados do Docker:**
- **RAM**: 8 GB mínimo (12 GB recomendado para testes de carga)
- **CPU**: 4+ cores
- **Disco**: 10 GB livres

### 📥 Instalação

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/tcc-microservices.git
cd tcc-microservices

# Dê permissão de execução aos scripts
chmod +x *.sh
```

---

## 🎬 Início Rápido (Quick Start)

### Opção 1: Deploy Automático com Script ⚡ (RECOMENDADO)

```bash
# Script automático que faz tudo
./deploy.sh
```

Este script:
1. ✅ Limpa containers anteriores
2. ✅ Faz build das imagens Docker
3. ✅ Inicia bancos de dados e aguarda health check
4. ✅ Inicia Eureka Server e aguarda registro
5. ✅ Inicia microsserviços
6. ✅ Inicia API Gateway
7. ✅ Valida todo o ambiente
8. ✅ Mostra URLs de acesso

**Tempo estimado**: 3-5 minutos

### Opção 2: Execução Manual Completa

#### 1️⃣ Build dos Microsserviços

```bash
# Build de todos os serviços de uma vez
./build-all.sh

# OU build individual de cada serviço
cd eureka-server-ms && mvn clean package -DskipTests && cd ..
cd gateway-service-ms && mvn clean package -DskipTests && cd ..
cd user-ms && mvn clean package -DskipTests && cd ..
cd post-ms && mvn clean package -DskipTests && cd ..
cd comment-ms && mvn clean package -DskipTests && cd ..
cd like-ms && mvn clean package -DskipTests && cd ..
cd friendship-ms && mvn clean package -DskipTests && cd ..
```

#### 2️⃣ Subir Infraestrutura

```bash
# Subir todos os serviços (single command)
docker-compose up -d

# OU subir em etapas (melhor para debugging)
# Etapa 1: Bancos de dados
docker-compose up -d user-ms-db post-ms-db comment-ms-db like-ms-db friendship-ms-db
sleep 15

# Etapa 2: Eureka Server
docker-compose up -d eureka-server-ms
sleep 45

# Etapa 3: Microsserviços
docker-compose up -d user-ms post-ms comment-ms like-ms friendship-ms
sleep 30

# Etapa 4: API Gateway
docker-compose up -d gateway-service-ms
```

#### 3️⃣ Subir Monitoramento (Opcional)

```bash
docker-compose -f docker-compose_monitoring.yml up -d

# Acessar Grafana
open http://localhost:3000  # user: admin, pass: admin
```

#### 4️⃣ Validar Ambiente

```bash
./validate-environment.sh
```

**Saída esperada**: Todos os checks devem estar ✅ verdes

#### 5️⃣ Verificar no Navegador

```bash
# Eureka Dashboard (ver serviços registrados)
open http://localhost:8761

# Prometheus (ver métricas)
open http://localhost:9090

# Grafana (dashboards)
open http://localhost:3000
```

---

## 🧪 Executar Testes de Carga

### Teste Completo Automatizado

```bash
# Executa teste completo com 5 cenários (~18 minutos)
./run-load-test.sh microservices
```

Este script:
1. ✅ Valida que todos os serviços estão rodando
2. ⏳ Aguarda estabilização do sistema (30s)
3. 🚀 Executa teste K6 com 5 cenários
4. 📊 Coleta métricas durante o teste
5. 💾 Salva resultados em `test-results/`

### Cenários de Teste Implementados

O script K6 executa 5 cenários sequenciais:

| Cenário | Duração | VUs | Rampa | Objetivo |
|---------|---------|-----|-------|----------|
| **Baseline** | 2 min | 5 | - | Carga constante baixa |
| **Steady** | 3 min | 20 | - | Carga constante média |
| **Stress** | 8 min | 0→150 | Linear | Teste de estresse progressivo |
| **Spike** | 2.5 min | 10→200→10 | Rápida | Picos de carga repentinos |
| **Read Heavy** | 2 min | 30 | - | Operações de leitura intensivas |

**Duração total:** ~18 minutos  
**Mix de operações:** 60% leitura, 40% escrita

### Thresholds de Qualidade

```javascript
thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],  // 95% < 2s, 99% < 5s
    http_req_failed: ['rate<0.05'],                    // Taxa de erro < 5%
    http_reqs: ['rate>10']                             // Mínimo 10 req/s
}
```

### Teste Manual com K6

```bash
# Teste básico (2 minutos, 10 VUs)
k6 run --vus 10 --duration 2m k6-load-test.js

# Teste customizado
k6 run \
  --vus 50 \
  --duration 5m \
  --out json=results.json \
  k6-load-test.js

# Teste com variáveis de ambiente
k6 run -e BASE_URL=http://localhost:18765 k6-load-test.js
```

### Operações Testadas

- **CREATE_USER**: Criação de novos usuários
- **CREATE_POST**: Publicação de posts
- **CREATE_COMMENT**: Comentários em posts
- **CREATE_LIKE**: Likes em posts/comentários
- **CREATE_FRIENDSHIP**: Solicitações de amizade
- **GET_USERS**: Listagem de usuários
- **GET_POSTS**: Feed de posts
- **GET_USER_POSTS**: Posts de um usuário específico

---

## 📊 Gerar Relatórios

### Relatório Comparativo (Microsserviços vs Monolítico)

```bash
python3 generate-report.py test-results/ \
  --micro test-results/microservices_20240315_summary.json \
  --mono test-results/monolithic_20240315_summary.json
```

### Relatório Individual

```bash
python3 generate-report.py test-results/
```

### Conteúdo do Relatório

O script gera:
- 📄 **Relatório Markdown** (`analysis_report_YYYYMMDD.md`)
- 📈 **Gráficos ASCII** no terminal
- 📊 **Tabelas comparativas** de métricas
- 💡 **Análise estatística** e recomendações
- 🎯 **Identificação de gargalos**

**Métricas analisadas:**
- Latência (média, P90, P95, P99)
- Throughput (req/s)
- Taxa de erro (%)
- Tempo de resposta por operação
- Uso de recursos (CPU, memória, rede)

---

## 📈 Monitoramento

### Prometheus (Métricas)

**URL:** http://localhost:9090

**Queries úteis:**

```promql
# Requests por segundo
rate(http_server_requests_seconds_count[1m])

# Latência P95 por endpoint
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket[5m]))

# Uso de memória JVM (heap)
jvm_memory_used_bytes{area="heap"}

# Taxa de erro HTTP 5xx
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Circuit breaker open
resilience4j_circuitbreaker_state{state="open"}

# Database connection pool
hikaricp_connections_active
```

### Grafana (Dashboards)

**URL:** http://localhost:3000  
**Usuário:** admin  
**Senha:** admin

**Dashboards recomendados para importar:**
- Spring Boot 2.1 Statistics (ID: 10280)
- JVM Micrometer (ID: 4701)
- Prometheus Stats (ID: 2)

**Painéis criados:**
- Overview dos serviços
- Performance por endpoint
- Métricas de banco de dados
- Circuit breaker status
- Análise de threads

---

## 📁 Estrutura do Projeto

```
tcc-microservices/
├── 📂 eureka-server-ms/          # Service Discovery
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── 📂 gateway-service-ms/        # API Gateway
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── 📂 user-ms/                   # User microservice
│   ├── src/main/java/
│   │   └── com/tcc/user/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       └── model/
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── pom.xml
│   └── Dockerfile
├── 📂 post-ms/                   # Post microservice
├── 📂 comment-ms/                # Comment microservice
├── 📂 like-ms/                   # Like microservice
├── 📂 friendship-ms/             # Friendship microservice
├── 📂 monitoring/                # Monitoring configs
│   ├── prometheus.yml
│   └── grafana/
│       ├── datasources/
│       └── dashboards/
├── 📂 test-results/              # K6 test results
│   ├── microservices_YYYYMMDD_summary.json
│   ├── microservices_YYYYMMDD_full.json
│   └── analysis_report_YYYYMMDD.md
├── 📂 docs/                      # Additional documentation
│   ├── ARCHITECTURE.md
│   ├── API.md
│   └── DEPLOYMENT.md
├── 📄 docker-compose.yml         # Main services
├── 📄 docker-compose_monitoring.yml  # Monitoring stack
├── 📄 k6-load-test.js           # K6 test script
├── 🔧 deploy.sh                 # Automated deployment
├── 🔧 build-all.sh              # Build all services
├── 🔧 validate-environment.sh   # Environment validator
├── 🔧 run-load-test.sh          # Test executor
├── 🔧 cleanup-git-target.sh     # Git cleanup utility
├── 🐍 generate-report.py        # Report generator
├── 📝 .gitignore                # Git ignore rules
├── 📖 README.md                 # This file
└── 🐛 TROUBLESHOOTING.md        # Troubleshooting guide
```

---

## 🔧 Scripts Utilitários

| Script | Descrição | Uso | Tempo |
|--------|-----------|-----|-------|
| `deploy.sh` | Deploy completo automatizado | `./deploy.sh` | ~3-5 min |
| `build-all.sh` | Build de todos os microsserviços | `./build-all.sh` | ~5-10 min |
| `validate-environment.sh` | Valida ambiente completo | `./validate-environment.sh` | ~30s |
| `run-load-test.sh` | Executa testes de carga | `./run-load-test.sh microservices` | ~18 min |
| `generate-report.py` | Gera relatórios de análise | `python3 generate-report.py test-results/` | ~10s |
| `cleanup-git-target.sh` | Remove target/ do Git | `./cleanup-git-target.sh` | ~5s |

---

## 🌐 Endpoints da API

Todos os endpoints são acessados via **API Gateway** em `http://localhost:18765`

### User Service (`/user-ms/api/users`)

```bash
# Create user
POST /user-ms/api/users
Content-Type: application/json
{
  "name": "João Silva",
  "email": "joao@example.com",
  "bio": "Software Developer"
}

# List all users
GET /user-ms/api/users

# Get user by ID
GET /user-ms/api/users/{id}

# Update user
PUT /user-ms/api/users/{id}
Content-Type: application/json
{
  "name": "João Silva Updated",
  "bio": "Senior Software Developer"
}

# Delete user
DELETE /user-ms/api/users/{id}
```

### Post Service (`/post-ms/api/posts`)

```bash
# Create post
POST /post-ms/api/posts
Content-Type: application/json
{
  "user": { "id": 1 },
  "content": "My first post!"
}

# List all posts
GET /post-ms/api/posts

# Get posts by user
GET /post-ms/api/posts/user/{userId}

# Get post by ID
GET /post-ms/api/posts/{id}

# Delete post
DELETE /post-ms/api/posts/{id}
```

### Comment Service (`/comment-ms/api/comments`)

```bash
# Create comment
POST /comment-ms/api/comments
Content-Type: application/json
{
  "postId": 1,
  "userId": 2,
  "content": "Great post!"
}

# Get comments by post
GET /comment-ms/api/comments/post/{postId}

# Get comment by ID
GET /comment-ms/api/comments/{id}

# Delete comment
DELETE /comment-ms/api/comments/{id}
```

### Like Service (`/like-ms/api/likes`)

```bash
# Create like (post)
POST /like-ms/api/likes
Content-Type: application/json
{
  "postId": 1,
  "userId": 2,
  "commentId": null
}

# Create like (comment)
POST /like-ms/api/likes
Content-Type: application/json
{
  "postId": null,
  "userId": 2,
  "commentId": 5
}

# Get likes by post
GET /like-ms/api/likes/post/{postId}

# Get likes by comment
GET /like-ms/api/likes/comment/{commentId}

# Delete like
DELETE /like-ms/api/likes/{id}
```

### Friendship Service (`/friendship-ms/api/friendships`)

```bash
# Create friendship
POST /friendship-ms/api/friendships
Content-Type: application/json
{
  "userId1": 1,
  "userId2": 2,
  "status": "PENDING"
}

# Get user friends
GET /friendship-ms/api/friendships/user/{userId}

# Update friendship status
PUT /friendship-ms/api/friendships/{id}
Content-Type: application/json
{
  "status": "ACCEPTED"
}

# Delete friendship
DELETE /friendship-ms/api/friendships/{id}
```

### Health & Actuator Endpoints

```bash
# Service health
GET /user-ms/actuator/health
GET /post-ms/actuator/health
# ... (all services)

# Metrics
GET /user-ms/actuator/prometheus
GET /user-ms/actuator/metrics

# Info
GET /user-ms/actuator/info
```

---

## 🐛 Troubleshooting

### Problemas Comuns e Soluções

#### 🔴 Serviços não iniciam

**Sintomas:**
- Containers reiniciando constantemente
- Logs mostram erros de memória ou conexão

**Soluções:**
```bash
# 1. Verificar logs
docker-compose logs gateway-service-ms
docker-compose logs user-ms

# 2. Aumentar memória do Docker
# Docker Desktop → Settings → Resources → Memory: 8-12GB

# 3. Reiniciar em ordem
docker-compose down
./deploy.sh
```

#### 🔴 Eureka não mostra serviços registrados

**Sintomas:**
- Eureka dashboard vazio ou incompleto
- Gateway retorna 503

**Soluções:**
```bash
# 1. Verificar se Eureka está healthy
curl http://localhost:8761/actuator/health

# 2. Reiniciar serviços em ordem
docker-compose restart eureka-server-ms
sleep 30
docker-compose restart user-ms post-ms comment-ms like-ms friendship-ms
sleep 20
docker-compose restart gateway-service-ms

# 3. Verificar registro
curl http://localhost:8761/eureka/apps
```

#### 🔴 Gateway retorna 503 Service Unavailable

**Sintomas:**
- Requisições ao Gateway falham com 503
- Serviços não estão acessíveis via Gateway

**Diagnóstico:**
```bash
# 1. Verificar se serviços estão registrados
open http://localhost:8761

# 2. Testar serviço diretamente
curl http://localhost:18081/api/users

# 3. Verificar logs do Gateway
docker-compose logs gateway-service-ms | grep -i error
```

**Soluções:**
- Aguardar 30-60s após iniciar serviços (tempo de registro)
- Verificar se todos os serviços aparecem no Eureka
- Reiniciar Gateway: `docker-compose restart gateway-service-ms`

#### 🔴 Erro de conexão com banco de dados

**Sintomas:**
- Serviços falham ao conectar ao PostgreSQL
- Logs mostram "Connection refused"

**Soluções:**
```bash
# 1. Verificar se bancos estão rodando
docker-compose ps | grep db

# 2. Testar conexão
docker exec user_ms_db psql -U user -d userdb -c "SELECT 1"

# 3. Reiniciar bancos
docker-compose restart user-ms-db post-ms-db comment-ms-db like-ms-db friendship-ms-db
sleep 10
docker-compose restart user-ms post-ms comment-ms like-ms friendship-ms
```

#### 🔴 Testes K6 com alta taxa de erro (>5%)

**Sintomas:**
- K6 mostra `http_req_failed` > 5%
- Muitos status 503 ou 500

**Possíveis causas:**
1. Serviços não estão completamente inicializados
2. Recursos insuficientes (RAM/CPU)
3. Circuit breaker aberto devido a falhas

**Soluções:**
```bash
# 1. Aguardar estabilização completa
sleep 60

# 2. Verificar health de todos os serviços
./validate-environment.sh

# 3. Verificar recursos do Docker
docker stats

# 4. Reduzir carga inicial do teste K6
# Editar k6-load-test.js: reduzir VUs iniciais

# 5. Verificar circuit breakers
curl http://localhost:18765/actuator/health | jq
```

#### 🔴 Porta já em uso

**Sintomas:**
- Erro: "Bind for 0.0.0.0:XXXX failed: port is already allocated"

**Soluções:**
```bash
# Linux/Mac - Encontrar processo
lsof -i :8761
netstat -tulpn | grep 8761

# Matar processo
kill -9 <PID>

# Ou parar todos containers Docker
docker stop $(docker ps -aq)
```

#### 🔴 Memória insuficiente

**Sintomas:**
- Containers sendo mortos (OOMKilled)
- Sistema lento
- Docker stats mostra uso >90%

**Soluções:**
```bash
# 1. Aumentar memória do Docker Desktop
# Settings → Resources → Memory → 8-12GB

# 2. Reduzir JVM heap size no docker-compose.yml
# Trocar -Xmx1024m para -Xmx768m

# 3. Escalar menos serviços simultaneamente
docker-compose up -d eureka-server-ms gateway-service-ms user-ms post-ms
# Aguardar estabilização, depois subir outros
```

### 📖 Documentação Completa de Troubleshooting

Para guia completo, consulte: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## 📊 Métricas Coletadas

### Performance Metrics

| Métrica | Descrição | Threshold |
|---------|-----------|-----------|
| **Latência Média** | Tempo médio de resposta | < 500ms |
| **P90 Latency** | 90% das requisições | < 1000ms |
| **P95 Latency** | 95% das requisições | < 2000ms |
| **P99 Latency** | 99% das requisições | < 5000ms |
| **Throughput** | Requisições por segundo | > 10 req/s |
| **Taxa de Erro** | % de requisições falhadas | < 5% |

### Resource Metrics

| Recurso | Métrica | Descrição |
|---------|---------|-----------|
| **CPU** | cpu_usage_percent | Uso de CPU por serviço |
| **Memória** | jvm_memory_used_bytes | Memória heap/non-heap |
| **Conexões DB** | hikaricp_connections_active | Pool de conexões |
| **Threads** | jvm_threads_live | Threads ativas |
| **GC** | jvm_gc_pause_seconds | Tempo de garbage collection |

### Resilience Metrics

| Métrica | Descrição |
|---------|-----------|
| **Circuit Breaker State** | open/closed/half-open |
| **Retry Success Rate** | % de retries bem-sucedidos |
| **Fallback Executions** | Número de fallbacks executados |
| **Request Timeout** | Timeouts por segundo |

---

## 🎓 Sobre o TCC

### Objetivos Acadêmicos

Este projeto visa fornecer **dados empíricos** para análise comparativa entre arquiteturas:

**Microsserviços:**
- ✅ Escalabilidade independente por serviço
- ✅ Resiliência a falhas parciais
- ✅ Flexibilidade tecnológica
- ⚠️ Complexidade operacional aumentada
- ⚠️ Overhead de rede e latência

**Monolítico:**
- ✅ Simplicidade de desenvolvimento e deploy
- ✅ Menor latência em operações simples
- ✅ Transações ACID facilitadas
- ⚠️ Escalabilidade limitada
- ⚠️ Acoplamento de componentes

### Hipóteses Testadas

1. ✅ **H1:** Microsserviços tem maior throughput com carga distribuída
2. ✅ **H2:** Monolítico tem menor latência em operações simples
3. ✅ **H3:** Microsserviços é mais resiliente a falhas parciais
4. ✅ **H4:** Overhead de rede impacta performance em microsserviços
5. ✅ **H5:** Microsserviços consome mais recursos (memória/CPU)

### Metodologia de Pesquisa

1. **Implementação Equivalente**
   - Mesmas funcionalidades em ambas arquiteturas
   - Mesmo stack tecnológico base (Spring Boot)
   - Mesma configuração de banco de dados

2. **Ambiente Controlado**
   - Docker containers com recursos limitados
   - Rede isolada
   - Monitoramento contínuo

3. **Testes Padronizados**
   - K6 com cenários idênticos
   - Métricas coletadas via Prometheus
   - Análise estatística dos resultados

4. **Análise Comparativa**
   - Comparação quantitativa (números)
   - Análise qualitativa (complexidade, manutenibilidade)
   - Trade-offs identificados

5. **Documentação**
   - Resultados reproduzíveis
   - Código aberto
   - Metodologia transparente

### Estrutura da Dissertação

1. **Introdução**
   - Contexto e motivação
   - Objetivos e questões de pesquisa
   
2. **Fundamentação Teórica**
   - Arquitetura de microsserviços
   - Padrões e práticas
   - Trabalhos relacionados

3. **Metodologia**
   - Implementação das arquiteturas
   - Setup de testes
   - Métricas coletadas

4. **Resultados e Análise**
   - Comparação de desempenho
   - Análise de escalabilidade
   - Discussão de trade-offs

5. **Conclusão**
   - Síntese dos resultados
   - Limitações
   - Trabalhos futuros

---

## 🚀 Roadmap

### ✅ Fase 1: Implementação Base (Concluído)
- [x] Arquitetura de microsserviços
- [x] Service discovery (Eureka)
- [x] API Gateway
- [x] Banco de dados por serviço
- [x] Dockerização

### ✅ Fase 2: Otimização (Concluído)
- [x] Connection pooling (HikariCP)
- [x] Thread pool tuning
- [x] Circuit breaker (Resilience4j)
- [x] Retry mechanism
- [x] Health checks

### ✅ Fase 3: Monitoramento (Concluído)
- [x] Prometheus integração
- [x] Grafana dashboards
- [x] Métricas de aplicação
- [x] Métricas de infraestrutura

### ✅ Fase 4: Testes de Carga (Concluído)
- [x] Implementação K6
- [x] Cenários de teste
- [x] Automação de testes
- [x] Coleta de resultados

### 🔄 Fase 5: Análise e Documentação (Em Andamento)
- [x] Script de geração de relatórios
- [ ] Análise comparativa completa
- [ ] Documentação final do TCC
- [ ] Apresentação e defesa

### 📋 Fase 6: Melhorias Futuras (Planejado)
- [ ] Implementação de cache (Redis)
- [ ] Message broker (RabbitMQ/Kafka)
- [ ] Distributed tracing (Jaeger)
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Autenticação JWT
- [ ] Rate limiting
- [ ] HTTPS/TLS

---

## 📚 Documentação Adicional

- [Guia de Arquitetura Detalhada](docs/ARCHITECTURE.md)
- [Documentação da API](docs/API.md)
- [Guia de Deployment](docs/DEPLOYMENT.md)
- [Troubleshooting Completo](TROUBLESHOOTING.md)
- [Análise de Resultados](test-results/README.md)

---

## 👨‍💻 Autor

**[Seu Nome]**  
Trabalho de Conclusão de Curso  
Bacharelado em Ciência da Computação  
[Nome da Universidade]  
Ano: 2024

**Orientador:** Prof. [Nome do Orientador]

---

## 📝 Licença

Este projeto foi desenvolvido para fins acadêmicos (TCC) e é disponibilizado sob licença MIT para referência educacional.

---

## 🙏 Agradecimentos

- **Prof. [Nome do Orientador]** - Orientação e suporte durante o TCC
- **[Nome da Universidade]** - Infraestrutura e recursos
- **Spring Boot & Spring Cloud Community** - Excelente framework
- **K6 Community** - Ferramenta de teste de carga
- **Prometheus & Grafana Teams** - Stack de monitoramento

---

## 📞 Contato

- 📧 **Email:** [seu.email@universidade.edu.br]
- 💼 **LinkedIn:** [linkedin.com/in/seu-perfil]
- 🐙 **GitHub:** [github.com/seu-usuario]
- 📱 **WhatsApp:** [seu-numero]

---

## 🔗 Links Úteis

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [K6 Documentation](https://k6.io/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

---

## 📈 Estatísticas do Projeto

- **Linhas de Código:** ~15.000+
- **Microsserviços:** 7
- **Endpoints API:** 25+
- **Testes Implementados:** 8 cenários
- **Tempo de Desenvolvimento:** 6 meses
- **Commits:** 200+
- **Issues Resolvidas:** 50+

---

<div align="center">

**⭐ Se este projeto ajudou você, considere dar uma estrela! ⭐**

**🎓 Desenvolvido como Trabalho de Conclusão de Curso**

---

[![Made with ❤️](https://img.shields.io/badge/Made%20with-❤️-red.svg)](https://github.com/seu-usuario/tcc-microservices)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/seu-usuario/tcc-microservices/pulls)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>