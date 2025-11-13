#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# ✅ SCRIPT DE VALIDAÇÃO PRÉ-TESTE
# TCC - Microsserviços vs Monolítico
# ═══════════════════════════════════════════════════════════════

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ERRORS=0
WARNINGS=0

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  ✅ VALIDAÇÃO DE AMBIENTE - TCC                       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════
# 1. VERIFICA PRÉ-REQUISITOS
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}📋 VERIFICANDO PRÉ-REQUISITOS${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

check_command() {
    local cmd=$1
    local name=$2
    local install_hint=$3
    
    if command -v "$cmd" &> /dev/null; then
        local version=$("$cmd" --version 2>&1 | head -n1 || echo "unknown")
        echo -e "${GREEN}✅ $name está instalado${NC} ($version)"
        return 0
    else
        echo -e "${RED}❌ $name não está instalado${NC}"
        echo -e "${YELLOW}   Instale com: $install_hint${NC}"
        ((ERRORS++))
        return 1
    fi
}

check_command "docker" "Docker" "https://docs.docker.com/get-docker/"
check_command "docker-compose" "Docker Compose" "https://docs.docker.com/compose/install/"
check_command "curl" "cURL" "apt-get install curl (Linux) ou brew install curl (macOS)"
check_command "k6" "K6 Load Testing" "brew install k6 (macOS) ou https://k6.io/docs/getting-started/installation/"
check_command "jq" "jq (JSON processor)" "brew install jq (macOS) ou apt-get install jq (Linux)" || WARNINGS=$((WARNINGS+1))
check_command "python3" "Python 3" "https://www.python.org/downloads/"

echo ""

# ═══════════════════════════════════════════════════════════════
# 2. VERIFICA DOCKER
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}🐳 VERIFICANDO DOCKER${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

if docker info &> /dev/null; then
    echo -e "${GREEN}✅ Docker está rodando${NC}"
    
    # Verifica memória alocada
    DOCKER_MEM=$(docker info 2>/dev/null | grep "Total Memory" | awk '{print $3 $4}')
    echo -e "   Memória disponível: $DOCKER_MEM"
    
    # Verifica se há pelo menos 4GB
    MEM_VALUE=$(docker info 2>/dev/null | grep "Total Memory" | awk '{print $3}')
    if (( $(echo "$MEM_VALUE < 4" | bc -l 2>/dev/null || echo 0) )); then
        echo -e "${YELLOW}⚠️  Recomendado: Pelo menos 4GB de RAM para Docker${NC}"
        ((WARNINGS++))
    fi
else
    echo -e "${RED}❌ Docker não está rodando${NC}"
    echo -e "${YELLOW}   Inicie o Docker Desktop ou docker daemon${NC}"
    ((ERRORS++))
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 3. VERIFICA PORTAS
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}🔌 VERIFICANDO PORTAS${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

REQUIRED_PORTS=(
    "5433:PostgreSQL User DB"
    "5434:PostgreSQL Post DB"
    "5435:PostgreSQL Comment DB"
    "5436:PostgreSQL Like DB"
    "5437:PostgreSQL Friendship DB"
    "8761:Eureka Server"
    "8765:API Gateway"
    "18081:User Service"
    "18082:Post Service"
    "18083:Comment Service"
    "18084:Friendship Service"
    "18085:Like Service"
)

OPTIONAL_PORTS=(
    "9090:Prometheus"
    "3000:Grafana"
)

check_port() {
    local port=$1
    local service=$2
    local optional=${3:-false}
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an 2>/dev/null | grep -q ":$port.*LISTEN"; then
        echo -e "${GREEN}✅ Porta $port ($service) está em uso${NC}"
        return 0
    else
        if [ "$optional" = true ]; then
            echo -e "${YELLOW}⚠️  Porta $port ($service) está livre (opcional)${NC}"
            ((WARNINGS++))
        else
            echo -e "${RED}❌ Porta $port ($service) está livre - serviço não está rodando${NC}"
            ((ERRORS++))
        fi
        return 1
    fi
}

echo "Portas Essenciais:"
for port_info in "${REQUIRED_PORTS[@]}"; do
    IFS=':' read -r port service <<< "$port_info"
    check_port "$port" "$service"
done

echo ""
echo "Portas Opcionais (Monitoramento):"
for port_info in "${OPTIONAL_PORTS[@]}"; do
    IFS=':' read -r port service <<< "$port_info"
    check_port "$port" "$service" true
done

echo ""

# ═══════════════════════════════════════════════════════════════
# 4. VERIFICA CONTAINERS DOCKER
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}📦 VERIFICANDO CONTAINERS DOCKER${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

REQUIRED_CONTAINERS=(
    "micro-eureka:Eureka Server"
    "micro-api-gateway:API Gateway"
    "micro-user-service:User Service"
    "micro-post-service:Post Service"
    "micro-comment-service:Comment Service"
    "micro-like-service:Like Service"
    "micro-friendship-service:Friendship Service"
    "user_ms_db:User Database"
    "post_ms_db:Post Database"
    "comment_ms_db:Comment Database"
    "like_ms_db:Like Database"
    "friendship_ms_db:Friendship Database"
)

check_container() {
    local container=$1
    local service=$2
    
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        local status=$(docker inspect --format='{{.State.Status}}' "$container")
        local health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "no-healthcheck")
        
        if [ "$status" = "running" ]; then
            if [ "$health" = "healthy" ] || [ "$health" = "no-healthcheck" ]; then
                echo -e "${GREEN}✅ $service ($container) está rodando${NC}"
                if [ "$health" != "no-healthcheck" ]; then
                    echo -e "   Status de saúde: $health"
                fi
            else
                echo -e "${YELLOW}⚠️  $service ($container) está rodando mas não está saudável${NC}"
                echo -e "   Status de saúde: $health"
                ((WARNINGS++))
            fi
        else
            echo -e "${RED}❌ $service ($container) está no estado: $status${NC}"
            ((ERRORS++))
        fi
    else
        echo -e "${RED}❌ $service ($container) não está rodando${NC}"
        ((ERRORS++))
    fi
}

for container_info in "${REQUIRED_CONTAINERS[@]}"; do
    IFS=':' read -r container service <<< "$container_info"
    check_container "$container" "$service"
done

echo ""

# ═══════════════════════════════════════════════════════════════
# 5. TESTA CONECTIVIDADE DOS SERVIÇOS
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}🌐 TESTANDO CONECTIVIDADE DOS SERVIÇOS${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null)
    
    if [ "$http_code" = "$expected_status" ] || [ "$http_code" = "200" ] || [ "$http_code" = "302" ]; then
        echo -e "${GREEN}✅ $name está respondendo${NC} (HTTP $http_code)"
        return 0
    else
        echo -e "${RED}❌ $name não está respondendo corretamente${NC} (HTTP $http_code)"
        ((ERRORS++))
        return 1
    fi
}

test_endpoint "Eureka Server" "http://localhost:8761/actuator/health"
test_endpoint "API Gateway" "http://localhost:8765/actuator/health"
test_endpoint "User Service (direto)" "http://localhost:18081/actuator/health"
test_endpoint "User Service (via Gateway)" "http://localhost:8765/user-ms/actuator/health"
test_endpoint "Post Service (via Gateway)" "http://localhost:8765/post-ms/actuator/health"
test_endpoint "Comment Service (via Gateway)" "http://localhost:8765/comment-ms/actuator/health"
test_endpoint "Like Service (via Gateway)" "http://localhost:8765/like-ms/actuator/health"
test_endpoint "Friendship Service (via Gateway)" "http://localhost:8765/friendship-ms/actuator/health"

echo ""

# ═══════════════════════════════════════════════════════════════
# 6. VERIFICA EUREKA SERVICE REGISTRY
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}🛰️ VERIFICANDO SERVIÇOS REGISTRADOS NO EUREKA${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

EUREKA_URL="http://localhost:8761/eureka/apps"
EUREKA_RESPONSE=$(curl -s -H "Accept: application/json" "$EUREKA_URL" 2>/dev/null)

if [ $? -eq 0 ]; then
    # Gateway não precisa estar registrado - ele apenas consome o Eureka
    EXPECTED_SERVICES=("USER-MS" "POST-MS" "COMMENT-MS" "LIKE-MS" "FRIENDSHIP-MS")
    
    for service in "${EXPECTED_SERVICES[@]}"; do
        if echo "$EUREKA_RESPONSE" | grep -q "\"$service\""; then
            echo -e "${GREEN}✅ $service está registrado no Eureka${NC}"
        else
            echo -e "${RED}❌ $service NÃO está registrado no Eureka${NC}"
            ((ERRORS++))
        fi
    done
    
    # Informativo sobre o Gateway (não conta como erro)
    if echo "$EUREKA_RESPONSE" | grep -q "\"GATEWAY-SERVICE\""; then
        echo -e "${GREEN}ℹ️  GATEWAY-SERVICE está registrado no Eureka (opcional)${NC}"
    else
        echo -e "${YELLOW}ℹ️  GATEWAY-SERVICE não está registrado (normal - ele apenas consome o Eureka)${NC}"
    fi
else
    echo -e "${RED}❌ Não foi possível consultar o Eureka${NC}"
    ((ERRORS++))
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 7. TESTE FUNCIONAL BÁSICO (CREATE USER)
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}🧪 TESTE FUNCIONAL BÁSICO${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

echo "Tentando criar um usuário de teste..."

TEST_USER_PAYLOAD='{"name":"Validation Test User","email":"validation_'$(date +%s)'@test.com","bio":"Test user for validation"}'
CREATE_RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "$TEST_USER_PAYLOAD" \
    -w "\nHTTP_CODE:%{http_code}" \
    "http://localhost:8765/user-ms/api/users" 2>/dev/null)

HTTP_CODE=$(echo "$CREATE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | sed '/HTTP_CODE:/d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✅ Criação de usuário funcionou!${NC}"
    echo "   Resposta: ${RESPONSE_BODY:0:100}..."
    
    # Tenta extrair ID do usuário
    if command -v jq &> /dev/null; then
        USER_ID=$(echo "$RESPONSE_BODY" | jq -r '.id' 2>/dev/null)
        if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ]; then
            echo "   ID do usuário criado: $USER_ID"
            
            # Tenta buscar o usuário
            echo ""
            echo "Tentando buscar o usuário criado..."
            GET_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "http://localhost:8765/user-ms/api/users/$USER_ID" 2>/dev/null)
            GET_HTTP_CODE=$(echo "$GET_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
            
            if [ "$GET_HTTP_CODE" = "200" ]; then
                echo -e "${GREEN}✅ Busca de usuário funcionou!${NC}"
            else
                echo -e "${YELLOW}⚠️  Busca de usuário retornou código: $GET_HTTP_CODE${NC}"
                ((WARNINGS++))
            fi
        fi
    fi
else
    echo -e "${RED}❌ Criação de usuário falhou!${NC}"
    echo "   HTTP Code: $HTTP_CODE"
    echo "   Resposta: ${RESPONSE_BODY:0:200}"
    ((ERRORS++))
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 8. VERIFICA RECURSOS DO SISTEMA
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}💻 RECURSOS DO SISTEMA${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

echo "Docker Stats (últimos 5 segundos):"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | head -n 15

echo ""

# ═══════════════════════════════════════════════════════════════
# 9. VERIFICA ARQUIVOS NECESSÁRIOS
# ═══════════════════════════════════════════════════════════════
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}📁 VERIFICANDO ARQUIVOS NECESSÁRIOS${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

check_file() {
    local file=$1
    local description=$2
    
    if [ -f "$SCRIPT_DIR/$file" ]; then
        echo -e "${GREEN}✅ $description existe${NC}"
        echo -e "   Localização: $SCRIPT_DIR/$file"
    else
        echo -e "${RED}❌ $description não encontrado${NC}"
        echo -e "   Esperado em: $SCRIPT_DIR/$file"
        ((ERRORS++))
    fi
}

check_file "../docker-compose.yml" "Docker Compose (Microsserviços)"
check_file "../docker-compose.db.yml" "Docker Compose (Databases)"
check_file "../k6-load-test.js" "Script K6 de teste de carga"
check_file "run-load-test.sh" "Script de execução de testes"
check_file "generate-report.py" "Gerador de relatórios Python"

# Verifica se diretório de resultados existe (na raiz)
if [ ! -d "$SCRIPT_DIR/../test-results" ]; then
    echo -e "${YELLOW}⚠️  Diretório test-results não existe, será criado${NC}"
    mkdir -p "$SCRIPT_DIR/../test-results"
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ Diretório test-results existe${NC}"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 10. RESUMO FINAL
# ═══════════════════════════════════════════════════════════════
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  📊 RESUMO DA VALIDAÇÃO                               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}   ✅ TUDO OK! Sistema pronto para testes!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${CYAN}Próximos passos:${NC}"
    echo "1. Execute os testes: ./run-load-test.sh microservices"
    echo "2. Monitore no Grafana: http://localhost:3000"
    echo "3. Gere relatórios: python3 generate-report.py test-results/"
    echo ""
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}   ⚠️  Sistema OK com $WARNINGS avisos${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Os avisos não impedem a execução dos testes, mas podem"
    echo "afetar a qualidade dos resultados ou funcionalidades opcionais."
    echo ""
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}   ❌ FALHA NA VALIDAÇÃO!${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${RED}Encontrados:${NC}"
    echo -e "  • ${RED}$ERRORS erros críticos${NC}"
    echo -e "  • ${YELLOW}$WARNINGS avisos${NC}"
    echo ""
    echo "Corrija os erros acima antes de executar os testes."
    echo ""
    echo "Comandos úteis:"
    echo "  • Verificar logs: docker logs <container_name>"
    echo "  • Reiniciar serviços: docker-compose restart"
    echo "  • Ver status: docker ps -a"
    echo ""
    exit 1
fi
