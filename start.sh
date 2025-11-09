#!/bin/bash
set -e

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧹 Cleaning up old containers and volumes...${NC}"
docker compose down -v --remove-orphans 2>/dev/null || true

echo -e "${BLUE}🔨 Building all images...${NC}"
docker compose build --no-cache

echo -e "${BLUE}🚀 Starting all services...${NC}"
docker compose up -d

echo ""
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"
echo ""

# Função para verificar health
check_health() {
  local container=$1
  local max_attempts=60
  local attempt=1
  
  while [ $attempt -le $max_attempts ]; do
    health=$(docker inspect --format='{{.State.Health.Status}}' $container 2>/dev/null || echo "starting")
    
    if [ "$health" == "healthy" ]; then
      echo -e "${GREEN}✅ $container is healthy${NC}"
      return 0
    fi
    
    echo -e "   ⏳ $container: $health (attempt $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
  done
  
  echo -e "${YELLOW}⚠️  $container did not become healthy in time${NC}"
  return 1
}

# Verificar MySQL
check_health "tcc_mysql"

# Verificar Eureka
check_health "microeureka"

# Aguardar mais um pouco para Eureka estabilizar
echo -e "${YELLOW}⏳ Waiting additional 15s for Eureka to fully stabilize...${NC}"
sleep 15

# Verificar Gateway
check_health "micro_api_gateway"

# Verificar microsserviços
check_health "micro_user_service"
check_health "micro_post_service"
check_health "micro_comment_service"
check_health "micro_friendship_service"
check_health "micro_like_service"

echo ""
echo -e "${GREEN}✅ All services are up and running!${NC}"
echo ""
echo -e "${BLUE}📊 Service URLs:${NC}"
echo "   🌐 Eureka Dashboard  → http://localhost:8761"
echo "   🚪 API Gateway       → http://localhost:8765"
echo "   👤 User Service      → http://localhost:18081/actuator/health"
echo "   📝 Post Service      → http://localhost:18082/actuator/health"
echo "   💬 Comment Service   → http://localhost:18083/actuator/health"
echo "   🤝 Friendship Service → http://localhost:18084/actuator/health"
echo "   ❤️  Like Service      → http://localhost:18085/actuator/health"
echo ""
echo -e "${BLUE}📈 Prometheus Metrics:${NC}"
echo "   http://localhost:18081/actuator/prometheus"
echo ""
echo -e "${BLUE}📦 Running Containers:${NC}"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo -e "${YELLOW}💡 Tip: Run './logs.sh [service-name]' to view logs${NC}"
echo -e "${YELLOW}💡 Tip: Run './stop.sh' to stop all services${NC}"