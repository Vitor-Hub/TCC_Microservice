#!/bin/bash
set -e

# ==============================
#  Microsservice Startup Script
# ==============================
# Autor: Vitor Cerqueira
# Objetivo: Subir toda a stack (MySQL, Eureka, Microserviços, Monitoramento)
# ==============================================

YELLOW='\033[1;33m'
GREEN='\033[1;32m'
RED='\033[1;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🚀 Iniciando ambiente Microsservice...${NC}"
cd "$(dirname "$0")/.." || exit 1

# ==============================
#  1️⃣ Rede docker
# ==============================
echo -e "${YELLOW}🧱 Criando rede tcc-network (se ainda não existir)...${NC}"
docker network create tcc-network >/dev/null 2>&1 || true

# ==============================
#  2️⃣ MySQL e Eureka
# ==============================
echo -e "${YELLOW}🐬 Subindo MySQL e Eureka...${NC}"
docker compose -f docker-compose.app.yml up -d mysql eureka-server

# Espera o MySQL responder
echo -e "${YELLOW}⏳ Aguardando MySQL ficar pronto...${NC}"
until docker exec tcc_mysql mysqladmin ping -proot --silent &>/dev/null; do
  echo "   Aguardando MySQL..."
  sleep 5
done
echo -e "${GREEN}✅ MySQL está pronto.${NC}"

# Espera o Eureka responder
echo -e "${YELLOW}⏳ Aguardando Eureka na porta 8761...${NC}"
until curl -s http://localhost:8761 >/dev/null; do
  echo "   Aguardando Eureka..."
  sleep 5
done
echo -e "${GREEN}✅ Eureka está pronto.${NC}"

# ==============================
#  3️⃣ Microsserviços
# ==============================
echo -e "${YELLOW}📦 Subindo microsserviços...${NC}"
docker compose -f docker-compose.app.yml up -d user-ms post-ms comment-ms like-ms friendship-ms gateway-service

# ==============================
#  4️⃣ Monitoramento (Prometheus, Grafana, Node Exporter, cAdvisor)
# ==============================
echo -e "${YELLOW}📊 Subindo stack de monitoramento...${NC}"
docker compose -f docker-compose.monitoring.yml up -d

# ==============================
#  5️⃣ Status final
# ==============================
sleep 5
echo -e "\n${GREEN}🎯 Ambiente iniciado com sucesso!${NC}\n"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n${YELLOW}🌐 Endpoints principais:${NC}"
echo -e " - Eureka:      ${GREEN}http://localhost:8761${NC}"
echo -e " - Gateway:     ${GREEN}http://localhost:8765${NC}"
echo -e " - Grafana:     ${GREEN}http://localhost:3000${NC} (login: admin / admin)"
echo -e " - Prometheus:  ${GREEN}http://localhost:9090${NC}"
echo -e " - cAdvisor:    ${GREEN}http://localhost:8080${NC}\n"

echo -e "${YELLOW}Para encerrar todos os containers:${NC}  docker compose -f docker-compose.app.yml down && docker compose -f docker-compose.monitoring.yml down"
