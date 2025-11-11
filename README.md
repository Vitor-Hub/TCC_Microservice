# 🚀 TCC: Microsserviços vs Monolítico

Projeto de comparação de desempenho entre arquiteturas de software para Trabalho de Conclusão de Curso.

---

## 📋 Sobre o Projeto

Este projeto implementa uma **rede social simplificada** em duas arquiteturas diferentes:
- **Microsserviços** (este repositório)
- **Monolítico** (repositório separado)

O objetivo é realizar testes de carga e comparar métricas de desempenho como:
- ⏱️ Tempo de resposta
- 🔄 Taxa de requisições/segundo
- 💾 Uso de memória
- 🔥 Latência sob carga

---

## 🏗️ Arquitetura de Microsserviços

### Serviços Implementados:

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **Eureka Server** | 8761 | Service Discovery |
| **API Gateway** | 8765 | Roteamento e Load Balancing |
| **User Service** | 18081 | Gerenciamento de usuários |
| **Post Service** | 18082 | Publicações |
| **Comment Service** | 18083 | Comentários |
| **Like Service** | 18085 | Curtidas |
| **Friendship Service** | 18084 | Amizades |

### Bancos de Dados:

Cada microsserviço possui seu próprio banco PostgreSQL:
- `user-ms-db` (porta 5433)
- `post-ms-db` (porta 5434)
- `comment-ms-db` (porta 5435)
- `like-ms-db` (porta 5436)
- `friendship-ms-db` (porta 5437)

---

## 🛠️ Tecnologias Utilizadas

- **Java 17** + **Spring Boot 3.2.3**
- **Spring Cloud** (Netflix Eureka, Spring Cloud Gateway)
- **PostgreSQL 15**
- **Docker** + **Docker Compose**
- **Maven**
- **K6** (testes de carga)
- **Prometheus** + **Grafana** (monitoramento)

---

## 🚀 Como Executar

### **Pré-requisitos:**
- Docker Desktop instalado
- Maven 3.8+
- Java 17+
- K6 (para testes de carga)

### **1. Build dos Microsserviços:**

```bash
# Build de todos os serviços
./build-all.sh

# Ou manualmente:
cd user-ms && mvn clean package -DskipTests && cd ..
cd post-ms && mvn clean package -DskipTests && cd ..
cd comment-ms && mvn clean package -DskipTests && cd ..
cd like-ms && mvn clean package -DskipTests && cd ..
cd friendship-ms && mvn clean package -DskipTests && cd ..
cd gateway-service-ms && mvn clean package -DskipTests && cd ..
cd eureka-server-ms && mvn clean package -DskipTests && cd ..
```

### **2. Subir os Bancos de Dados:**

```bash
docker-compose -f docker-compose.db.yml up -d
```

### **3. Subir os Microsserviços:**

```bash
docker-compose up -d
```

### **4. Verificar se está funcionando:**

```bash
# Status dos containers
docker ps

# Eureka Dashboard
open http://localhost:8761

# Testar API
curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"João Silva","email":"joao@test.com","bio":"Desenvolvedor"}'
```

---

## 📊 Monitoramento (Opcional)

```bash
# Subir Prometheus + Grafana
docker-compose -f docker-compose.monitoring.yml up -d

# Acessar
open http://localhost:3000  # Grafana (admin/admin)
open http://localhost:9090  # Prometheus
```

---

## 🧪 Executar Testes de Carga

```bash
# Executar teste K6
./run-load-test.sh

# Ou manualmente:
k6 run k6-load-test.js

# Gerar relatório
python3 generate-report.py
```

### Cenários de Teste:

O teste K6 simula:
1. **Carga Constante**: 10 usuários por 2 minutos
2. **Rampa Progressiva**: 0 → 150 usuários em 7 minutos
3. **Teste de Pico**: Picos repentinos de 200 usuários

---

## 📁 Estrutura do Projeto

```
.
├── user-ms/              # Microsserviço de usuários
├── post-ms/              # Microsserviço de posts
├── comment-ms/           # Microsserviço de comentários
├── like-ms/              # Microsserviço de likes
├── friendship-ms/        # Microsserviço de amizades
├── gateway-service-ms/   # API Gateway
├── eureka-server-ms/     # Service Discovery
├── docker-compose.yml    # Microsserviços
├── docker-compose.db.yml # Bancos de dados
├── docker-compose.monitoring.yml # Prometheus + Grafana
├── k6-load-test.js       # Script de testes de carga
├── run-load-test.sh      # Executor de testes
└── generate-report.py    # Gerador de relatórios
```

---

## 🔧 Scripts Úteis

| Script | Descrição |
|--------|-----------|
| `build-all.sh` | Build de todos os microsserviços |
| `clean-project.sh` | Limpa arquivos temporários |
| `clean-targets.sh` | Remove pastas target/ |
| `run-load-test.sh` | Executa testes de carga K6 |

---

## 📈 Endpoints da API

### **User Service** (via Gateway: `http://localhost:8765/user-ms`)

```bash
# Criar usuário
POST /api/users
{
  "name": "João Silva",
  "email": "joao@test.com",
  "bio": "Desenvolvedor"
}

# Listar usuários
GET /api/users

# Buscar usuário
GET /api/users/{id}
```

### **Post Service** (via Gateway: `http://localhost:8765/post-ms`)

```bash
# Criar post
POST /api/posts
{
  "user": { "id": 1 },
  "content": "Meu primeiro post!"
}

# Listar posts
GET /api/posts

# Posts de um usuário
GET /api/posts/user/{userId}
```

### **Comment, Like e Friendship Services**

Endpoints similares disponíveis via Gateway nas rotas:
- `/comment-ms/api/comments`
- `/like-ms/api/likes`
- `/friendship-ms/api/friendships`

---

## 🐛 Troubleshooting

### Serviços não iniciam:

```bash
# Verificar logs
docker logs micro_user_service
docker logs micro_api_gateway

# Verificar Eureka
open http://localhost:8761
```

### Erro de memória (Exit 137):

```bash
# Aumentar memória do Docker Desktop
# Settings → Resources → Memory: 6-8GB

# Ou limitar memória dos serviços (já configurado no docker-compose.yml)
```

### Bancos de dados com erro:

```bash
# Resetar volumes
docker-compose -f docker-compose.db.yml down -v
docker-compose -f docker-compose.db.yml up -d
```

---

## 👨‍💻 Autor

**Seu Nome** - TCC Ciência da Computação

---

## 📝 Licença

Este projeto foi desenvolvido para fins acadêmicos (TCC).

---

## 🙏 Agradecimentos

- Orientador(a): [Nome]
- Instituição: [Nome da Universidade]
- Spring Boot & Spring Cloud Community