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

### Serviços Implementados

```
┌─────────────────────────────────────────────────────────────┐
│                     API GATEWAY (8765)                       │
│              Roteamento e Load Balancing                     │
└────────────┬────────────────────────────────────────────────┘
             │
    ┌────────┴─────────────────────────────────────┐
    │        EUREKA SERVER (8761)                   │
    │          Service Discovery                    │
    └────────┬─────────────────────────────────────┘
             │
     ┌───────┴──────┬─────────┬──────────┬──────────┐
     │              │         │          │          │
┌────▼────┐  ┌─────▼───┐ ┌──▼─────┐ ┌──▼─────┐ ┌──▼────────┐
│ User MS │  │ Post MS │ │ Like MS│ │Comment │ │Friendship │
│ (18081) │  │ (18082) │ │ (18085)│ │   MS   │ │    MS     │
│         │  │         │ │        │ │ (18083)│ │  (18084)  │
└────┬────┘  └────┬────┘ └───┬────┘ └───┬────┘ └─────┬─────┘
     │            │          │          │            │
┌────▼────┐  ┌───▼────┐ ┌───▼────┐ ┌──▼─────┐ ┌────▼─────┐
│User DB  │  │Post DB │ │Like DB │ │Comment │ │Friendship│
│ (5433)  │  │ (5434) │ │ (5436) │ │   DB   │ │    DB    │
│         │  │        │ │        │ │ (5435) │ │  (5437)  │
└─────────┘  └────────┘ └────────┘ └────────┘ └──────────┘
```

### 📦 Componentes

| Componente | Porta | Descrição | Tecnologia |
|------------|-------|-----------|------------|
| **Eureka Server** | 8761 | Service Discovery e Registry | Spring Cloud Netflix |
| **API Gateway** | 8765 | Roteamento, Load Balancing | Spring Cloud Gateway |
| **User Service** | 18081 | Gerenciamento de usuários | Spring Boot + JPA |
| **Post Service** | 18082 | Publicações e posts | Spring Boot + JPA |
| **Comment Service** | 18083 | Comentários em posts | Spring Boot + JPA |
| **Like Service** | 18085 | Sistema de likes | Spring Boot + JPA |
| **Friendship Service** | 18084 | Rede de amizades | Spring Boot + JPA |
| **PostgreSQL DBs** | 5433-5437 | Bancos independentes | PostgreSQL 15 |
| **Prometheus** | 9090 | Coleta de métricas | Prometheus |
| **Grafana** | 3000 | Visualização e dashboards | Grafana |

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 17** - Linguagem de programação
- **Spring Boot 3.2.3** - Framework principal
- **Spring Cloud** - Microsserviços (Eureka, Gateway)
- **Spring Data JPA** - Persistência
- **PostgreSQL 15** - Banco de dados relacional

### Infraestrutura
- **Docker** + **Docker Compose** - Containerização
- **Maven** - Build e gerenciamento de dependências

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
- ✅ **K6** (para testes de carga)
- ✅ **Python 3** (para geração de relatórios)

**Recursos recomendados do Docker:**
- RAM: 6-8 GB
- CPU: 4+ cores

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

### Opção 1: Execução Automática Completa

```bash
# 1. Build de todos os microsserviços
./build-all.sh

# 2. Subir toda a infraestrutura
docker-compose -f docker-compose.db.yml up -d
docker-compose up -d
docker-compose -f docker-compose.monitoring.yml up -d

# 3. Aguardar inicialização (2-3 minutos)
sleep 120

# 4. Validar ambiente
./validate-environment.sh

# 5. Executar testes de carga
./run-load-test.sh microservices

# 6. Gerar relatório
python3 generate-report.py test-results/
```

### Opção 2: Passo a Passo Manual

#### 1️⃣ Build dos Microsserviços

```bash
# Build individual de cada serviço
cd eureka-server-ms && mvn clean package -DskipTests && cd ..
cd gateway-service-ms && mvn clean package -DskipTests && cd ..
cd user-ms && mvn clean package -DskipTests && cd ..
cd post-ms && mvn clean package -DskipTests && cd ..
cd comment-ms && mvn clean package -DskipTests && cd ..
cd like-ms && mvn clean package -DskipTests && cd ..
cd friendship-ms && mvn clean package -DskipTests && cd ..
```

#### 2️⃣ Subir Bancos de Dados

```bash
docker-compose -f docker-compose.db.yml up -d

# Aguardar bancos estarem prontos
docker ps | grep _db
```

#### 3️⃣ Subir Microsserviços

```bash
docker-compose up -d

# Verificar status
docker ps
```

#### 4️⃣ Subir Monitoramento (Opcional)

```bash
docker-compose -f docker-compose.monitoring.yml up -d

# Acessar Grafana
open http://localhost:3000  # usuário: admin, senha: admin
```

#### 5️⃣ Validar Ambiente

```bash
./validate-environment.sh
```

Se tudo estiver ✅ verde, prossiga para os testes!

#### 6️⃣ Testar Manualmente

```bash
# Criar usuário
curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@test.com",
    "bio": "Desenvolvedor Full Stack"
  }'

# Listar usuários
curl http://localhost:8765/user-ms/api/users

# Verificar Eureka
open http://localhost:8761

# Ver métricas Prometheus
open http://localhost:9090
```

---

## 🧪 Executar Testes de Carga

### Teste Completo Automatizado

```bash
./run-load-test.sh microservices
```

Este script:
1. ✅ Valida que todos os serviços estão rodando
2. ⏳ Aguarda estabilização (30s)
3. 🚀 Executa teste K6 (~18 minutos)
4. 📊 Coleta métricas do sistema
5. 💾 Salva resultados em `test-results/`

### Teste Manual com K6

```bash
# Teste básico (2 minutos)
k6 run --vus 10 --duration 2m k6-load-test.js

# Teste com opções customizadas
k6 run \
  --out json=results.json \
  --summary-export=summary.json \
  k6-load-test.js

# Teste com variáveis de ambiente
k6 run -e BASE_URL=http://localhost:8765 k6-load-test.js
```

### Cenários de Teste Implementados

O script K6 executa 5 cenários diferentes:

| Cenário | Duração | VUs | Objetivo |
|---------|---------|-----|----------|
| **Baseline** | 2 min | 5 | Carga constante baixa |
| **Steady** | 3 min | 20 | Carga constante média |
| **Stress** | 8 min | 0→150 | Rampa progressiva |
| **Spike** | 2.5 min | 10→200→10 | Picos repentinos |
| **Read Heavy** | 2 min | 30 | Operações de leitura |

**Duração total:** ~18 minutos

---

## 📊 Gerar Relatórios

### Relatório Comparativo (Microsserviços vs Monolítico)

```bash
python3 generate-report.py test-results/ \
  --micro microservices_20240101_summary.json \
  --mono monolithic_20240101_summary.json
```

### Relatório Individual (Apenas Microsserviços)

```bash
python3 generate-report.py test-results/
```

O script gera:
- 📄 Relatório Markdown detalhado
- 📈 Gráficos ASCII no terminal
- 📊 Tabelas comparativas
- 💡 Análise e recomendações

---

## 📈 Monitoramento

### Prometheus

Acesse: http://localhost:9090

**Queries úteis:**

```promql
# Requests por segundo
rate(http_server_requests_seconds_count[1m])

# Latência P95 por endpoint
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Uso de memória JVM
jvm_memory_used_bytes{area="heap"}

# Taxa de erro
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### Grafana

Acesse: http://localhost:3000
- **Usuário:** admin
- **Senha:** admin

**Dashboards disponíveis:**
- Spring Boot 2.1 Statistics
- JVM Micrometer
- Prometheus 2.0 Stats

---

## 📁 Estrutura do Projeto

```
tcc-microservices/
├── eureka-server-ms/          # Service Discovery
├── gateway-service-ms/        # API Gateway
├── user-ms/                   # Microsserviço de Usuários
├── post-ms/                   # Microsserviço de Posts
├── comment-ms/                # Microsserviço de Comentários
├── like-ms/                   # Microsserviço de Likes
├── friendship-ms/             # Microsserviço de Amizades
├── monitoring/                # Configurações Prometheus/Grafana
│   ├── prometheus.yml
│   └── grafana/
├── test-results/              # Resultados dos testes K6
├── docker-compose.yml         # Microsserviços
├── docker-compose.db.yml      # Bancos de dados
├── docker-compose.monitoring.yml  # Monitoramento
├── k6-load-test.js           # Script de teste de carga
├── run-load-test.sh          # Executor de testes
├── validate-environment.sh   # Validador de ambiente
├── generate-report.py        # Gerador de relatórios
├── TROUBLESHOOTING.md        # Guia de resolução de problemas
└── README.md                 # Este arquivo
```

---

## 🔧 Scripts Utilitários

| Script | Descrição | Uso |
|--------|-----------|-----|
| `build-all.sh` | Build de todos os microsserviços | `./build-all.sh` |
| `validate-environment.sh` | Valida configuração completa | `./validate-environment.sh` |
| `run-load-test.sh` | Executa testes de carga | `./run-load-test.sh microservices` |
| `generate-report.py` | Gera relatórios comparativos | `python3 generate-report.py test-results/` |
| `clean-project.sh` | Limpa arquivos temporários | `./clean-project.sh` |

---

## 🌐 Endpoints da API

Todos os endpoints são acessados via **API Gateway** (`http://localhost:8765`)

### User Service

```bash
# Criar usuário
POST /user-ms/api/users
{
  "name": "string",
  "email": "string",
  "bio": "string"
}

# Listar usuários
GET /user-ms/api/users

# Buscar usuário
GET /user-ms/api/users/{id}

# Atualizar usuário
PUT /user-ms/api/users/{id}

# Deletar usuário
DELETE /user-ms/api/users/{id}
```

### Post Service

```bash
# Criar post
POST /post-ms/api/posts
{
  "user": { "id": 1 },
  "content": "string"
}

# Listar posts
GET /post-ms/api/posts

# Posts de um usuário
GET /post-ms/api/posts/user/{userId}
```

### Comment Service

```bash
# Criar comentário
POST /comment-ms/api/comments
{
  "postId": 1,
  "userId": 1,
  "content": "string"
}

# Comentários de um post
GET /comment-ms/api/comments/post/{postId}
```

### Like Service

```bash
# Dar like
POST /like-ms/api/likes
{
  "postId": 1,
  "userId": 1,
  "commentId": null
}

# Listar likes de um post
GET /like-ms/api/likes/post/{postId}
```

### Friendship Service

```bash
# Criar amizade
POST /friendship-ms/api/friendships
{
  "userId1": 1,
  "userId2": 2,
  "status": "ACCEPTED"
}

# Amigos de um usuário
GET /friendship-ms/api/friendships/user/{userId}
```

---

## 🐛 Troubleshooting

### Problemas Comuns

**🔴 Serviços não iniciam:**
```bash
# Verificar logs
docker logs micro_user_service

# Aumentar memória do Docker
Docker Desktop → Settings → Resources → Memory: 6-8GB
```

**🔴 Eureka não mostra serviços:**
```bash
# Reiniciar em ordem
docker restart microeureka
sleep 30
docker restart micro_api_gateway micro_user_service
```

**🔴 Gateway retorna 503:**
```bash
# Verificar registro no Eureka
curl http://localhost:8761/eureka/apps

# Testar direto no serviço
curl http://localhost:18081/api/users
```

**📖 Guia completo:** Veja [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## 📊 Métricas Coletadas

### Performance
- ⏱️ Latência (média, mediana, P90, P95, P99)
- 🔄 Throughput (requisições/segundo)
- 📈 Taxa de sucesso/erro
- ⚡ Tempo de resposta por operação

### Recursos
- 💾 Uso de memória (heap, non-heap)
- 🔥 CPU por serviço
- 🌐 Transferência de dados (MB sent/received)
- 🔌 Conexões de rede

### Resiliência
- ✅ Taxa de disponibilidade
- 🔁 Recuperação de falhas
- ⚠️ Degradação graceful

---

## 📚 Documentação Adicional

- [Arquitetura Detalhada](docs/ARCHITECTURE.md) *(a criar)*
- [Guia de Contribuição](docs/CONTRIBUTING.md) *(a criar)*
- [API Documentation](docs/API.md) *(a criar)*
- [Troubleshooting](TROUBLESHOOTING.md) ✅

---

## 🎓 Sobre o TCC

### Objetivos Acadêmicos

Este projeto visa fornecer dados empíricos para análise comparativa entre:

**Microsserviços:**
- Vantagens em escalabilidade e autonomia
- Complexidade operacional aumentada
- Trade-offs de performance vs. flexibilidade

**Monolítico:**
- Simplicidade de desenvolvimento e deploy
- Menor latência em operações simples
- Limitações de escalabilidade

### Hipóteses Testadas

1. ✅ Microsserviços tem maior throughput com carga distribuída
2. ✅ Monolítico tem menor latência em operações simples
3. ✅ Microsserviços é mais resiliente a falhas parciais
4. ✅ Overhead de rede impacta performance em microsserviços

### Metodologia

1. **Implementação** das duas arquiteturas com funcionalidades equivalentes
2. **Testes de carga** controlados com K6
3. **Coleta de métricas** automatizada (Prometheus)
4. **Análise estatística** dos resultados
5. **Documentação** de trade-offs e recomendações

---

## 👨‍💻 Autor

**[Seu Nome]**  
Trabalho de Conclusão de Curso  
Bacharelado em Ciência da Computação  
[Nome da Universidade]  
2024

---

## 📝 Licença

Este projeto foi desenvolvido para fins acadêmicos (TCC).

---

## 🙏 Agradecimentos

- Prof. [Nome do Orientador] - Orientação do TCC
- [Nome da Universidade] - Infraestrutura e suporte
- Spring Boot & Spring Cloud Community
- K6 Load Testing Community
- Prometheus & Grafana Teams

---

## 📞 Contato

- 📧 Email: [seu.email@universidade.edu.br]
- 💼 LinkedIn: [seu-perfil]
- 🐙 GitHub: [seu-usuario]

---

<div align="center">

**⭐ Se este projeto ajudou você, considere dar uma estrela! ⭐**

</div>
