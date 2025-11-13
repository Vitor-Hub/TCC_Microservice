#!/bin/bash

# ===================================
# 🎯 SCRIPT DE TESTES AUTOMATIZADOS
# TCC - Microsserviços vs Monolítico
# ===================================

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Diretórios
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ARCH_TYPE="${1:-microservices}" # microservices ou monolithic

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🚀 TCC - TESTE DE CARGA AUTOMATIZADO   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# ===================================
# 1. VERIFICAÇÕES INICIAIS
# ===================================
echo -e "${YELLOW}📋 Verificando pré-requisitos...${NC}"

# Verifica se k6 está instalado
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}❌ K6 não está instalado!${NC}"
    echo "Instale com: brew install k6 (macOS) ou https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# Verifica se curl está disponível
if ! command -v curl &> /dev/null; then
    echo -e "${RED}❌ curl não está instalado!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Pré-requisitos OK${NC}"
echo ""

# ===================================
# 2. VERIFICA SERVIÇOS
# ===================================
echo -e "${YELLOW}🔍 Verificando se os serviços estão rodando...${NC}"

GATEWAY_URL="http://localhost:8765"
EUREKA_URL="http://localhost:8761"
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"

check_service() {
    local name=$1
    local url=$2
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|302"; then
        echo -e "${GREEN}✅ $name está rodando${NC}"
        return 0
    else
        echo -e "${RED}❌ $name não está respondendo em $url${NC}"
        return 1
    fi
}

# Verifica serviços essenciais
check_service "API Gateway" "$GATEWAY_URL/actuator/health" || { echo "Inicie os serviços primeiro!"; exit 1; }
check_service "Eureka Server" "$EUREKA_URL/actuator/health" || { echo "Inicie o Eureka Server!"; exit 1; }
check_service "Prometheus" "$PROMETHEUS_URL/-/healthy" || echo "⚠️  Prometheus não está rodando (opcional)"
check_service "Grafana" "$GRAFANA_URL/api/health" || echo "⚠️  Grafana não está rodando (opcional)"

echo ""

# ===================================
# 3. PREPARA AMBIENTE
# ===================================
echo -e "${YELLOW}📁 Preparando diretório de resultados...${NC}"

mkdir -p "$RESULTS_DIR"
RESULT_FILE="${RESULTS_DIR}/${ARCH_TYPE}_${TIMESTAMP}"

echo -e "${GREEN}✅ Salvando resultados em: $RESULT_FILE${NC}"
echo ""

# ===================================
# 4. COLETA MÉTRICAS INICIAIS
# ===================================
echo -e "${YELLOW}📊 Coletando métricas iniciais...${NC}"

collect_initial_metrics() {
    echo "=== MÉTRICAS INICIAIS ===" > "${RESULT_FILE}_initial.txt"
    echo "Timestamp: $(date)" >> "${RESULT_FILE}_initial.txt"
    echo "" >> "${RESULT_FILE}_initial.txt"
    
    # Docker stats
    echo "--- Docker Stats ---" >> "${RESULT_FILE}_initial.txt"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" >> "${RESULT_FILE}_initial.txt"
    echo "" >> "${RESULT_FILE}_initial.txt"
    
    # Prometheus metrics (se disponível)
    if curl -s "$PROMETHEUS_URL/-/healthy" &> /dev/null; then
        echo "--- Prometheus Targets ---" >> "${RESULT_FILE}_initial.txt"
        curl -s "$PROMETHEUS_URL/api/v1/targets" | python3 -m json.tool >> "${RESULT_FILE}_initial.txt" 2>/dev/null || echo "Não foi possível coletar targets do Prometheus"
    fi
}

collect_initial_metrics
echo -e "${GREEN}✅ Métricas iniciais coletadas${NC}"
echo ""

# ===================================
# 5. AGUARDA ESTABILIZAÇÃO
# ===================================
echo -e "${YELLOW}⏳ Aguardando estabilização dos serviços (30s)...${NC}"
sleep 30
echo -e "${GREEN}✅ Serviços estabilizados${NC}"
echo ""

# ===================================
# 6. EXECUTA TESTES K6
# ===================================
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     🚀 INICIANDO TESTES DE CARGA          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Configura opções do K6
K6_SCRIPT="${SCRIPT_DIR}/k6-load-test.js"
K6_OPTIONS="--out json=${RESULT_FILE}_k6.json --summary-export=${RESULT_FILE}_summary.json"

# Verifica se o script existe
if [ ! -f "$K6_SCRIPT" ]; then
    echo -e "${RED}❌ Script K6 não encontrado: $K6_SCRIPT${NC}"
    exit 1
fi

echo -e "${YELLOW}📝 Executando K6 Test...${NC}"
echo "Script: $K6_SCRIPT"
echo "Arquitetura: $ARCH_TYPE"
echo ""

# Executa K6 e salva output
k6 run $K6_OPTIONS \
    -e BASE_URL="$GATEWAY_URL" \
    "$K6_SCRIPT" | tee "${RESULT_FILE}_output.log"

K6_EXIT_CODE=$?

if [ $K6_EXIT_CODE -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Testes K6 concluídos com sucesso!${NC}"
else
    echo ""
    echo -e "${RED}⚠️  Testes K6 finalizaram com erros (código: $K6_EXIT_CODE)${NC}"
fi

echo ""

# ===================================
# 7. COLETA MÉTRICAS FINAIS
# ===================================
echo -e "${YELLOW}📊 Coletando métricas finais...${NC}"

collect_final_metrics() {
    echo "=== MÉTRICAS FINAIS ===" > "${RESULT_FILE}_final.txt"
    echo "Timestamp: $(date)" >> "${RESULT_FILE}_final.txt"
    echo "" >> "${RESULT_FILE}_final.txt"
    
    # Docker stats
    echo "--- Docker Stats ---" >> "${RESULT_FILE}_final.txt"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" >> "${RESULT_FILE}_final.txt"
    echo "" >> "${RESULT_FILE}_final.txt"
    
    # Container logs (últimas 50 linhas de cada serviço)
    echo "--- Container Logs (Últimas 50 linhas) ---" >> "${RESULT_FILE}_final.txt"
    for container in micro-user-service micro-post-service micro-comment-service micro-like-service micro-friendship-service micro-api-gateway; do
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            echo "" >> "${RESULT_FILE}_final.txt"
            echo "=== $container ===" >> "${RESULT_FILE}_final.txt"
            docker logs --tail 50 "$container" >> "${RESULT_FILE}_final.txt" 2>&1
        fi
    done
}

collect_final_metrics
echo -e "${GREEN}✅ Métricas finais coletadas${NC}"
echo ""

# ===================================
# 8. GERA RELATÓRIO RESUMIDO
# ===================================
echo -e "${YELLOW}📈 Gerando relatório resumido...${NC}"

generate_summary() {
    local summary_file="${RESULT_FILE}_report.txt"
    
    echo "╔═══════════════════════════════════════════════════════╗" > "$summary_file"
    echo "║       📊 RELATÓRIO DE TESTES - $ARCH_TYPE" >> "$summary_file"
    echo "╚═══════════════════════════════════════════════════════╝" >> "$summary_file"
    echo "" >> "$summary_file"
    echo "Data/Hora: $(date)" >> "$summary_file"
    echo "Arquitetura: $ARCH_TYPE" >> "$summary_file"
    echo "" >> "$summary_file"
    
    # Extrai métricas do JSON do K6 (se disponível)
    if [ -f "${RESULT_FILE}_summary.json" ]; then
        echo "═══════════════════════════════════════════════════════" >> "$summary_file"
        echo "🎯 MÉTRICAS PRINCIPAIS" >> "$summary_file"
        echo "═══════════════════════════════════════════════════════" >> "$summary_file"
        
        # Usa jq se disponível, senão usa python
        if command -v jq &> /dev/null; then
            echo "" >> "$summary_file"
            echo "📊 Requisições HTTP:" >> "$summary_file"
            jq -r '.metrics.http_reqs.values | "  Total: \(.count // 0)\n  Taxa: \(.rate // 0) req/s"' "${RESULT_FILE}_summary.json" >> "$summary_file" 2>/dev/null || echo "  Dados não disponíveis" >> "$summary_file"
            
            echo "" >> "$summary_file"
            echo "⏱️  Duração das Requisições:" >> "$summary_file"
            jq -r '.metrics.http_req_duration.values | "  Média: \(.avg // 0)ms\n  Mediana: \(.med // 0)ms\n  P90: \(.["p(90)"] // 0)ms\n  P95: \(.["p(95)"] // 0)ms\n  P99: \(.["p(99)"] // 0)ms\n  Máximo: \(.max // 0)ms"' "${RESULT_FILE}_summary.json" >> "$summary_file" 2>/dev/null || echo "  Dados não disponíveis" >> "$summary_file"
            
            echo "" >> "$summary_file"
            echo "✅ Taxa de Sucesso:" >> "$summary_file"
            jq -r '.metrics.http_req_failed.values | "  Sucesso: \(100 - (.rate * 100))%\n  Falhas: \(.rate * 100)%"' "${RESULT_FILE}_summary.json" >> "$summary_file" 2>/dev/null || echo "  Dados não disponíveis" >> "$summary_file"
        else
            echo "  (Instale 'jq' para ver métricas detalhadas)" >> "$summary_file"
        fi
    fi
    
    echo "" >> "$summary_file"
    echo "═══════════════════════════════════════════════════════" >> "$summary_file"
    echo "📁 ARQUIVOS GERADOS" >> "$summary_file"
    echo "═══════════════════════════════════════════════════════" >> "$summary_file"
    echo "  - Métricas Iniciais: ${RESULT_FILE}_initial.txt" >> "$summary_file"
    echo "  - Métricas Finais: ${RESULT_FILE}_final.txt" >> "$summary_file"
    echo "  - Output K6: ${RESULT_FILE}_output.log" >> "$summary_file"
    echo "  - JSON K6: ${RESULT_FILE}_k6.json" >> "$summary_file"
    echo "  - Summary JSON: ${RESULT_FILE}_summary.json" >> "$summary_file"
    echo "  - Este Relatório: $summary_file" >> "$summary_file"
    echo "" >> "$summary_file"
    
    # Exibe o relatório na tela também
    cat "$summary_file"
}

generate_summary
echo ""

# ===================================
# 9. INSTRUÇÕES FINAIS
# ===================================
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          ✅ TESTES CONCLUÍDOS!            ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}📊 Próximos Passos:${NC}"
echo ""
echo "1. 📈 Visualize os dashboards no Grafana:"
echo "   ${GRAFANA_URL}"
echo ""
echo "2. 🔍 Analise as métricas no Prometheus:"
echo "   ${PROMETHEUS_URL}"
echo ""
echo "3. 📁 Resultados salvos em:"
echo "   $RESULTS_DIR"
echo ""
echo "4. 📊 Para gerar gráficos comparativos:"
echo "   python3 scripts/generate-report.py $RESULTS_DIR"
echo ""
echo -e "${GREEN}🎓 Boa sorte com seu TCC!${NC}"