#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# TCC MICROSERVICES - MANAGEMENT CONSOLE
# ═══════════════════════════════════════════════════════════════

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$SCRIPT_DIR/test-results"
K6_SCRIPT="$SCRIPT_DIR/k6-load-test.js"

# ═══════════════════════════════════════════════════════════════
# MENU
# ═══════════════════════════════════════════════════════════════
show_menu() {
    clear
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     ${BOLD}TCC MICROSERVICES - MANAGEMENT CONSOLE${NC}${BLUE}          ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${CYAN}BUILD & DEPLOY${NC}"
    echo "  1) Build All Services"
    echo "  2) Deploy System"
    echo "  3) Fresh Start  (limpa tudo + build + deploy)"
    echo ""
    echo -e "${CYAN}MONITORAMENTO${NC}"
    echo "  4) Check System Health"
    echo "  5) View Logs"
    echo ""
    echo -e "${CYAN}TESTES DE CARGA (k6)${NC}"
    echo "  6) Rodar Suite Completa (~18 min)"
    echo "  7) Baseline Load        (5 VUs, 2 min)"
    echo "  8) Steady Load          (20 VUs, 3 min)"
    echo "  9) Stress Test          (ate 150 VUs, 8 min)"
    echo "  10) Spike Test           (ate 200 VUs, 3 min)"
    echo "  11) Read-Heavy           (30 VUs, 2 min)"
    echo ""
    echo -e "${CYAN}DADOS & RELATORIO${NC}"
    echo "  12) Seed Test Data"
    echo "  13) Gerar Relatorio de Comparacao"
    echo ""
    echo -e "${CYAN}MANUTENCAO${NC}"
    echo "  14) Stop All Services"
    echo "  15) Clean Everything"
    echo ""
    echo -e "${CYAN}DASHBOARDS${NC}"
    echo "  16) Grafana   http://localhost:3000"
    echo "  17) Eureka    http://localhost:8761"
    echo "  18) Prometheus http://localhost:9090"
    echo ""
    echo "  0) Sair"
    echo ""
    echo -ne "${YELLOW}Opcao: ${NC}"
}

# ═══════════════════════════════════════════════════════════════
# 1. BUILD
# ═══════════════════════════════════════════════════════════════
build_all() {
    echo -e "${BLUE}[BUILD] Building all services...${NC}"
    echo ""

    local services=(
        "eureka-server-ms:Eureka Server"
        "user-ms:User Service"
        "post-ms:Post Service"
        "comment-ms:Comment Service"
        "like-ms:Like Service"
        "friendship-ms:Friendship Service"
        "gateway-service-ms:API Gateway"
    )

    local errors=0
    local count=0
    local total=${#services[@]}

    for entry in "${services[@]}"; do
        IFS=':' read -r dir name <<< "$entry"
        ((count++)) || true
        echo -e "${CYAN}[$count/$total] $name${NC}"
        cd "$PROJECT_ROOT/$dir"
        if mvn clean package -DskipTests -q 2>/tmp/build_${dir}.log; then
            echo -e "  ${GREEN}OK${NC}"
        else
            echo -e "  ${RED}FALHOU - ver /tmp/build_${dir}.log${NC}"
            ((errors++)) || true
        fi
        cd "$PROJECT_ROOT"
    done

    echo ""
    if [ "$errors" -eq 0 ]; then
        echo -e "${GREEN}Todos os servicos compilados com sucesso.${NC}"
    else
        echo -e "${RED}$errors servico(s) falharam.${NC}"
    fi
    pause
}

# ═══════════════════════════════════════════════════════════════
# 2. DEPLOY
# Ordem correta:
#   1. docker-compose.yml  -> cria a rede mstcc-net + DBs + microsservicos
#   2. docker-compose.monitoring.yml -> join na rede externa
# ═══════════════════════════════════════════════════════════════
deploy_system() {
    echo -e "${BLUE}[DEPLOY] Subindo o sistema...${NC}"
    echo ""
    cd "$PROJECT_ROOT"

    echo -e "${YELLOW}[1/2] Subindo microsservicos e bancos de dados...${NC}"
    docker compose up -d --build

    echo ""
    echo -e "${YELLOW}Aguardando 90s para os servicos iniciarem...${NC}"
    for i in {90..1}; do
        printf "\r  %02ds restantes..." $i
        sleep 1
    done
    echo ""

    echo -e "${YELLOW}[2/2] Subindo monitoramento (Prometheus + Grafana)...${NC}"
    docker compose -p mstcc-monitoring -f docker-compose.monitoring.yml up -d

    echo ""
    check_health_quick
    echo ""
    echo -e "${GREEN}Sistema pronto!${NC}"
    echo -e "  Grafana:    ${CYAN}http://localhost:3000${NC}  (admin/admin)"
    echo -e "  Eureka:     ${CYAN}http://localhost:8761${NC}"
    echo -e "  Gateway:    ${CYAN}http://localhost:18765${NC}"
    echo ""
    pause
}

# ═══════════════════════════════════════════════════════════════
# 3. FRESH START
# ═══════════════════════════════════════════════════════════════
fresh_start() {
    echo -e "${BLUE}[FRESH START]${NC}"
    echo ""
    echo -e "${RED}Atencao: todos os containers e volumes (dados) serao removidos!${NC}"
    echo -ne "Continuar? (yes/no): "
    read -r confirm
    [ "$confirm" != "yes" ] && { echo "Cancelado."; pause; return; }

    cd "$PROJECT_ROOT"

    echo -e "${CYAN}[1/3] Removendo containers e volumes...${NC}"
    docker compose -p mstcc-monitoring -f docker-compose.monitoring.yml down -v 2>/dev/null || true
    docker stop mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker rm   mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker compose down -v 2>/dev/null || true

    echo -e "${CYAN}[2/3] Compilando servicos...${NC}"
    build_all

    echo -e "${CYAN}[3/3] Fazendo deploy...${NC}"
    deploy_system
}

# ═══════════════════════════════════════════════════════════════
# 4. HEALTH CHECK
# ═══════════════════════════════════════════════════════════════
check_service() {
    local name=$1
    local url=$2
    printf "  %-30s" "$name"
    if curl -f -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}DOWN${NC}"
        return 1
    fi
}

check_health() {
    echo -e "${BLUE}[HEALTH CHECK]${NC}"
    echo ""
    local ok=0
    check_service "Eureka Server"     "http://localhost:8761"                    && ((ok++)) || true
    check_service "API Gateway"       "http://localhost:18765/actuator/health"   && ((ok++)) || true
    check_service "User Service"      "http://localhost:18081/actuator/health"   && ((ok++)) || true
    check_service "Post Service"      "http://localhost:18082/actuator/health"   && ((ok++)) || true
    check_service "Comment Service"   "http://localhost:18083/actuator/health"   && ((ok++)) || true
    check_service "Like Service"      "http://localhost:18084/actuator/health"   && ((ok++)) || true
    check_service "Friendship Service" "http://localhost:18085/actuator/health"  && ((ok++)) || true
    check_service "Prometheus"        "http://localhost:9090/-/ready"            && ((ok++)) || true
    check_service "Grafana"           "http://localhost:3000/api/health"         && ((ok++)) || true
    echo ""
    echo -e "${CYAN}$ok/9 servicos saudaveis${NC}"
    echo ""
    pause
}

check_health_quick() {
    echo -e "${YELLOW}Health check rapido...${NC}"
    curl -f -s http://localhost:8761 > /dev/null 2>&1 \
        && echo -e "  ${GREEN}OK${NC} Eureka" || echo -e "  ${RED}DOWN${NC} Eureka"
    curl -f -s http://localhost:18765/actuator/health > /dev/null 2>&1 \
        && echo -e "  ${GREEN}OK${NC} Gateway" || echo -e "  ${RED}DOWN${NC} Gateway"
}

# ═══════════════════════════════════════════════════════════════
# 5. LOGS
# ═══════════════════════════════════════════════════════════════
view_logs() {
    echo -e "${BLUE}[LOGS] Selecione o servico:${NC}"
    echo "  1) User Service"
    echo "  2) Post Service"
    echo "  3) Comment Service"
    echo "  4) Like Service"
    echo "  5) Friendship Service"
    echo "  6) Gateway"
    echo "  7) Eureka"
    echo "  0) Voltar"
    echo ""
    echo -ne "Opcao: "
    read -r opt
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

# ═══════════════════════════════════════════════════════════════
# K6 - FUNCOES AUXILIARES
# ═══════════════════════════════════════════════════════════════
check_k6() {
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}k6 nao esta instalado.${NC}"
        echo "  macOS: brew install k6"
        echo "  Docs:  https://k6.io/docs/get-started/installation/"
        pause
        return 1
    fi
    return 0
}

run_k6_scenario() {
    local label=$1
    local duration_note=$2
    local extra_args=${3:-""}

    check_k6 || return

    mkdir -p "$RESULTS_DIR"
    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)
    local tag
    tag=$(echo "$label" | tr ' ' '_' | tr '[:upper:]' '[:lower:]')
    local out_json="$RESULTS_DIR/${tag}_${timestamp}.json"
    local out_summary="$RESULTS_DIR/${tag}_${timestamp}_summary.json"

    echo -e "${BLUE}[K6] $label  ($duration_note)${NC}"
    echo -e "${CYAN}  Resultado: $out_json${NC}"
    echo ""

    # shellcheck disable=SC2086
    k6 run \
        --out "json=$out_json" \
        --summary-export "$out_summary" \
        $extra_args \
        "$K6_SCRIPT"

    echo ""
    echo -e "${GREEN}Teste concluido!${NC}"
    echo -e "  Summary: ${CYAN}$out_summary${NC}"
    echo ""
    pause
}

# ═══════════════════════════════════════════════════════════════
# 6-11. CENARIOS K6
# ═══════════════════════════════════════════════════════════════
run_full_suite() {
    run_k6_scenario "Suite Completa" "~18 min"
}

run_baseline() {
    run_k6_scenario "Baseline Load" "2 min" \
        "--env SCENARIO=baseline_load --duration 2m --vus 5"
}

run_steady() {
    run_k6_scenario "Steady Load" "3 min" \
        "--env SCENARIO=steady_load --duration 3m --vus 20"
}

run_stress() {
    run_k6_scenario "Stress Test" "~8 min" \
        "--env SCENARIO=stress_test"
}

run_spike() {
    run_k6_scenario "Spike Test" "~3 min" \
        "--env SCENARIO=spike_test"
}

run_read_heavy() {
    run_k6_scenario "Read-Heavy" "2 min" \
        "--env SCENARIO=read_heavy --duration 2m --vus 30"
}

# ═══════════════════════════════════════════════════════════════
# 12. SEED DATA
# ═══════════════════════════════════════════════════════════════
run_seed() {
    echo -e "${BLUE}[SEED DATA]${NC}"
    bash "$SCRIPT_DIR/seed-data.sh"
    pause
}

# ═══════════════════════════════════════════════════════════════
# 13. RELATORIO DE COMPARACAO
# ═══════════════════════════════════════════════════════════════
generate_report() {
    echo -e "${BLUE}[RELATORIO]${NC}"
    bash "$SCRIPT_DIR/generate-report.sh"
    pause
}

# ═══════════════════════════════════════════════════════════════
# 14. STOP
# ═══════════════════════════════════════════════════════════════
stop_all() {
    echo -e "${BLUE}[STOP] Parando todos os servicos...${NC}"
    cd "$PROJECT_ROOT"
    docker compose -p mstcc-monitoring -f docker-compose.monitoring.yml down 2>/dev/null || true
    docker stop mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker rm   mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker compose down 2>/dev/null || true
    echo -e "${GREEN}Servicos parados.${NC}"
    echo ""
    pause
}

# ═══════════════════════════════════════════════════════════════
# 15. CLEAN
# ═══════════════════════════════════════════════════════════════
clean_all() {
    echo -e "${RED}ATENCAO: remove todos os containers, volumes e imagens!${NC}"
    echo -ne "Continuar? (yes/no): "
    read -r confirm
    [ "$confirm" != "yes" ] && { echo "Cancelado."; pause; return; }

    cd "$PROJECT_ROOT"
    docker compose -p mstcc-monitoring -f docker-compose.monitoring.yml down -v 2>/dev/null || true
    docker stop mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker rm   mstcc_prometheus mstcc_grafana 2>/dev/null || true
    docker compose down -v 2>/dev/null || true
    docker images | grep "microsservice" | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true
    echo -e "${GREEN}Limpeza concluida.${NC}"
    echo ""
    pause
}

# ═══════════════════════════════════════════════════════════════
# 16-18. DASHBOARDS
# ═══════════════════════════════════════════════════════════════
open_url() {
    open "$1" 2>/dev/null || xdg-open "$1" 2>/dev/null || echo "Acesse: $1"
    pause
}

# ═══════════════════════════════════════════════════════════════
# HELPER
# ═══════════════════════════════════════════════════════════════
pause() {
    echo ""
    read -rp "Pressione Enter para continuar..."
}

# ═══════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════
while true; do
    show_menu
    read -r option
    case $option in
        1)  build_all ;;
        2)  deploy_system ;;
        3)  fresh_start ;;
        4)  check_health ;;
        5)  view_logs ;;
        6)  run_full_suite ;;
        7)  run_baseline ;;
        8)  run_steady ;;
        9)  run_stress ;;
        10) run_spike ;;
        11) run_read_heavy ;;
        12) run_seed ;;
        13) generate_report ;;
        14) stop_all ;;
        15) clean_all ;;
        16) open_url "http://localhost:3000" ;;
        17) open_url "http://localhost:8761" ;;
        18) open_url "http://localhost:9090" ;;
        0)  echo -e "${GREEN}Saindo...${NC}"; exit 0 ;;
        *)  echo -e "${RED}Opcao invalida${NC}"; sleep 1 ;;
    esac
done
