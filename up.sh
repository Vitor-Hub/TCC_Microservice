#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# 🔧 RECOVERY: Restore pom.xml and Apply Correct Fixes
# ═══════════════════════════════════════════════════════════════

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🔧 RECOVERING POM.XML FILES                          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

SERVICES=("user-ms" "comment-ms" "like-ms" "friendship-ms")

echo -e "${YELLOW}📋 Step 1: Restoring from backups...${NC}"
echo ""

for service in "${SERVICES[@]}"; do
    POM="$service/pom.xml"
    BAK="$service/pom.xml.bak"
    
    if [ -f "$BAK" ]; then
        echo -e "${YELLOW}  Restoring $service...${NC}"
        cp "$BAK" "$POM"
        echo -e "${GREEN}  ✅ Restored from backup${NC}"
    else
        echo -e "${RED}  ⚠️  No backup found for $service${NC}"
    fi
done

echo ""
echo -e "${YELLOW}📋 Step 2: Adding cache dependencies correctly...${NC}"
echo ""

# Function to add cache dependencies properly
add_cache_deps() {
    local service=$1
    local pom_file="$service/pom.xml"
    
    echo -e "${YELLOW}  Processing $service...${NC}"
    
    # Check if already has cache dependency
    if grep -q "spring-boot-starter-cache" "$pom_file"; then
        echo -e "${BLUE}    ℹ️  Cache dependencies already exist${NC}"
        return 0
    fi
    
    # Find the line number of </dependencies>
    local line_num=$(grep -n "</dependencies>" "$pom_file" | head -1 | cut -d: -f1)
    
    if [ -z "$line_num" ]; then
        echo -e "${RED}    ❌ Could not find </dependencies> tag${NC}"
        return 1
    fi
    
    # Create temp file with new dependencies
    head -n $((line_num - 1)) "$pom_file" > "$pom_file.tmp"
    
    # Add cache dependencies
    cat >> "$pom_file.tmp" << 'EOF'

        <!-- Cache dependencies for performance optimization -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>3.1.8</version>
        </dependency>
EOF
    
    # Add closing tag and rest of file
    tail -n +$line_num "$pom_file" >> "$pom_file.tmp"
    
    # Replace original file
    mv "$pom_file.tmp" "$pom_file"
    
    echo -e "${GREEN}    ✅ Added cache dependencies${NC}"
}

# Apply to each service
for service in "${SERVICES[@]}"; do
    add_cache_deps "$service"
done

echo ""
echo -e "${YELLOW}📋 Step 3: Validating pom.xml files...${NC}"
echo ""

VALID_COUNT=0
for service in "${SERVICES[@]}"; do
    echo -e "${YELLOW}  Validating $service...${NC}"
    if mvn -f "$service/pom.xml" validate 2>/dev/null; then
        echo -e "${GREEN}    ✅ Valid XML${NC}"
        ((VALID_COUNT++))
    else
        echo -e "${RED}    ❌ Invalid XML${NC}"
    fi
done

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  📊 RECOVERY SUMMARY                                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ $VALID_COUNT -eq ${#SERVICES[@]} ]; then
    echo -e "${GREEN}✅ All pom.xml files recovered and fixed!${NC}"
    echo ""
    echo "🧪 Next steps:"
    echo "  1. Test build:"
    echo "     mvn clean install -DskipTests"
    echo ""
    echo "  2. If successful, rebuild all services:"
    echo "     ./scripts/build-all.sh"
    echo ""
else
    echo -e "${RED}⚠️  Some pom.xml files may still have issues${NC}"
    echo ""
    echo "Please share the content of one pom.xml file around line 77"
    echo "so I can see the exact structure."
fi