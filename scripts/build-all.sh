#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# 🔨 BUILD ALL MICROSERVICES
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

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🔨 BUILD DE TODOS OS MICROSSERVIÇOS                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVICES_BUILT=0
BUILD_ERRORS=0
TOTAL_SERVICES=7

# Lista de serviços para build
SERVICES=(
    "eureka-server-ms:Eureka Server"
    "gateway-service-ms:API Gateway"
    "user-ms:User Service"
    "post-ms:Post Service"
    "comment-ms:Comment Service"
    "like-ms:Like Service"
    "friendship-ms:Friendship Service"
)

# ═══════════════════════════════════════════════════════════════
# Função para build de um serviço
# ═══════════════════════════════════════════════════════════════
build_service() {
    local service_dir=$1
    local service_name=$2
    local service_num=$3
    
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}[$service_num/$TOTAL_SERVICES] Building $service_name${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
    
    if [ ! -d "$PROJECT_ROOT/$service_dir" ]; then
        echo -e "${RED}❌ Diretório não encontrado: $service_dir${NC}"
        ((BUILD_ERRORS++))
        return 1
    fi
    
    cd "$PROJECT_ROOT/$service_dir"
    
    # Verificar se tem pom.xml
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}❌ pom.xml não encontrado em $service_dir${NC}"
        ((BUILD_ERRORS++))
        return 1
    fi
    
    echo -e "${YELLOW}🔧 Executando: mvn clean package -DskipTests${NC}"
    
    # Executar Maven
    if mvn clean package -DskipTests > /tmp/build_${service_dir}.log 2>&1; then
        echo -e "${GREEN}✅ $service_name compilado com sucesso!${NC}"
        
        # Verificar se JAR foi criado
        if ls target/*.jar 1> /dev/null 2>&1; then
            JAR_SIZE=$(du -h target/*.jar 2>/dev/null | head -1 | cut -f1)
            echo -e "   📦 JAR gerado: $JAR_SIZE"
        fi
        
        ((SERVICES_BUILT++))
        return 0
    else
        echo -e "${RED}❌ Erro ao compilar $service_name${NC}"
        echo -e "${YELLOW}Ver log completo: /tmp/build_${service_dir}.log${NC}"
        echo ""
        echo "Últimas linhas do erro:"
        tail -n 20 /tmp/build_${service_dir}.log
        ((BUILD_ERRORS++))
        return 1
    fi
}

# ═══════════════════════════════════════════════════════════════
# Verificações iniciais
# ═══════════════════════════════════════════════════════════════
echo -e "${BLUE}🗂️  Script location: $SCRIPT_DIR${NC}"
echo -e "${BLUE}🗂️  Project root: $PROJECT_ROOT${NC}"
echo ""
echo -e "${YELLOW}📋 Verificando pré-requisitos...${NC}"

# Verificar Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven não está instalado!${NC}"
    echo "Instale com:"
    echo "  - macOS: brew install maven"
    echo "  - Linux: apt-get install maven"
    echo "  - Windows: https://maven.apache.org/download.cgi"
    exit 1
fi

MAVEN_VERSION=$(mvn -version | head -n 1)
echo -e "${GREEN}✅ Maven encontrado: $MAVEN_VERSION${NC}"

# Verificar Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java não está instalado!${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo -e "${GREEN}✅ Java encontrado: $JAVA_VERSION${NC}"

# Verificar versão do Java (deve ser 17+)
JAVA_MAJOR_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_MAJOR_VERSION" -lt 17 ] 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Aviso: Java 17+ é recomendado (encontrado: $JAVA_MAJOR_VERSION)${NC}"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# Build de cada serviço
# ═══════════════════════════════════════════════════════════════
START_TIME=$(date +%s)
SERVICE_NUM=1

for service_info in "${SERVICES[@]}"; do
    IFS=':' read -r service_dir service_name <<< "$service_info"
    build_service "$service_dir" "$service_name" "$SERVICE_NUM" || true
    ((SERVICE_NUM++))
    cd "$PROJECT_ROOT"
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# ═══════════════════════════════════════════════════════════════
# Resumo Final
# ═══════════════════════════════════════════════════════════════
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  📊 RESUMO DO BUILD                                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ "$BUILD_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}   ✅ TODOS OS SERVIÇOS COMPILADOS COM SUCESSO!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${CYAN}Serviços compilados: $SERVICES_BUILT/$TOTAL_SERVICES${NC}"
    echo -e "${CYAN}Tempo total: ${DURATION}s${NC}"
    echo ""
    echo -e "${YELLOW}Próximos passos:${NC}"
    echo "1. Deploy com otimizações (reset completo):"
    echo "   ./fresh-start-optimized.sh"
    echo ""
    echo "2. Ou deploy normal:"
    echo "   ./deploy-complete-v2.sh"
    echo ""
    echo "3. Monitorar:"
    echo "   open http://localhost:3000 (Grafana)"
    echo ""
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}   ❌ FALHAS NO BUILD!${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${RED}Serviços com erro: $BUILD_ERRORS${NC}"
    echo -e "${GREEN}Serviços compilados: $SERVICES_BUILT/$TOTAL_SERVICES${NC}"
    echo ""
    echo -e "${YELLOW}Verifique os logs em: /tmp/build_*.log${NC}"
    echo ""
    echo "Comandos úteis:"
    echo "  - Ver logs: cat /tmp/build_<servico>.log"
    echo "  - Limpar Maven: mvn clean"
    echo "  - Recompilar: cd <servico> && mvn clean package"
    echo ""
    exit 1
fi