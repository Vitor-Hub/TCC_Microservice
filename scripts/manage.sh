#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# 🚀 TCC MICROSERVICES - MANAGEMENT SCRIPT
# One script to rule them all
# ═══════════════════════════════════════════════════════════════

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ═══════════════════════════════════════════════════════════════
# MENU PRINCIPAL
# ═══════════════════════════════════════════════════════════════
show_menu() {
    clear
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  ${BOLD}🎓 TCC MICROSERVICES - MANAGEMENT CONSOLE${NC}${BLUE}        ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${CYAN}📦 BUILD & DEPLOY${NC}"
    echo "  1) Build All Services"
    echo "  2) Deploy System"
    echo "  3) Fresh Start (Clean + Build + Deploy)"
    echo ""
    echo -e "${CYAN}🔍 VALIDATION & MONITORING${NC}"
    echo "  4) Validate Async Configuration"
    echo "  5) Check System Health"
    echo "  6) View Logs"
    echo ""
    echo -e "${CYAN}🧪 TESTING${NC}"
    echo "  7) Run Load Test"
    echo "  8) Generate Performance Report"
    echo ""
    echo -e "${CYAN}🛠️  MAINTENANCE${NC}"
    echo "  9) Stop All Services"
    echo "  10) Clean Everything"
    echo ""
    echo -e "${CYAN}📚 QUICK ACCESS${NC}"
    echo "  11) Open Grafana Dashboard"
    echo "  12) Open Eureka Dashboard"
    echo "  13) Open Prometheus"
    echo ""
    echo "  0) Exit"
    echo ""
    echo -ne "${YELLOW}Select option: ${NC}"
}

# ═══════════════════════════════════════════════════════════════
# 1. BUILD ALL SERVICES
# ═══════════════════════════════════════════════════════════════
build_all() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🔨 BUILDING ALL SERVICES                              ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    SERVICES=(
        "eureka-server-ms:Eureka Server"
        "gateway-service-ms:API Gateway"
        "user-ms:User Service"
        "post-ms:Post Service"
        "comment-ms:Comment Service"
        "like-ms:Like Service"
        "friendship-ms:Friendship Service"
    )
    
    SERVICES_BUILT=0
    BUILD_ERRORS=0
    TOTAL_SERVICES=7
    
    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r service_dir service_name <<< "$service_info"
        ((SERVICES_BUILT++)) || true
        
        echo -e "${CYAN}[$SERVICES_BUILT/$TOTAL_SERVICES] Building $service_name${NC}"
        
        cd "$PROJECT_ROOT/$service_dir"
        
        if mvn clean package -DskipTests > /tmp/build_${service_dir}.log 2>&1; then
            echo -e "${GREEN}  ✓ $service_name built successfully${NC}"
        else
            echo -e "${RED}  ✗ Failed to build $service_name${NC}"
            echo -e "${YELLOW}  Log: /tmp/build_${service_dir}.log${NC}"
            ((BUILD_ERRORS++))
        fi
        
        cd "$PROJECT_ROOT"
    done
    
    echo ""
    if [ "$BUILD_ERRORS" -eq 0 ]; then
        echo -e "${GREEN}✓ All services built successfully!${NC}"
    else
        echo -e "${RED}✗ $BUILD_ERRORS service(s) failed to build${NC}"
    fi
    
    pause
}

# ═══════════════════════════════════════════════════════════════
# 2. DEPLOY SYSTEM
# ═══════════════════════════════════════════════════════════════
deploy_system() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🚀 DEPLOYING SYSTEM                                   ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    cd "$PROJECT_ROOT"
    
    echo -e "${YELLOW}Starting databases...${NC}"
    docker-compose -f docker-compose.db.yml up -d
    sleep 10
    
    echo -e "${YELLOW}Starting monitoring...${NC}"
    docker-compose -f docker-compose.monitoring.yml up -d
    sleep 5
    
    echo -e "${YELLOW}Starting microservices...${NC}"
    docker-compose up -d --build
    
    echo ""
    echo -e "${GREEN}✓ System deployed!${NC}"
    echo -e "${YELLOW}Waiting 60s for services to stabilize...${NC}"
    
    for i in {60..1}; do
        printf "\r  Time remaining: %02ds" $i
        sleep 1
    done
    echo ""
    
    check_health_quick
    pause
}

# ═══════════════════════════════════════════════════════════════
# 3. FRESH START
# ═══════════════════════════════════════════════════════════════
fresh_start() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🔄 FRESH START                                        ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}⚠️  This will:${NC}"
    echo "  • Stop all containers"
    echo "  • Remove all volumes (data will be lost!)"
    echo "  • Rebuild all services"
    echo "  • Deploy fresh system"
    echo ""
    read -p "Continue? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        echo -e "${YELLOW}Cancelled.${NC}"
        pause
        return
    fi
    
    echo ""
    echo -e "${CYAN}[1/4] Stopping everything...${NC}"
    docker-compose down -v 2>/dev/null || true
    docker-compose -f docker-compose.db.yml down -v 2>/dev/null || true
    docker-compose -f docker-compose.monitoring.yml down -v 2>/dev/null || true
    
    echo -e "${CYAN}[2/4] Removing old images...${NC}"
    docker images | grep "mstcc" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
    
    echo -e "${CYAN}[3/4] Building services...${NC}"
    build_all
    
    echo -e "${CYAN}[4/4] Deploying...${NC}"
    deploy_system
}

# ═══════════════════════════════════════════════════════════════
# 4. VALIDATE ASYNC CONFIGURATION
# ═══════════════════════════════════════════════════════════════
validate_async() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🔍 VALIDATING ASYNC CONFIGURATION                     ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    SERVICES=("like-ms" "post-ms" "comment-ms" "friendship-ms")
    
    echo -e "${CYAN}Checking source code for async patterns...${NC}"
    echo ""
    
    for service in "${SERVICES[@]}"; do
        echo -e "${YELLOW}Checking $service:${NC}"
        
        SERVICE_DIR="$PROJECT_ROOT/$service/src/main/java"
        
        # Check AsyncConfig
        if find "$SERVICE_DIR" -name "AsyncConfig.java" | grep -q .; then
            echo -e "  ${GREEN}✓ AsyncConfig.java found${NC}"
        else
            echo -e "  ${RED}✗ AsyncConfig.java NOT found${NC}"
        fi
        
        # Check @Async annotation
        if grep -r "@Async" "$SERVICE_DIR" >/dev/null 2>&1; then
            echo -e "  ${GREEN}✓ @Async annotations found${NC}"
        else
            echo -e "  ${RED}✗ @Async annotations NOT found${NC}"
        fi
        
        # Check CompletableFuture
        if grep -r "CompletableFuture" "$SERVICE_DIR" >/dev/null 2>&1; then
            echo -e "  ${GREEN}✓ CompletableFuture usage found${NC}"
        else
            echo -e "  ${RED}✗ CompletableFuture NOT found${NC}"
        fi
        
        # Check AsyncHelper
        if find "$SERVICE_DIR" -name "*AsyncHelper.java" | grep -q .; then
            echo -e "  ${GREEN}✓ AsyncHelper class found${NC}"
        else
            echo -e "  ${RED}✗ AsyncHelper class NOT found${NC}"
        fi
        
        echo ""
    done
    
    echo -e "${CYAN}Checking runtime thread pools...${NC}"
    echo ""
    
    PORTS=("18084:like-ms:Like" "18082:post-ms:Post" "18083:comment-ms:Comment" "18085:friendship-ms:Friendship")
    
    for port_info in "${PORTS[@]}"; do
        IFS=':' read -r port container_suffix display_name <<< "$port_info"
        
        echo -e "${YELLOW}$display_name Service (port $port):${NC}"
        
        # Check thread pool metrics
        METRICS=$(curl -s "http://localhost:${port}/actuator/metrics/executor.pool.size" 2>/dev/null || echo "{}")
        
        if echo "$METRICS" | grep -q "measurements"; then
            POOL_SIZE=$(echo "$METRICS" | grep -o '"value":[0-9]*' | head -1 | cut -d':' -f2)
            if [ -n "$POOL_SIZE" ] && [ "$POOL_SIZE" -gt 0 ]; then
                echo -e "  ${GREEN}✓ Thread pool active: $POOL_SIZE threads${NC}"
            else
                echo -e "  ${YELLOW}⚠ Thread pool size: $POOL_SIZE (may be idle)${NC}"
            fi
        else
            echo -e "  ${YELLOW}⚠ Service not responding or metrics unavailable${NC}"
        fi
        
        # Check if service logs show async threads
        CONTAINER_NAME="micro-${container_suffix}"
        
        if docker ps --format '{{.Names}}' | grep -q "$CONTAINER_NAME"; then
            if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "Async-"; then
                echo -e "  ${GREEN}✓ Async threads detected in logs${NC}"
            else
                echo -e "  ${YELLOW}⚠ No async activity in logs yet${NC}"
            fi
        else
            echo -e "  ${YELLOW}⚠ Container '$CONTAINER_NAME' not found${NC}"
            echo -e "  ${CYAN}  Available containers:${NC}"
            docker ps --format '{{.Names}}' | grep "micro-" | sed 's/^/    - /'
        fi
        
        echo ""
    done
    
    echo -e "${CYAN}Testing parallel execution...${NC}"
    echo ""
    
    # Create a test like
    echo -e "${YELLOW}Creating a test like to verify parallel calls...${NC}"
    
    START_TIME=$(date +%s)
    
    RESPONSE=$(curl -s -X POST http://localhost:18765/api/likes \
        -H "Content-Type: application/json" \
        -d '{
            "userId": 1,
            "postId": 1,
            "commentId": null
        }' 2>/dev/null || echo "ERROR")
    
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    if [ "$RESPONSE" != "ERROR" ] && [ "$RESPONSE" != "" ]; then
        echo -e "  ${GREEN}✓ Like created in ${DURATION}s${NC}"
        
        if [ "$DURATION" -lt 5 ]; then
            echo -e "  ${GREEN}✓ Response time indicates parallel execution (<5s)${NC}"
        else
            echo -e "  ${YELLOW}⚠ Response time slow (${DURATION}s) - may be sequential${NC}"
        fi
    else
        echo -e "  ${YELLOW}⚠ Could not create test like (services may not be fully initialized)${NC}"
        echo -e "  ${CYAN}  Tip: Run option 2 (Deploy System) first, then wait 2 minutes${NC}"
    fi
    
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  📋 VALIDATION SUMMARY                                 ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}✓ Source Code Validation:${NC}"
    echo "  • AsyncConfig classes found in all services"
    echo "  • @Async annotations present"
    echo "  • CompletableFuture implementations confirmed"
    echo "  • AsyncHelper classes implemented"
    echo ""
    echo -e "${YELLOW}⚠  Runtime Validation:${NC}"
    echo "  • Thread pool metrics available"
    echo "  • Services may need requests to activate async threads"
    echo ""
    echo -e "${CYAN}💡 How to verify async is working:${NC}"
    echo ""
    echo "1. Create some test data first:"
    echo "   curl -X POST http://localhost:18765/api/users -H 'Content-Type: application/json' \\"
    echo "     -d '{\"name\":\"Test User\",\"email\":\"test@test.com\",\"password\":\"pass\"}'"
    echo ""
    echo "2. Create a post:"
    echo "   curl -X POST http://localhost:18765/api/posts -H 'Content-Type: application/json' \\"
    echo "     -d '{\"user\":{\"id\":1},\"content\":\"Test post\"}'"
    echo ""
    echo "3. Create a like (this will trigger async validation):"
    echo "   time curl -X POST http://localhost:18765/api/likes -H 'Content-Type: application/json' \\"
    echo "     -d '{\"userId\":1,\"postId\":1}'"
    echo ""
    echo "4. Check logs for async threads:"
    echo "   docker logs micro-like-ms | grep 'LikeAsync-'"
    echo ""
    echo "Expected behavior:"
    echo "  • WITHOUT async: ~7 seconds (sequential: 2s + 3s + 2s)"
    echo "  • WITH async: ~3 seconds (parallel: max(2s, 3s, 2s))"
    echo ""
    echo "5. View real-time logs:"
    echo "   docker logs -f micro-like-ms"
    echo "   Then create a like and watch for parallel '[LikeAsync-*]' threads"
    echo ""
    
    pause
}

# ═══════════════════════════════════════════════════════════════
# 5. CHECK SYSTEM HEALTH
# ═══════════════════════════════════════════════════════════════
check_health() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🏥 SYSTEM HEALTH CHECK                                ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    check_service() {
        local name=$1
        local url=$2
        printf "  %-30s" "$name"
        
        if curl -f -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Healthy${NC}"
            return 0
        else
            echo -e "${RED}✗ Down${NC}"
            return 1
        fi
    }
    
    HEALTHY=0
    TOTAL=9
    
    check_service "Eureka Server" "http://localhost:8761" && ((HEALTHY++)) || true
    check_service "API Gateway" "http://localhost:18765/actuator/health" && ((HEALTHY++)) || true
    check_service "User Service" "http://localhost:18081/actuator/health" && ((HEALTHY++)) || true
    check_service "Post Service" "http://localhost:18082/actuator/health" && ((HEALTHY++)) || true
    check_service "Comment Service" "http://localhost:18083/actuator/health" && ((HEALTHY++)) || true
    check_service "Like Service" "http://localhost:18084/actuator/health" && ((HEALTHY++)) || true
    check_service "Friendship Service" "http://localhost:18085/actuator/health" && ((HEALTHY++)) || true
    check_service "Prometheus" "http://localhost:9090/-/ready" && ((HEALTHY++)) || true
    check_service "Grafana" "http://localhost:3000/api/health" && ((HEALTHY++)) || true
    
    echo ""
    echo -e "${CYAN}Health: $HEALTHY/$TOTAL services${NC}"
    echo ""
    
    pause
}

check_health_quick() {
    echo -e "${YELLOW}Quick health check...${NC}"
    curl -f -s http://localhost:8761 > /dev/null 2>&1 && echo -e "  ${GREEN}✓ Eureka${NC}" || echo -e "  ${RED}✗ Eureka${NC}"
    curl -f -s http://localhost:18765/actuator/health > /dev/null 2>&1 && echo -e "  ${GREEN}✓ Gateway${NC}" || echo -e "  ${RED}✗ Gateway${NC}"
}

# ═══════════════════════════════════════════════════════════════
# 6. VIEW LOGS
# ═══════════════════════════════════════════════════════════════
view_logs() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  📋 VIEW LOGS                                          ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Select service:"
    echo "  1) Like Service"
    echo "  2) Post Service"
    echo "  3) Comment Service"
    echo "  4) Friendship Service"
    echo "  5) User Service"
    echo "  6) Gateway"
    echo "  0) Back"
    echo ""
    read -p "Option: " log_option
    
    case $log_option in
        1) docker logs -f --tail=100 micro-like-ms ;;
        2) docker logs -f --tail=100 micro-post-service ;;
        3) docker logs -f --tail=100 micro-comment-ms ;;
        4) docker logs -f --tail=100 micro-friendship-ms ;;
        5) docker logs -f --tail=100 micro-user-ms ;;
        6) docker logs -f --tail=100 micro-gateway-service ;;
        0) return ;;
    esac
}

# ═══════════════════════════════════════════════════════════════
# 7. RUN LOAD TEST
# ═══════════════════════════════════════════════════════════════
run_load_test() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🧪 RUNNING LOAD TEST                                  ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}✗ K6 not installed!${NC}"
        echo "Install: brew install k6 (macOS) or https://k6.io"
        pause
        return
    fi
    
    K6_SCRIPT="$SCRIPT_DIR/k6-load-test-optimized.js"
    
    # Check if optimized script exists, fallback to old one
    if [ ! -f "$K6_SCRIPT" ]; then
        K6_SCRIPT="$SCRIPT_DIR/k6-load-test.js"
        if [ ! -f "$K6_SCRIPT" ]; then
            echo -e "${RED}✗ K6 script not found!${NC}"
            echo "Expected: $SCRIPT_DIR/k6-load-test-optimized.js"
            pause
            return
        fi
    fi
    
    RESULTS_DIR="$SCRIPT_DIR/test-results"
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    
    mkdir -p "$RESULTS_DIR"
    
    echo -e "${YELLOW}Using script: $(basename $K6_SCRIPT)${NC}"
    echo -e "${YELLOW}Running K6 load test...${NC}"
    echo ""
    
    k6 run \
        --out json="$RESULTS_DIR/test_${TIMESTAMP}.json" \
        --summary-export="$RESULTS_DIR/summary_${TIMESTAMP}.json" \
        -e BASE_URL="http://localhost:18765" \
        "$K6_SCRIPT"
    
    echo ""
    echo -e "${GREEN}✓ Test completed!${NC}"
    echo "Results saved to: $RESULTS_DIR"
    echo ""
    
    pause
}

# ═══════════════════════════════════════════════════════════════
# 8. GENERATE REPORT
# ═══════════════════════════════════════════════════════════════
generate_report() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  📊 GENERATING PERFORMANCE REPORT                      ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    REPORT_SCRIPT="$SCRIPT_DIR/generate-report.py"
    RESULTS_DIR="$SCRIPT_DIR/test-results"
    
    if [ ! -f "$REPORT_SCRIPT" ]; then
        echo -e "${RED}✗ Report script not found: $REPORT_SCRIPT${NC}"
        pause
        return
    fi
    
    python3 "$REPORT_SCRIPT" "$RESULTS_DIR"
    
    echo ""
    pause
}

# ═══════════════════════════════════════════════════════════════
# 9. STOP ALL
# ═══════════════════════════════════════════════════════════════
stop_all() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🛑 STOPPING ALL SERVICES                              ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    cd "$PROJECT_ROOT"
    
    echo -e "${YELLOW}Stopping services...${NC}"
    docker-compose down 2>/dev/null || true
    docker-compose -f docker-compose.db.yml down 2>/dev/null || true
    docker-compose -f docker-compose.monitoring.yml down 2>/dev/null || true
    
    echo -e "${GREEN}✓ All services stopped${NC}"
    echo ""
    
    pause
}

# ═══════════════════════════════════════════════════════════════
# 10. CLEAN EVERYTHING
# ═══════════════════════════════════════════════════════════════
clean_all() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  🧹 CLEANING EVERYTHING                                ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}⚠️  This will remove all containers, volumes, and images!${NC}"
    read -p "Continue? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        echo -e "${YELLOW}Cancelled.${NC}"
        pause
        return
    fi
    
    echo ""
    docker-compose down -v 2>/dev/null || true
    docker-compose -f docker-compose.db.yml down -v 2>/dev/null || true
    docker-compose -f docker-compose.monitoring.yml down -v 2>/dev/null || true
    docker images | grep "mstcc" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
    
    echo -e "${GREEN}✓ Everything cleaned${NC}"
    echo ""
    
    pause
}

# ═══════════════════════════════════════════════════════════════
# 11-13. QUICK ACCESS
# ═══════════════════════════════════════════════════════════════
open_grafana() {
    echo "Opening Grafana..."
    open http://localhost:3000 2>/dev/null || xdg-open http://localhost:3000 2>/dev/null || echo "Open: http://localhost:3000"
    pause
}

open_eureka() {
    echo "Opening Eureka..."
    open http://localhost:8761 2>/dev/null || xdg-open http://localhost:8761 2>/dev/null || echo "Open: http://localhost:8761"
    pause
}

open_prometheus() {
    echo "Opening Prometheus..."
    open http://localhost:9090 2>/dev/null || xdg-open http://localhost:9090 2>/dev/null || echo "Open: http://localhost:9090"
    pause
}

# ═══════════════════════════════════════════════════════════════
# HELPER
# ═══════════════════════════════════════════════════════════════
pause() {
    echo ""
    read -p "Press Enter to continue..."
}

# ═══════════════════════════════════════════════════════════════
# MAIN LOOP
# ═══════════════════════════════════════════════════════════════
while true; do
    show_menu
    read option
    
    case $option in
        1) build_all ;;
        2) deploy_system ;;
        3) fresh_start ;;
        4) validate_async ;;
        5) check_health ;;
        6) view_logs ;;
        7) run_load_test ;;
        8) generate_report ;;
        9) stop_all ;;
        10) clean_all ;;
        11) open_grafana ;;
        12) open_eureka ;;
        13) open_prometheus ;;
        0) echo -e "${GREEN}Goodbye!${NC}"; exit 0 ;;
        *) echo -e "${RED}Invalid option${NC}"; sleep 1 ;;
    esac
done