#!/bin/bash

# Complete Microservices Deployment Script
# Deploys microservices, databases, and monitoring stack

set -e

echo "======================================"
echo "🚀 Complete System Deployment"
echo "======================================"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Detect if we're in scripts directory or project root
if [ -d "../monitoring" ]; then
    echo -e "${YELLOW}📂 Detected: Running from scripts directory${NC}"
    cd ..
elif [ ! -d "monitoring" ]; then
    echo -e "${RED}❌ Error: Cannot find monitoring directory${NC}"
    echo "Please run this script from project root or scripts/ directory"
    exit 1
fi

echo -e "${BLUE}📂 Working directory: $(pwd)${NC}"
echo ""

# Configuration
COMPOSE_FILES="-f docker-compose.yml"
INCLUDE_DB=true
INCLUDE_MONITORING=true

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --no-db)
            INCLUDE_DB=false
            shift
            ;;
        --no-monitoring)
            INCLUDE_MONITORING=false
            shift
            ;;
        --only-services)
            INCLUDE_DB=false
            INCLUDE_MONITORING=false
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--no-db] [--no-monitoring] [--only-services]"
            exit 1
            ;;
    esac
done

# Build compose files list
if [ "$INCLUDE_DB" = true ]; then
    if [ -f "docker-compose.db.yml" ]; then
        COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.db.yml"
    else
        echo -e "${RED}⚠️  Warning: docker-compose.db.yml not found${NC}"
        INCLUDE_DB=false
    fi
fi

if [ "$INCLUDE_MONITORING" = true ]; then
    if [ -f "docker-compose.monitoring.yml" ]; then
        COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.monitoring.yml"
    else
        echo -e "${RED}⚠️  Warning: docker-compose.monitoring.yml not found${NC}"
        INCLUDE_MONITORING=false
    fi
fi

echo -e "${BLUE}📋 Deployment Configuration:${NC}"
echo "  - Main services: ✅"
echo "  - Databases: $([ "$INCLUDE_DB" = true ] && echo "✅" || echo "❌")"
echo "  - Monitoring: $([ "$INCLUDE_MONITORING" = true ] && echo "✅" || echo "❌")"
echo "  - Compose files: $COMPOSE_FILES"
echo ""

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=0

    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -e "   Attempt $attempt/$max_attempts..."
        sleep 5
    done
    
    echo -e "${RED}❌ $service_name failed to become ready${NC}"
    return 1
}

# Function to check if container is running
check_container() {
    local container_name=$1
    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        return 0
    else
        return 1
    fi
}

# Step 1: Clean up
echo -e "\n${YELLOW}🧹 Cleaning up existing containers...${NC}"
docker-compose $COMPOSE_FILES down -v 2>/dev/null || true
sleep 2

# Step 2: Build images
echo -e "\n${YELLOW}🔨 Building Docker images...${NC}"
docker-compose $COMPOSE_FILES build --parallel

# Step 3: Start databases (if included)
if [ "$INCLUDE_DB" = true ]; then
    echo -e "\n${YELLOW}🗄️ Starting PostgreSQL databases...${NC}"
    docker-compose $COMPOSE_FILES up -d user-ms-db post-ms-db comment-ms-db like-ms-db friendship-ms-db
    
    echo -e "${YELLOW}⏳ Waiting for databases to initialize...${NC}"
    sleep 15
    
    # Verify databases are healthy
    echo ""
    for db in user_ms_db post_ms_db comment_ms_db like_ms_db friendship_ms_db; do
        if check_container "$db"; then
            echo -e "  ${GREEN}✅ $db${NC}"
        else
            echo -e "  ${RED}⚠️  $db failed to start${NC}"
        fi
    done
    echo ""
fi

# Step 4: Start Eureka Server
echo -e "${YELLOW}🌐 Starting Eureka Server...${NC}"
docker-compose $COMPOSE_FILES up -d eureka-server-ms

# Wait for Eureka
if ! wait_for_service "Eureka Server" "http://localhost:8761/actuator/health" 40; then
    echo -e "${YELLOW}⚠️  Trying alternative Eureka endpoint...${NC}"
    wait_for_service "Eureka Server (main page)" "http://localhost:8761" 20 || true
fi

echo -e "${YELLOW}⏳ Giving Eureka time to fully initialize...${NC}"
sleep 20

# Step 5: Start microservices
echo -e "\n${YELLOW}⚙️ Starting microservices...${NC}"
docker-compose $COMPOSE_FILES up -d user-ms post-ms comment-ms like-ms friendship-ms

echo -e "${YELLOW}⏳ Waiting for services to start and register...${NC}"
sleep 35

# Step 6: Start API Gateway
echo -e "\n${YELLOW}🚪 Starting API Gateway...${NC}"
docker-compose $COMPOSE_FILES up -d gateway-service-ms

echo -e "${YELLOW}⏳ Waiting for Gateway to initialize...${NC}"
sleep 25

if ! wait_for_service "API Gateway" "http://localhost:18765/actuator/health" 20; then
    echo -e "${YELLOW}⚠️  Gateway may still be starting...${NC}"
fi

# Step 7: Start monitoring stack (if included)
if [ "$INCLUDE_MONITORING" = true ]; then
    echo -e "\n${YELLOW}📊 Starting monitoring stack...${NC}"
    
    # Start Prometheus
    echo -e "${BLUE}  Starting Prometheus...${NC}"
    docker-compose $COMPOSE_FILES up -d prometheus
    sleep 5
    
    if check_container "mstcc_prometheus"; then
        echo -e "  ${GREEN}✅ Prometheus started${NC}"
        if wait_for_service "Prometheus" "http://localhost:9090/-/ready" 15; then
            echo -e "  ${GREEN}✅ Prometheus is ready${NC}"
        fi
    else
        echo -e "  ${RED}⚠️  Prometheus failed to start${NC}"
    fi
    
    # Start Grafana
    echo -e "${BLUE}  Starting Grafana...${NC}"
    docker-compose $COMPOSE_FILES up -d grafana
    sleep 5
    
    if check_container "mstcc_grafana"; then
        echo -e "  ${GREEN}✅ Grafana started${NC}"
        if wait_for_service "Grafana" "http://localhost:3000/api/health" 20; then
            echo -e "  ${GREEN}✅ Grafana is ready${NC}"
        fi
    else
        echo -e "  ${RED}⚠️  Grafana failed to start${NC}"
    fi
    echo ""
fi

# Step 8: Comprehensive verification
echo -e "${YELLOW}🔍 Verifying deployment...${NC}"
echo ""
echo -e "${BLUE}Container Status:${NC}"
docker-compose $COMPOSE_FILES ps

# Step 9: Check Eureka registry
echo -e "\n${YELLOW}📋 Checking Eureka service registry...${NC}"
sleep 5

EUREKA_APPS=$(curl -s http://localhost:8761/eureka/apps 2>/dev/null || echo "")
if [ -n "$EUREKA_APPS" ]; then
    REGISTERED_SERVICES=$(echo "$EUREKA_APPS" | grep -o '<app>[^<]*</app>' | sed 's/<[^>]*>//g' | wc -l | tr -d ' ')
    echo -e "${GREEN}✅ $REGISTERED_SERVICES services registered:${NC}"
    echo "$EUREKA_APPS" | grep -o '<app>[^<]*</app>' | sed 's/<[^>]*>//g' | sed 's/^/  - /'
else
    echo -e "${YELLOW}⚠️  Could not fetch Eureka registry${NC}"
fi

# Step 10: Test endpoints
echo -e "\n${YELLOW}🔍 Testing service endpoints...${NC}"

test_endpoint() {
    local name=$1
    local url=$2
    printf "  %-25s" "$name"
    if curl -f -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅${NC}"
    else
        echo -e "${YELLOW}⚠️  (not ready yet)${NC}"
    fi
}

test_endpoint "Eureka Server" "http://localhost:8761"
test_endpoint "API Gateway" "http://localhost:18765/actuator/health"
test_endpoint "User Service" "http://localhost:18081/actuator/health"
test_endpoint "Post Service" "http://localhost:18082/actuator/health"
test_endpoint "Comment Service" "http://localhost:18083/actuator/health"
test_endpoint "Like Service" "http://localhost:18084/actuator/health"
test_endpoint "Friendship Service" "http://localhost:18085/actuator/health"

if [ "$INCLUDE_MONITORING" = true ]; then
    test_endpoint "Prometheus" "http://localhost:9090/-/ready"
    test_endpoint "Grafana" "http://localhost:3000/api/health"
fi

# Final summary
echo ""
echo "======================================"
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo "======================================"
echo ""

echo "📊 Service URLs:"
echo "  ${BLUE}Core Services:${NC}"
echo "    - Eureka Dashboard:   http://localhost:8761"
echo "    - API Gateway:        http://localhost:18765"
echo "    - User Service:       http://localhost:18081"
echo "    - Post Service:       http://localhost:18082"
echo "    - Comment Service:    http://localhost:18083"
echo "    - Like Service:       http://localhost:18084"
echo "    - Friendship Service: http://localhost:18085"

if [ "$INCLUDE_MONITORING" = true ]; then
    echo ""
    echo "  ${BLUE}Monitoring Stack:${NC}"
    echo "    - Prometheus:         http://localhost:9090"
    echo "      • Targets:          http://localhost:9090/targets"
    echo "      • Graph:            http://localhost:9090/graph"
    echo "    - Grafana:            http://localhost:3000"
    echo "      • Credentials:      admin/admin"
    echo "      • Dashboard:        TCC - Microsserviços Performance"
fi

if [ "$INCLUDE_DB" = true ]; then
    echo ""
    echo "🗄️  Database Ports:"
    echo "    - User DB:       localhost:5433 (user/user123)"
    echo "    - Post DB:       localhost:5434 (post/post123)"
    echo "    - Comment DB:    localhost:5435 (comment/comment123)"
    echo "    - Like DB:       localhost:5436 (like/like123)"
    echo "    - Friendship DB: localhost:5437 (friendship/friendship123)"
fi

echo ""
echo "📝 Useful Commands:"
echo "    - View all logs:       docker-compose $COMPOSE_FILES logs -f"
echo "    - View service logs:   docker-compose $COMPOSE_FILES logs -f [service-name]"
echo "    - Stop all:            docker-compose $COMPOSE_FILES down"
echo "    - Restart service:     docker-compose $COMPOSE_FILES restart [service-name]"
echo "    - Check health:        curl http://localhost:[port]/actuator/health"
echo "    - Rebuild service:     docker-compose $COMPOSE_FILES build [service-name]"

if [ "$INCLUDE_MONITORING" = true ]; then
    echo ""
    echo "📊 Quick Monitoring Checks:"
    echo "    - Prometheus targets:  curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'"
    echo "    - Grafana datasource:  curl -s http://localhost:3000/api/datasources"
    echo "    - Generate traffic:    curl http://localhost:18765/api/users"
fi

echo ""
echo -e "${GREEN}🎉 System is ready for use!${NC}"

if [ "$INCLUDE_MONITORING" = true ]; then
    echo ""
    echo -e "${YELLOW}💡 Next Steps for Monitoring:${NC}"
    echo "    1. Open Grafana: http://localhost:3000"
    echo "    2. Login with admin/admin"
    echo "    3. Navigate to Dashboards → TCC - Microsserviços Performance"
    echo "    4. Generate traffic to see metrics populate"
    echo "    5. Adjust time range if needed (top right)"
fi

echo ""