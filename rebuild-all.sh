#!/bin/bash
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SERVICES=("eureka-server-ms" "gateway-service-ms" "user-ms" "post-ms" "comment-ms" "friendship-ms" "like-ms")

echo -e "${BLUE}🔨 Iniciando recompilação de todos os microsserviços...${NC}"
echo ""

# Função para compilar um serviço
compile_service() {
    local service=$1
    echo -e "${YELLOW}📦 Compilando ${service}...${NC}"
    
    cd "$service" || exit 1
    
    if mvn clean package -DskipTests > /dev/null 2>&1; then
        echo -e "${GREEN}   ✅ ${service} compilado com sucesso${NC}"
        
        # Verificar se o JAR foi gerado
        if [ -f "target/*.jar" ]; then
            JAR_SIZE=$(du -sh target/*.jar | cut -f1)
            echo -e "      📊 JAR gerado: ${JAR_SIZE}"
        fi
    else
        echo -e "${RED}   ❌ Erro ao compilar ${service}${NC}"
        return 1
    fi
    
    cd ..
}

# Compilar todos os serviços
for service in "${SERVICES[@]}"; do
    if [ -d "$service" ]; then
        compile_service "$service"
        echo ""
    else
        echo -e "${YELLOW}⚠️  Diretório não encontrado: ${service}${NC}"
    fi
done

echo -e "${GREEN}✅ Recompilação concluída!${NC}"
echo ""
echo -e "${BLUE}Próximos passos:${NC}"
echo "1. Reconstruir imagens Docker: ${YELLOW}docker compose build --no-cache${NC}"
echo "2. Subir serviços: ${YELLOW}docker compose up -d${NC}"
echo "3. Verificar logs: ${YELLOW}docker compose logs -f${NC}"