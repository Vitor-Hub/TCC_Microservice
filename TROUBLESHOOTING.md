# 🔧 Guia de Troubleshooting - TCC Microsserviços

Este guia ajuda a resolver problemas comuns durante a execução do projeto.

---

## 📋 Índice

1. [Problemas de Inicialização](#1-problemas-de-inicialização)
2. [Erros de Conectividade](#2-erros-de-conectividade)
3. [Problemas de Memória](#3-problemas-de-memória)
4. [Banco de Dados](#4-banco-de-dados)
5. [Eureka e Service Discovery](#5-eureka-e-service-discovery)
6. [API Gateway](#6-api-gateway)
7. [Testes de Carga K6](#7-testes-de-carga-k6)
8. [Monitoramento](#8-monitoramento)
9. [Comandos Úteis](#9-comandos-úteis)

---

## 1. Problemas de Inicialização

### ❌ Container sai imediatamente após iniciar

**Sintoma:**
```bash
$ docker ps -a
CONTAINER ID   STATUS                     PORTS     NAMES
abc123...      Exited (137) 2 seconds ago           micro-user-service
```

**Causas Comuns:**
- Falta de memória (Exit code 137)
- Erro na aplicação Spring Boot
- Porta já em uso

**Soluções:**

1. **Verificar logs:**
```bash
docker logs micro-user-service
docker logs micro-user-service --tail 100
```

2. **Aumentar memória do Docker:**
   - Docker Desktop → Settings → Resources → Memory: 6-8GB

3. **Verificar portas em uso:**
```bash
# macOS/Linux
lsof -i :18081
netstat -an | grep 18081

# Windows
netstat -an | findstr 18081
```

4. **Reiniciar o container:**
```bash
docker restart micro-user-service
```

---

### ❌ "Cannot connect to the Docker daemon"

**Sintoma:**
```
ERROR: Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solução:**
```bash
# Verificar se Docker está rodando
docker info

# Se não estiver, iniciar Docker Desktop
# ou docker daemon (Linux)
sudo systemctl start docker
```

---

### ❌ Serviços não registram no Eureka

**Sintoma:**
- Eureka Dashboard (http://localhost:8761) não mostra os serviços
- Logs mostram: "DiscoveryClient_... - Registration failed"

**Soluções:**

1. **Verificar se Eureka está saudável:**
```bash
curl http://localhost:8761/actuator/health
```

2. **Verificar logs do Eureka:**
```bash
docker logs microeureka
```

3. **Verificar configuração do serviço:**
```yaml
# Deve ter no application.yml ou application.properties
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
```

4. **Reiniciar em ordem:**
```bash
docker restart microeureka
sleep 30
docker restart micro-api-gateway micro-user-service micro-post-service
```

---

## 2. Erros de Conectividade

### ❌ "Connection refused" entre serviços

**Sintoma:**
```
java.net.ConnectException: Connection refused
```

**Causas:**
- Serviço destino não está rodando
- Firewall bloqueando
- Nome do host incorreto

**Soluções:**

1. **Verificar se todos os containers estão na mesma rede:**
```bash
docker network inspect mstcc-net
```

2. **Testar conectividade entre containers:**
```bash
# Entrar em um container
docker exec -it micro-user-service sh

# Tentar pingar outro serviço
ping post-ms
curl http://post-ms:18082/actuator/health
```

3. **Verificar se a porta está correta:**
```bash
# Listar portas expostas
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

---

### ❌ Gateway retorna 503 Service Unavailable

**Sintoma:**
```bash
$ curl http://localhost:8765/user-ms/api/users
503 Service Unavailable
```

**Causas:**
- Serviço não está registrado no Eureka
- Serviço está unhealthy
- Rota não configurada no Gateway

**Soluções:**

1. **Verificar se serviço está no Eureka:**
```bash
curl http://localhost:8761/eureka/apps | grep USER-MS
```

2. **Verificar health do serviço:**
```bash
curl http://localhost:18081/actuator/health
```

3. **Testar direto no serviço (bypass do Gateway):**
```bash
curl http://localhost:18081/api/users
```

4. **Verificar rotas do Gateway:**
```bash
curl http://localhost:8765/actuator/gateway/routes
```

---

## 3. Problemas de Memória

### ❌ OutOfMemoryError

**Sintoma:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Soluções:**

1. **Aumentar heap da JVM no docker-compose.yml:**
```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m  # Era 384m/256m
```

2. **Aumentar limite do container:**
```yaml
deploy:
  resources:
    limits:
      memory: 768M  # Era 512M
```

3. **Verificar uso de memória:**
```bash
docker stats
```

4. **Analisar heap dump (se disponível):**
```bash
docker exec micro-user-service jmap -heap 1
```

---

### ❌ Sistema lento/travando

**Sintoma:**
- Requisições lentas
- Timeouts frequentes
- CPU alta

**Soluções:**

1. **Verificar recursos:**
```bash
docker stats --no-stream
```

2. **Verificar threads da JVM:**
```bash
docker exec micro-user-service jstack 1
```

3. **Reduzir carga do teste K6:**
```javascript
// No k6-load-test.js, reduzir VUs
vus: 5,  // Era 10
```

4. **Adicionar delays entre requisições:**
```javascript
sleep(randomInt(2, 5));  // Era randomInt(1, 3)
```

---

## 4. Banco de Dados

### ❌ "Connection to localhost:5432 refused"

**Sintoma:**
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Causas:**
- Container do PostgreSQL não está rodando
- String de conexão incorreta

**Soluções:**

1. **Verificar se bancos estão rodando:**
```bash
docker ps | grep _db
```

2. **Iniciar bancos se não estiverem rodando:**
```bash
docker-compose -f docker-compose.db.yml up -d
```

3. **Verificar conexão:**
```bash
docker exec -it user_ms_db psql -U user -d userdb -c "SELECT 1;"
```

4. **Verificar string de conexão no docker-compose.yml:**
```yaml
- SPRING_DATASOURCE_URL=jdbc:postgresql://user-ms-db:5432/userdb
# NÃO usar localhost! Usar nome do container
```

---

### ❌ "password authentication failed"

**Sintoma:**
```
FATAL: password authentication failed for user "user"
```

**Soluções:**

1. **Verificar credenciais no docker-compose.yml e docker-compose.db.yml:**
```yaml
# Devem ser IGUAIS
# docker-compose.db.yml
POSTGRES_USER: user
POSTGRES_PASSWORD: user123

# docker-compose.yml
SPRING_DATASOURCE_USERNAME: user
SPRING_DATASOURCE_PASSWORD: user123
```

2. **Recriar containers do banco:**
```bash
docker-compose -f docker-compose.db.yml down -v
docker-compose -f docker-compose.db.yml up -d
```

---

### ❌ Tabelas não são criadas

**Sintoma:**
- Erro: "relation does not exist"
- Banco vazio

**Soluções:**

1. **Verificar se JPA está configurado para criar tabelas:**
```yaml
# application.yml ou application.properties
spring:
  jpa:
    hibernate:
      ddl-auto: update  # ou create
```

2. **Verificar logs de inicialização do Hibernate:**
```bash
docker logs micro-user-service | grep -i "hibernate"
```

3. **Conectar no banco e verificar:**
```bash
docker exec -it user_ms_db psql -U user -d userdb

# No psql:
\dt   # Listar tabelas
\d users  # Descrever tabela users
```

---

## 5. Eureka e Service Discovery

### ❌ Serviços aparecem como "DOWN" no Eureka

**Sintoma:**
- Eureka Dashboard mostra serviços em vermelho
- Status: DOWN

**Soluções:**

1. **Verificar health endpoint:**
```bash
curl http://localhost:18081/actuator/health
```

2. **Adicionar actuator ao pom.xml (se não tiver):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

3. **Expor health endpoint:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

### ❌ Múltiplas instâncias do mesmo serviço aparecendo

**Sintoma:**
- Eureka mostra USER-MS (2) ou mais instâncias

**Causa:**
- Containers antigos não foram removidos

**Solução:**
```bash
docker ps -a | grep user_service
docker rm -f $(docker ps -a | grep user_service | awk '{print $1}')
docker-compose up -d user-ms
```

---

## 6. API Gateway

### ❌ CORS Errors

**Sintoma:**
```
Access to XMLHttpRequest at 'http://localhost:8765/...' has been blocked by CORS policy
```

**Solução:**

Adicionar configuração CORS no Gateway:

```java
@Configuration
public class CorsConfiguration {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        
        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
```

---

### ❌ Gateway não roteia corretamente

**Sintoma:**
- 404 Not Found ao acessar via Gateway
- Funciona acessando serviço diretamente

**Soluções:**

1. **Verificar rotas configuradas:**
```bash
curl http://localhost:8765/actuator/gateway/routes | jq
```

2. **Testar padrão de rota:**
```bash
# Padrão: /SERVICE-NAME/**
curl http://localhost:8765/user-ms/api/users
```

3. **Verificar application.yml do Gateway:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-ms
          predicates:
            - Path=/user-ms/**
          filters:
            - StripPrefix=1
```

---

## 7. Testes de Carga K6

### ❌ "ECONNREFUSED" errors no K6

**Sintoma:**
```
ERRO[0010] GoError: Get "http://localhost:8765/...": dial tcp: connect: connection refused
```

**Soluções:**

1. **Verificar se Gateway está rodando:**
```bash
curl http://localhost:8765/actuator/health
```

2. **Aguardar serviços estabilizarem antes de testar:**
```bash
./validate-environment.sh
```

3. **Aumentar timeout no K6:**
```javascript
const params = {
  timeout: '30s',  // Era 10s
};
```

---

### ❌ Taxa de erro muito alta (>10%)

**Sintoma:**
```
http_req_failed............: 15.32%
```

**Causas:**
- Carga muito alta
- Serviços não escalados
- Recursos insuficientes

**Soluções:**

1. **Reduzir VUs:**
```javascript
vus: 10,  // Reduzir de 20
```

2. **Aumentar recursos dos containers:**
```yaml
deploy:
  resources:
    limits:
      memory: 1024M  # Era 512M
```

3. **Adicionar delay entre requisições:**
```javascript
sleep(randomInt(3, 6));
```

---

## 8. Monitoramento

### ❌ Prometheus não coleta métricas

**Sintoma:**
- Prometheus targets aparecem como "DOWN"
- Dashboards do Grafana sem dados

**Soluções:**

1. **Verificar targets no Prometheus:**
```bash
open http://localhost:9090/targets
```

2. **Verificar se Micrometer está no pom.xml:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

3. **Expor endpoint prometheus:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```

4. **Testar endpoint manualmente:**
```bash
curl http://localhost:18081/actuator/prometheus
```

---

### ❌ Grafana não conecta ao Prometheus

**Sintoma:**
- Grafana mostra "Data source error"

**Soluções:**

1. **Verificar se Prometheus está acessível:**
```bash
curl http://prometheus:9090/api/v1/query?query=up
```

2. **Reconfigurar datasource no Grafana:**
   - URL: `http://prometheus:9090`
   - Access: Server (default)

---

## 9. Comandos Úteis

### Verificações Rápidas

```bash
# Status de todos os containers
docker ps -a

# Logs em tempo real
docker logs -f micro-user-service

# Executar comando dentro do container
docker exec -it micro-user-service sh

# Ver uso de recursos
docker stats

# Listar redes
docker network ls

# Inspecionar rede
docker network inspect mstcc-net

# Listar volumes
docker volume ls
```

### Limpeza e Reset

```bash
# Parar todos os containers
docker-compose down

# Remover volumes (CUIDADO: apaga dados!)
docker-compose down -v

# Remover imagens antigas
docker image prune -a

# Reset completo
docker-compose down -v
docker system prune -a --volumes
docker-compose build --no-cache
docker-compose up -d
```

### Debugging

```bash
# Ver últimas 100 linhas de log
docker logs micro-user-service --tail 100

# Seguir logs em tempo real
docker logs -f micro-user-service

# Ver logs de todos os containers
docker-compose logs -f

# Ver logs de um serviço específico
docker-compose logs -f user-ms

# Exportar logs para arquivo
docker logs micro-user-service > logs.txt 2>&1
```

### Testes Manuais

```bash
# Testar health
curl http://localhost:8765/actuator/health

# Testar criação de usuário
curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","bio":"Testing"}'

# Listar usuários
curl http://localhost:8765/user-ms/api/users

# Ver métricas do Prometheus
curl http://localhost:9090/api/v1/query?query=up
```

---

## 🆘 Ainda com Problemas?

1. **Execute o script de validação:**
```bash
chmod +x validate-environment.sh
./validate-environment.sh
```

2. **Verifique os logs detalhados:**
```bash
docker-compose logs > full-logs.txt
```

3. **Procure por palavras-chave nos logs:**
```bash
docker-compose logs | grep -i "error\|exception\|failed"
```

4. **Verifique a documentação do Spring:**
   - [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
   - [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

5. **Consulte issues similares:**
   - [Stack Overflow - Spring Cloud](https://stackoverflow.com/questions/tagged/spring-cloud)
   - [GitHub Issues - Spring Cloud](https://github.com/spring-cloud)

---

**Última atualização:** 2024  
**Para o TCC de Ciência da Computação**
