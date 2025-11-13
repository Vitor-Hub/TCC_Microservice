# ⚡ Guia de Início Rápido - 5 Minutos

Este guia te ajuda a ter o projeto rodando o mais rápido possível.

---

## ⏱️ Timeline

- ⚙️ **Setup:** 2 minutos
- 🔨 **Build:** 5-10 minutos
- 🚀 **Inicialização:** 2-3 minutos
- ✅ **Validação:** 1 minuto
- 🧪 **Testes:** 18 minutos
- 📊 **Relatório:** 1 minuto

**Total:** ~30 minutos do zero até ter os resultados

---

## 📋 Checklist Inicial

Antes de começar, certifique-se de ter:

- [ ] Docker Desktop instalado e rodando
- [ ] Java 17+ instalado
- [ ] Maven 3.8+ instalado
- [ ] K6 instalado ([https://k6.io/docs/getting-started/installation/](https://k6.io/docs/getting-started/installation/))
- [ ] Python 3 instalado
- [ ] Pelo menos 6GB RAM disponível para Docker

**Testar instalações:**
```bash
docker --version
java -version
mvn -version
k6 version
python3 --version
```

---

## 🚀 Comandos Rápidos

### 1️⃣ Clone e Prepare (30 segundos)

```bash
git clone https://github.com/seu-usuario/tcc-microservices.git
cd tcc-microservices
chmod +x *.sh
```

### 2️⃣ Build Todos os Serviços (5-10 minutos)

```bash
./build-all.sh
```

☕ Aproveite para tomar um café enquanto compila...

### 3️⃣ Subir Infraestrutura (2 minutos)

```bash
# Bancos de dados
docker-compose -f docker-compose.db.yml up -d

# Microsserviços
docker-compose up -d

# Monitoramento (opcional)
docker-compose -f docker-compose.monitoring.yml up -d

# Aguardar inicialização
sleep 120
```

### 4️⃣ Validar Ambiente (30 segundos)

```bash
./validate-environment.sh
```

✅ Se tudo estiver verde, prossiga!

### 5️⃣ Teste Manual Rápido (30 segundos)

```bash
# Criar um usuário
curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","bio":"Testing"}'

# Listar usuários
curl http://localhost:8765/user-ms/api/users

# Abrir Eureka
open http://localhost:8761
```

### 6️⃣ Executar Testes de Carga (18 minutos)

```bash
./run-load-test.sh microservices
```

### 7️⃣ Gerar Relatório (1 minuto)

```bash
python3 generate-report.py test-results/
```

---

## 🎯 Comandos Únicos (One-Liners)

### Setup Completo em Um Comando

```bash
./build-all.sh && \
docker-compose -f docker-compose.db.yml up -d && \
docker-compose up -d && \
sleep 120 && \
./validate-environment.sh
```

### Reiniciar Tudo

```bash
docker-compose down && \
docker-compose -f docker-compose.db.yml down && \
docker-compose -f docker-compose.db.yml up -d && \
docker-compose up -d
```

### Limpar e Recomeçar do Zero

```bash
docker-compose down -v && \
docker-compose -f docker-compose.db.yml down -v && \
docker system prune -f && \
./build-all.sh && \
docker-compose -f docker-compose.db.yml up -d && \
docker-compose up -d
```

---

## 🔍 Verificações Rápidas

### Status dos Serviços

```bash
docker ps
```

**Esperado:** 12 containers rodando

### Logs em Tempo Real

```bash
# Todos os logs
docker-compose logs -f

# Apenas um serviço
docker logs -f micro-user-service
```

### Testar Endpoints Manualmente

```bash
# Health do Gateway
curl http://localhost:8765/actuator/health

# Health do Eureka
curl http://localhost:8761/actuator/health

# Criar e listar usuário
USER_ID=$(curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Quick Test","email":"quick@test.com","bio":"Quick start test"}' \
  -s | jq -r '.id')

curl http://localhost:8765/user-ms/api/users/$USER_ID
```

---

## 📊 Acessar Dashboards

```bash
# Eureka Dashboard
open http://localhost:8761

# Prometheus
open http://localhost:9090

# Grafana (admin/admin)
open http://localhost:3000
```

---

## ⚠️ Problemas Comuns

### Container sai imediatamente

```bash
# Ver logs
docker logs micro-user-service

# Aumentar memória do Docker
# Docker Desktop → Settings → Resources → Memory: 6-8GB
```

### Porta já em uso

```bash
# Descobrir quem está usando
lsof -i :8765

# Matar processo
kill -9 <PID>
```

### Serviços não aparecem no Eureka

```bash
# Reiniciar Eureka e aguardar
docker restart microeureka
sleep 30

# Reiniciar serviços
docker-compose restart
```

### Build falha

```bash
# Limpar e tentar novamente
mvn clean
./build-all.sh
```

---

## 🛟 Precisa de Ajuda?

1. **Validação automática:**
   ```bash
   ./validate-environment.sh
   ```

2. **Troubleshooting completo:**
   - Consulte: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

3. **Ver README completo:**
   - Consulte: [README.md](README.md)

---

## 📈 Próximos Passos

Após ter tudo rodando:

1. ✅ Execute o teste de validação
2. 🧪 Rode os testes de carga
3. 📊 Gere os relatórios
4. 📖 Leia o relatório gerado em Markdown
5. 🎓 Use os dados no seu TCC!

---

## 🎯 Checklist Final

- [ ] Todos os containers estão rodando (`docker ps`)
- [ ] Eureka mostra todos os serviços (http://localhost:8761)
- [ ] Gateway está respondendo (http://localhost:8765/actuator/health)
- [ ] Consegue criar um usuário via API
- [ ] Prometheus está coletando métricas (http://localhost:9090)
- [ ] Grafana está acessível (http://localhost:3000)
- [ ] Script de validação passou ✅

---

## 🏁 Pronto!

Se chegou até aqui com tudo ✅, você está pronto para:

```bash
# Executar os testes
./run-load-test.sh microservices

# Gerar relatório
python3 generate-report.py test-results/

# Ver o relatório
cat test-results/relatorio_*.md
```

**Boa sorte com seu TCC! 🎓**

---

*Última atualização: 2024*
