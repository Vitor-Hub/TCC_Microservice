#!/bin/bash

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         🚀 SCRIPT FINAL - Corrigir TUDO           ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════╝${NC}"
echo ""

# Função para gerar pom.xml completo
generate_pom() {
    local service=$1
    local artifact_id=$2
    local description=$3
    local pom_file="${service}/pom.xml"
    
    if [ ! -d "$service" ]; then
        echo -e "${RED}❌ Diretório não encontrado: ${service}${NC}"
        return 1
    fi
    
    echo -e "${YELLOW}📝 Gerando ${service}/pom.xml...${NC}"
    
    # Fazer backup
    if [ -f "$pom_file" ]; then
        cp "$pom_file" "${pom_file}.backup.$(date +%s)"
    fi
    
    # Criar pom.xml (sem usar heredoc para evitar problemas)
    cat > "$pom_file" << 'ENDOFFILE'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>
    <groupId>com.mstcc</groupId>
    <artifactId>SERVICE_NAME</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>SERVICE_NAME</name>
    <description>SERVICE_DESCRIPTION</description>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
ENDOFFILE

    # Substituir placeholders
    sed -i.bak "s/SERVICE_NAME/$artifact_id/g" "$pom_file"
    sed -i.bak "s/SERVICE_DESCRIPTION/$description/g" "$pom_file"
    rm -f "${pom_file}.bak"
    
    echo -e "${GREEN}   ✅ ${service}/pom.xml criado${NC}"
}

# Função para compilar
compile_service() {
    local service=$1
    echo -e "${YELLOW}🔨 Compilando ${service}...${NC}"
    
    cd "$service" || return 1
    
    if mvn clean package -DskipTests > /dev/null 2>&1; then
        echo -e "${GREEN}   ✅ ${service} compilado com sucesso${NC}"
        cd ..
        return 0
    else
        echo -e "${RED}   ❌ Erro ao compilar ${service}${NC}"
        echo -e "${YELLOW}      Executando mvn novamente para ver o erro...${NC}"
        mvn clean package -DskipTests
        cd ..
        return 1
    fi
}

# PASSO 1: Restaurar backups se existirem (caso o script tenha falhado antes)
echo -e "${BLUE}═══ PASSO 1: Limpando arquivos antigos ═══${NC}"
for service in user-ms post-ms comment-ms friendship-ms like-ms; do
    if [ -f "${service}/pom.xml.backup" ]; then
        echo -e "${YELLOW}   Restaurando backup de ${service}${NC}"
        mv "${service}/pom.xml.backup" "${service}/pom.xml"
    fi
done
echo ""

# PASSO 2: Gerar todos os pom.xml
echo -e "${BLUE}═══ PASSO 2: Gerando pom.xml corretos ═══${NC}"
generate_pom "user-ms" "user-ms" "User Microservice for TCC"
generate_pom "post-ms" "post-ms" "Post Microservice for TCC"
generate_pom "comment-ms" "comment-ms" "Comment Microservice for TCC"
generate_pom "friendship-ms" "friendship-ms" "Friendship Microservice for TCC"
generate_pom "like-ms" "like-ms" "Like Microservice for TCC"
echo ""

# PASSO 3: Compilar tudo
echo -e "${BLUE}═══ PASSO 3: Compilando todos os serviços ═══${NC}"
SERVICES=("user-ms" "post-ms" "comment-ms" "friendship-ms" "like-ms")
FAILED=()

for service in "${SERVICES[@]}"; do
    if ! compile_service "$service"; then
        FAILED+=("$service")
    fi
done

echo ""

if [ ${#FAILED[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ Todos os serviços compilados com sucesso!${NC}"
    echo ""
    
    # PASSO 4: Rebuild Docker
    echo -e "${BLUE}═══ PASSO 4: Reconstruindo imagens Docker ═══${NC}"
    echo -e "${YELLOW}Parando containers...${NC}"
    docker compose down
    
    echo -e "${YELLOW}Reconstruindo imagens (isso pode demorar)...${NC}"
    docker compose build --no-cache
    
    echo ""
    echo -e "${GREEN}🎉 SUCESSO! Tudo pronto para rodar!${NC}"
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${GREEN}Para subir os serviços:${NC}"
    echo -e "   ${YELLOW}docker compose up -d${NC}"
    echo ""
    echo -e "${GREEN}Para ver logs em tempo real:${NC}"
    echo -e "   ${YELLOW}docker compose logs -f${NC}"
    echo ""
    echo -e "${GREEN}Para verificar Eureka Dashboard:${NC}"
    echo -e "   ${YELLOW}http://localhost:8761${NC}"
    echo ""
    echo -e "${GREEN}Para verificar se os serviços estão registrados:${NC}"
    echo -e "   ${YELLOW}curl http://localhost:8761/eureka/apps${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
else
    echo -e "${RED}❌ Falha ao compilar os seguintes serviços:${NC}"
    for failed in "${FAILED[@]}"; do
        echo -e "   - ${failed}"
    done
    echo ""
    echo -e "${YELLOW}💡 Dica: Os backups dos pom.xml originais estão salvos com timestamp.${NC}"
    echo -e "${YELLOW}💡 Verifique os erros de compilação acima.${NC}"
    exit 1
fi