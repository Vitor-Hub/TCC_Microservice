#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# TCC MICROSERVICES - SEED DATA
# Popula dados iniciais para os testes de carga
# ═══════════════════════════════════════════════════════════════

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

BASE_URL="http://localhost:18765"

NUM_USERS=${NUM_USERS:-50}
NUM_POSTS=${NUM_POSTS:-100}
NUM_COMMENTS=${NUM_COMMENTS:-150}
NUM_LIKES=${NUM_LIKES:-200}
NUM_FRIENDSHIPS=${NUM_FRIENDSHIPS:-80}

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         TCC - SEED TEST DATA                           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Usuarios:   ${CYAN}$NUM_USERS${NC}"
echo -e "  Posts:      ${CYAN}$NUM_POSTS${NC}"
echo -e "  Comentarios:${CYAN}$NUM_COMMENTS${NC}"
echo -e "  Likes:      ${CYAN}$NUM_LIKES${NC}"
echo -e "  Amizades:   ${CYAN}$NUM_FRIENDSHIPS${NC}"
echo ""
echo -e "${YELLOW}Verificando gateway...${NC}"
if ! curl -f -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}Gateway nao esta respondendo em $BASE_URL${NC}"
    echo "Execute o deploy primeiro (opcao 2 no manage.sh)"
    exit 1
fi
echo -e "${GREEN}Gateway OK${NC}"
echo ""

# ─────────────────────────────────────────────
# Armazena IDs criados
USER_IDS=()
POST_IDS=()
COMMENT_IDS=()

# ─────────────────────────────────────────────
post_json() {
    local endpoint=$1
    local body=$2
    curl -s -o /tmp/seed_resp.json -w "%{http_code}" \
        -X POST "${BASE_URL}${endpoint}" \
        -H "Content-Type: application/json" \
        -d "$body" 2>/dev/null
}

get_id() {
    # Extrai o campo "id" do JSON na resposta
    grep -o '"id":[0-9]*' /tmp/seed_resp.json 2>/dev/null | head -1 | grep -o '[0-9]*' || true
}

# ─────────────────────────────────────────────
# USUARIOS
# ─────────────────────────────────────────────
echo -e "${YELLOW}Criando $NUM_USERS usuarios...${NC}"
CREATED_USERS=0
for i in $(seq 1 $NUM_USERS); do
    TS=$(date +%s%N | tail -c 6)
    BODY="{\"name\":\"TCC User $i\",\"email\":\"tcc_user_${i}_${TS}@test.com\",\"password\":\"tcc123\"}"
    CODE=$(post_json "/user-ms/api/users" "$BODY")
    if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
        ID=$(get_id)
        if [ -n "$ID" ]; then
            USER_IDS+=("$ID")
            ((CREATED_USERS++)) || true
        fi
    fi
    printf "\r  %d/%d" "$CREATED_USERS" "$NUM_USERS"
done
echo ""
echo -e "${GREEN}$CREATED_USERS usuarios criados${NC}"
echo ""

if [ ${#USER_IDS[@]} -eq 0 ]; then
    echo -e "${RED}Nenhum usuario criado. Verifique os logs do user-ms.${NC}"
    exit 1
fi

# ─────────────────────────────────────────────
# POSTS
# ─────────────────────────────────────────────
echo -e "${YELLOW}Criando $NUM_POSTS posts...${NC}"
CREATED_POSTS=0
for i in $(seq 1 $NUM_POSTS); do
    IDX=$(( (i - 1) % ${#USER_IDS[@]} ))
    UID="${USER_IDS[$IDX]}"
    BODY="{\"user\":{\"id\":$UID},\"content\":\"Post $i sobre microsservicos Spring Boot e comparacao de performance com monolito - TCC 2024\"}"
    CODE=$(post_json "/post-ms/api/posts" "$BODY")
    if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
        ID=$(get_id)
        if [ -n "$ID" ]; then
            POST_IDS+=("$ID")
            ((CREATED_POSTS++)) || true
        fi
    fi
    printf "\r  %d/%d" "$CREATED_POSTS" "$NUM_POSTS"
done
echo ""
echo -e "${GREEN}$CREATED_POSTS posts criados${NC}"
echo ""

# ─────────────────────────────────────────────
# COMENTARIOS
# ─────────────────────────────────────────────
if [ ${#POST_IDS[@]} -gt 0 ]; then
    echo -e "${YELLOW}Criando $NUM_COMMENTS comentarios...${NC}"
    CREATED_COMMENTS=0
    for i in $(seq 1 $NUM_COMMENTS); do
        U_IDX=$(( RANDOM % ${#USER_IDS[@]} ))
        P_IDX=$(( RANDOM % ${#POST_IDS[@]} ))
        UID="${USER_IDS[$U_IDX]}"
        PID="${POST_IDS[$P_IDX]}"
        BODY="{\"userId\":$UID,\"postId\":$PID,\"content\":\"Comentario $i - analisando performance de microsservicos\"}"
        CODE=$(post_json "/comment-ms/api/comments" "$BODY")
        if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
            ID=$(get_id)
            if [ -n "$ID" ]; then
                COMMENT_IDS+=("$ID")
                ((CREATED_COMMENTS++)) || true
            fi
        fi
        printf "\r  %d/%d" "$CREATED_COMMENTS" "$NUM_COMMENTS"
    done
    echo ""
    echo -e "${GREEN}$CREATED_COMMENTS comentarios criados${NC}"
    echo ""
fi

# ─────────────────────────────────────────────
# LIKES EM POSTS
# ─────────────────────────────────────────────
if [ ${#POST_IDS[@]} -gt 0 ]; then
    HALF_LIKES=$(( NUM_LIKES / 2 ))
    echo -e "${YELLOW}Criando $HALF_LIKES likes em posts...${NC}"
    CREATED_LIKES=0
    for i in $(seq 1 $HALF_LIKES); do
        U_IDX=$(( RANDOM % ${#USER_IDS[@]} ))
        P_IDX=$(( RANDOM % ${#POST_IDS[@]} ))
        UID="${USER_IDS[$U_IDX]}"
        PID="${POST_IDS[$P_IDX]}"
        BODY="{\"userId\":$UID,\"postId\":$PID,\"commentId\":null}"
        CODE=$(post_json "/like-ms/api/likes" "$BODY")
        [ "$CODE" = "201" ] || [ "$CODE" = "200" ] && ((CREATED_LIKES++)) || true
        printf "\r  %d/%d" "$CREATED_LIKES" "$HALF_LIKES"
    done
    echo ""
    echo -e "${GREEN}$CREATED_LIKES likes em posts criados${NC}"
    echo ""
fi

# ─────────────────────────────────────────────
# LIKES EM COMENTARIOS
# ─────────────────────────────────────────────
if [ ${#COMMENT_IDS[@]} -gt 0 ]; then
    HALF_LIKES=$(( NUM_LIKES / 2 ))
    echo -e "${YELLOW}Criando $HALF_LIKES likes em comentarios...${NC}"
    CREATED_COMMENT_LIKES=0
    for i in $(seq 1 $HALF_LIKES); do
        U_IDX=$(( RANDOM % ${#USER_IDS[@]} ))
        C_IDX=$(( RANDOM % ${#COMMENT_IDS[@]} ))
        UID="${USER_IDS[$U_IDX]}"
        CID="${COMMENT_IDS[$C_IDX]}"
        BODY="{\"userId\":$UID,\"postId\":null,\"commentId\":$CID}"
        CODE=$(post_json "/like-ms/api/likes" "$BODY")
        [ "$CODE" = "201" ] || [ "$CODE" = "200" ] && ((CREATED_COMMENT_LIKES++)) || true
        printf "\r  %d/%d" "$CREATED_COMMENT_LIKES" "$HALF_LIKES"
    done
    echo ""
    echo -e "${GREEN}$CREATED_COMMENT_LIKES likes em comentarios criados${NC}"
    echo ""
fi

# ─────────────────────────────────────────────
# AMIZADES
# ─────────────────────────────────────────────
if [ ${#USER_IDS[@]} -ge 2 ]; then
    echo -e "${YELLOW}Criando $NUM_FRIENDSHIPS amizades...${NC}"
    CREATED_FRIENDSHIPS=0
    ATTEMPTS=0
    while [ "$CREATED_FRIENDSHIPS" -lt "$NUM_FRIENDSHIPS" ] && [ "$ATTEMPTS" -lt $(( NUM_FRIENDSHIPS * 3 )) ]; do
        ((ATTEMPTS++)) || true
        U1_IDX=$(( RANDOM % ${#USER_IDS[@]} ))
        U2_IDX=$(( RANDOM % ${#USER_IDS[@]} ))
        UID1="${USER_IDS[$U1_IDX]}"
        UID2="${USER_IDS[$U2_IDX]}"
        [ "$UID1" = "$UID2" ] && continue
        BODY="{\"userId1\":$UID1,\"userId2\":$UID2,\"status\":\"ACCEPTED\"}"
        CODE=$(post_json "/friendship-ms/api/friendships" "$BODY")
        [ "$CODE" = "201" ] || [ "$CODE" = "200" ] && ((CREATED_FRIENDSHIPS++)) || true
        printf "\r  %d/%d" "$CREATED_FRIENDSHIPS" "$NUM_FRIENDSHIPS"
    done
    echo ""
    echo -e "${GREEN}$CREATED_FRIENDSHIPS amizades criadas${NC}"
    echo ""
fi

# ─────────────────────────────────────────────
# RESUMO
# ─────────────────────────────────────────────
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         SEED CONCLUIDO                                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Usuarios criados:    ${GREEN}$CREATED_USERS${NC}"
echo -e "  Posts criados:       ${GREEN}$CREATED_POSTS${NC}"
echo -e "  Comentarios criados: ${GREEN}${CREATED_COMMENTS:-0}${NC}"
echo -e "  Likes criados:       ${GREEN}$(( ${CREATED_LIKES:-0} + ${CREATED_COMMENT_LIKES:-0} ))${NC}"
echo -e "  Amizades criadas:    ${GREEN}${CREATED_FRIENDSHIPS:-0}${NC}"
echo ""
echo -e "${CYAN}Sistema pronto para os testes de carga!${NC}"
echo ""
