#!/usr/bin/env bash
# =============================================================================
# TCC — MANAGEMENT CONSOLE
# Unified entry point for the Microsservice and Monolith stacks.
# =============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MICRO_DIR="$SCRIPT_DIR/microsservice"
MONO_DIR="$SCRIPT_DIR/monolith"
MONITORING_COMPOSE="$SCRIPT_DIR/docker-compose.monitoring.yml"
K6_SCRIPT="$MICRO_DIR/scripts/k6-load-test.js"
RESULTS_DIR="$MICRO_DIR/scripts/test-results"

# ---------------------------------------------------------------------------
# Detect OS — on Linux Docker Compose needs an extra flag so containers can
# reach services on the host via host.docker.internal.
# ---------------------------------------------------------------------------
HOST_GATEWAY_FLAG=""
if [[ "$(uname -s)" == "Linux" ]]; then
    HOST_GATEWAY_FLAG="--add-host=host.docker.internal:host-gateway"
fi

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
pause() { echo ""; read -rp "Press Enter to continue..."; }

check_service() {
    local name=$1 url=$2
    printf "  %-35s" "$name"
    if curl -f -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}DOWN${NC}"
        return 1
    fi
}

check_k6() {
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}k6 is not installed.${NC}"
        echo "  macOS:  brew install k6"
        echo "  Docs:   https://k6.io/docs/get-started/installation/"
        pause; return 1
    fi
    return 0
}

run_k6() {
    local base_url=$1 label=$2 extra_args=${3:-""}
    check_k6 || return
    mkdir -p "$RESULTS_DIR"
    local ts tag out_json out_summary
    ts=$(date +%Y%m%d_%H%M%S)
    tag=$(echo "$label" | tr ' ' '_' | tr '[:upper:]' '[:lower:]')
    out_json="$RESULTS_DIR/${tag}_${ts}.json"
    out_summary="$RESULTS_DIR/${tag}_${ts}_summary.json"

    echo -e "${BLUE}[K6] $label${NC}"
    echo -e "${CYAN}  Output: $out_json${NC}"
    echo ""
    # shellcheck disable=SC2086
    k6 run \
        --env BASE_URL="$base_url" \
        --out "json=$out_json" \
        --summary-export "$out_summary" \
        $extra_args \
        "$K6_SCRIPT"
    echo ""
    echo -e "${GREEN}Test finished.  Summary: ${CYAN}$out_summary${NC}"
    echo ""
    pause
}

open_url() {
    open "$1" 2>/dev/null || xdg-open "$1" 2>/dev/null || echo "Open: $1"
    pause
}

# ---------------------------------------------------------------------------
# MICROSSERVICES sub-menu
# ---------------------------------------------------------------------------
micro_build() {
    echo -e "${BLUE}[MICRO] Building all microservices...${NC}"
    local services=("eureka-server-ms" "user-ms" "post-ms" "comment-ms" "like-ms" "friendship-ms" "gateway-service-ms")
    local errors=0 count=0 total=${#services[@]}
    for svc in "${services[@]}"; do
        ((count++)) || true
        echo -e "${CYAN}[$count/$total] $svc${NC}"
        if (cd "$MICRO_DIR/$svc" && mvn clean package -DskipTests -q 2>"/tmp/build_${svc}.log"); then
            echo -e "  ${GREEN}OK${NC}"
        else
            echo -e "  ${RED}FAILED — see /tmp/build_${svc}.log${NC}"
            ((errors++)) || true
        fi
    done
    echo ""
    [[ $errors -eq 0 ]] && echo -e "${GREEN}All services built.${NC}" \
                        || echo -e "${RED}$errors service(s) failed.${NC}"
    pause
}

micro_deploy() {
    echo -e "${BLUE}[MICRO] Deploying microservices...${NC}"
    cd "$MICRO_DIR"
    # shellcheck disable=SC2086
    docker compose $HOST_GATEWAY_FLAG up -d --build
    echo ""
    echo -e "${YELLOW}Waiting 90s for services to start...${NC}"
    for i in {90..1}; do printf "\r  %02ds remaining..." $i; sleep 1; done
    echo ""
    echo -e "${GREEN}Microservices deployed.${NC}"
    echo -e "  Gateway: ${CYAN}http://localhost:18765${NC}"
    echo -e "  Eureka:  ${CYAN}http://localhost:8761${NC}"
    echo ""
    pause
}

micro_fresh() {
    echo -e "${RED}This will remove all microservice containers and volumes!${NC}"
    echo -ne "Continue? (yes/no): "; read -r confirm
    [[ "$confirm" != "yes" ]] && { echo "Cancelled."; pause; return; }
    cd "$MICRO_DIR"
    docker compose down -v 2>/dev/null || true
    micro_build
    micro_deploy
}

micro_health() {
    echo -e "${BLUE}[MICRO] Health Check${NC}"; echo ""
    local ok=0
    check_service "Eureka Server"      "http://localhost:8761"                  && ((ok++)) || true
    check_service "API Gateway"        "http://localhost:18765/actuator/health" && ((ok++)) || true
    check_service "User Service"       "http://localhost:18081/actuator/health" && ((ok++)) || true
    check_service "Post Service"       "http://localhost:18082/actuator/health" && ((ok++)) || true
    check_service "Comment Service"    "http://localhost:18083/actuator/health" && ((ok++)) || true
    check_service "Like Service"       "http://localhost:18084/actuator/health" && ((ok++)) || true
    check_service "Friendship Service" "http://localhost:18085/actuator/health" && ((ok++)) || true
    echo ""; echo -e "${CYAN}$ok/7 services healthy${NC}"; echo ""
    pause
}

micro_logs() {
    echo -e "${BLUE}[MICRO] Select service:${NC}"
    echo "  1) User     2) Post     3) Comment"
    echo "  4) Like     5) Friendship  6) Gateway  7) Eureka   0) Back"
    echo -ne "Option: "; read -r opt
    case $opt in
        1) docker logs -f --tail=100 mstcc_user_service ;;
        2) docker logs -f --tail=100 mstcc_post_service ;;
        3) docker logs -f --tail=100 mstcc_comment_service ;;
        4) docker logs -f --tail=100 mstcc_like_service ;;
        5) docker logs -f --tail=100 mstcc_friendship_service ;;
        6) docker logs -f --tail=100 mstcc_gateway ;;
        7) docker logs -f --tail=100 mstcc_eureka_server ;;
        0) return ;;
    esac
}

micro_k6() {
    echo -e "${BLUE}[MICRO] K6 — select scenario:${NC}"
    echo "  1) Full suite (~18 min)      2) Baseline (5 VUs, 2 min)"
    echo "  3) Steady load (20 VUs, 3 min)  4) Stress test (up to 150 VUs)"
    echo "  5) Spike test (up to 200 VUs)   6) Read-heavy (30 VUs, 2 min)"
    echo "  0) Back"
    echo -ne "Option: "; read -r opt
    local url="http://localhost:18765"
    case $opt in
        1) run_k6 "$url" "micro_full_suite" ;;
        2) run_k6 "$url" "micro_baseline"   "--duration 2m --vus 5" ;;
        3) run_k6 "$url" "micro_steady"     "--duration 3m --vus 20" ;;
        4) run_k6 "$url" "micro_stress" ;;
        5) run_k6 "$url" "micro_spike" ;;
        6) run_k6 "$url" "micro_read_heavy" "--duration 2m --vus 30" ;;
        0) return ;;
    esac
}

micro_stop() {
    echo -e "${BLUE}[MICRO] Stopping microservices...${NC}"
    cd "$MICRO_DIR" && docker compose down 2>/dev/null || true
    echo -e "${GREEN}Done.${NC}"; pause
}

micro_clean() {
    echo -e "${RED}This will remove ALL microservice containers, volumes and images!${NC}"
    echo -ne "Continue? (yes/no): "; read -r confirm
    [[ "$confirm" != "yes" ]] && { echo "Cancelled."; pause; return; }
    cd "$MICRO_DIR"
    docker compose down -v 2>/dev/null || true
    docker images | grep -E "microsservice|mstcc" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
    echo -e "${GREEN}Clean done.${NC}"; pause
}

micro_menu() {
    while true; do
        clear
        echo -e "${BLUE}${BOLD}TCC — MICROSSERVICES${NC}"
        echo ""
        echo "  1) Build             2) Deploy"
        echo "  3) Fresh Start       4) Health Check"
        echo "  5) Logs              6) Stress Test (K6)"
        echo "  7) Stop              8) Clean"
        echo "  0) Back"
        echo ""
        echo -ne "${YELLOW}Option: ${NC}"; read -r opt
        case $opt in
            1) micro_build ;;
            2) micro_deploy ;;
            3) micro_fresh ;;
            4) micro_health ;;
            5) micro_logs ;;
            6) micro_k6 ;;
            7) micro_stop ;;
            8) micro_clean ;;
            0) return ;;
            *) echo -e "${RED}Invalid option${NC}"; sleep 1 ;;
        esac
    done
}

# ---------------------------------------------------------------------------
# MONOLITH sub-menu
# ---------------------------------------------------------------------------
mono_build() {
    echo -e "${BLUE}[MONO] Building monolith...${NC}"
    if (cd "$MONO_DIR" && mvn clean package -DskipTests -q 2>/tmp/build_monolith.log); then
        echo -e "${GREEN}Build successful.${NC}"
    else
        echo -e "${RED}Build FAILED — see /tmp/build_monolith.log${NC}"
    fi
    pause
}

mono_deploy() {
    echo -e "${BLUE}[MONO] Deploying monolith...${NC}"
    cd "$MONO_DIR"
    # shellcheck disable=SC2086
    docker compose $HOST_GATEWAY_FLAG up -d --build
    echo ""
    echo -e "${YELLOW}Waiting 60s for service to start...${NC}"
    for i in {60..1}; do printf "\r  %02ds remaining..." $i; sleep 1; done
    echo ""
    echo -e "${GREEN}Monolith deployed.${NC}"
    echo -e "  App:  ${CYAN}http://localhost:8080${NC}"
    echo ""
    pause
}

mono_fresh() {
    echo -e "${RED}This will remove the monolith container and volume!${NC}"
    echo -ne "Continue? (yes/no): "; read -r confirm
    [[ "$confirm" != "yes" ]] && { echo "Cancelled."; pause; return; }
    cd "$MONO_DIR"
    docker compose down -v 2>/dev/null || true
    mono_build
    mono_deploy
}

mono_health() {
    echo -e "${BLUE}[MONO] Health Check${NC}"; echo ""
    local ok=0
    check_service "Monolith App" "http://localhost:8080/actuator/health" && ((ok++)) || true
    echo ""; echo -e "${CYAN}$ok/1 services healthy${NC}"; echo ""
    pause
}

mono_logs() {
    echo -e "${BLUE}[MONO] Streaming logs (Ctrl+C to stop)...${NC}"
    docker logs -f --tail=100 mono_app
}

mono_k6() {
    echo -e "${BLUE}[MONO] K6 — select scenario:${NC}"
    echo "  1) Full suite (~18 min)      2) Baseline (5 VUs, 2 min)"
    echo "  3) Steady load (20 VUs, 3 min)  4) Stress test (up to 150 VUs)"
    echo "  5) Spike test (up to 200 VUs)   6) Read-heavy (30 VUs, 2 min)"
    echo "  0) Back"
    echo -ne "Option: "; read -r opt
    local url="http://localhost:8080"
    case $opt in
        1) run_k6 "$url" "mono_full_suite" ;;
        2) run_k6 "$url" "mono_baseline"   "--duration 2m --vus 5" ;;
        3) run_k6 "$url" "mono_steady"     "--duration 3m --vus 20" ;;
        4) run_k6 "$url" "mono_stress" ;;
        5) run_k6 "$url" "mono_spike" ;;
        6) run_k6 "$url" "mono_read_heavy" "--duration 2m --vus 30" ;;
        0) return ;;
    esac
}

mono_stop() {
    echo -e "${BLUE}[MONO] Stopping monolith...${NC}"
    cd "$MONO_DIR" && docker compose down 2>/dev/null || true
    echo -e "${GREEN}Done.${NC}"; pause
}

mono_clean() {
    echo -e "${RED}This will remove the monolith container, volume and image!${NC}"
    echo -ne "Continue? (yes/no): "; read -r confirm
    [[ "$confirm" != "yes" ]] && { echo "Cancelled."; pause; return; }
    cd "$MONO_DIR"
    docker compose down -v 2>/dev/null || true
    docker images | grep "monolith" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
    echo -e "${GREEN}Clean done.${NC}"; pause
}

mono_menu() {
    while true; do
        clear
        echo -e "${BLUE}${BOLD}TCC — MONOLITH${NC}"
        echo ""
        echo "  1) Build             2) Deploy"
        echo "  3) Fresh Start       4) Health Check"
        echo "  5) Logs              6) Stress Test (K6)"
        echo "  7) Stop              8) Clean"
        echo "  0) Back"
        echo ""
        echo -ne "${YELLOW}Option: ${NC}"; read -r opt
        case $opt in
            1) mono_build ;;
            2) mono_deploy ;;
            3) mono_fresh ;;
            4) mono_health ;;
            5) mono_logs ;;
            6) mono_k6 ;;
            7) mono_stop ;;
            8) mono_clean ;;
            0) return ;;
            *) echo -e "${RED}Invalid option${NC}"; sleep 1 ;;
        esac
    done
}

# ---------------------------------------------------------------------------
# MONITORING sub-menu
# ---------------------------------------------------------------------------
mon_start() {
    echo -e "${BLUE}[MON] Starting monitoring stack...${NC}"
    cd "$SCRIPT_DIR"
    # shellcheck disable=SC2086
    docker compose $HOST_GATEWAY_FLAG -p mstcc-monitoring -f "$MONITORING_COMPOSE" up -d
    echo -e "${GREEN}Monitoring started.${NC}"
    echo -e "  Grafana:    ${CYAN}http://localhost:3000${NC}  (admin/admin)"
    echo -e "  Prometheus: ${CYAN}http://localhost:9090${NC}"
    echo ""
    pause
}

mon_stop() {
    echo -e "${BLUE}[MON] Stopping monitoring stack...${NC}"
    cd "$SCRIPT_DIR"
    docker compose -p mstcc-monitoring -f "$MONITORING_COMPOSE" down 2>/dev/null || true
    echo -e "${GREEN}Done.${NC}"; pause
}

mon_menu() {
    while true; do
        clear
        echo -e "${BLUE}${BOLD}TCC — MONITORING${NC}"
        echo ""
        echo "  1) Start Monitoring"
        echo "  2) Stop Monitoring"
        echo "  3) Grafana    http://localhost:3000"
        echo "  4) Prometheus http://localhost:9090"
        echo "  0) Back"
        echo ""
        echo -ne "${YELLOW}Option: ${NC}"; read -r opt
        case $opt in
            1) mon_start ;;
            2) mon_stop ;;
            3) open_url "http://localhost:3000" ;;
            4) open_url "http://localhost:9090" ;;
            0) return ;;
            *) echo -e "${RED}Invalid option${NC}"; sleep 1 ;;
        esac
    done
}

# ---------------------------------------------------------------------------
# MAIN MENU
# ---------------------------------------------------------------------------
while true; do
    clear
    echo -e "${BLUE}╔═══════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     ${BOLD}TCC — MANAGEMENT CONSOLE${NC}${BLUE}                ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${CYAN}[micro]${NC} Microsservices    ${CYAN}[mono]${NC} Monolith"
    echo -e "  ${CYAN}[mon]${NC}   Monitoring        ${CYAN}[0]${NC}    Exit"
    echo ""
    echo -ne "${YELLOW}Option: ${NC}"; read -r option
    case $option in
        micro) micro_menu ;;
        mono)  mono_menu ;;
        mon)   mon_menu ;;
        0)     echo -e "${GREEN}Exiting...${NC}"; exit 0 ;;
        *)     echo -e "${RED}Invalid option${NC}"; sleep 1 ;;
    esac
done
