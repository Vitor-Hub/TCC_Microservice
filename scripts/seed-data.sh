#!/bin/bash

# Seed Test Data
# Creates initial data for load testing

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

BASE_URL="http://localhost:18765"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🌱 SEEDING TEST DATA                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if gateway is up
echo -e "${YELLOW}Checking gateway...${NC}"
if ! curl -f -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Gateway not responding${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Gateway is up${NC}"
echo ""

# Create 100 users
echo -e "${YELLOW}Creating 100 users...${NC}"
CREATED_USERS=0
for i in {1..100}; do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/users" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"User $i\",
            \"email\": \"user$i@test.com\",
            \"password\": \"pass123\"
        }" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
        ((CREATED_USERS++))
        printf "\r  Progress: %d/100" $CREATED_USERS
    fi
done
echo ""
echo -e "${GREEN}✓ Created $CREATED_USERS users${NC}"
echo ""

# Create 100 posts
echo -e "${YELLOW}Creating 100 posts...${NC}"
CREATED_POSTS=0
for i in {1..100}; do
    USER_ID=$((1 + RANDOM % CREATED_USERS))
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/posts" \
        -H "Content-Type: application/json" \
        -d "{
            \"user\": {\"id\": $USER_ID},
            \"content\": \"This is test post number $i from user $USER_ID\"
        }" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
        ((CREATED_POSTS++))
        printf "\r  Progress: %d/100" $CREATED_POSTS
    fi
done
echo ""
echo -e "${GREEN}✓ Created $CREATED_POSTS posts${NC}"
echo ""

# Create some comments
echo -e "${YELLOW}Creating 50 comments...${NC}"
CREATED_COMMENTS=0
for i in {1..50}; do
    USER_ID=$((1 + RANDOM % CREATED_USERS))
    POST_ID=$((1 + RANDOM % CREATED_POSTS))
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/comments" \
        -H "Content-Type: application/json" \
        -d "{
            \"userId\": $USER_ID,
            \"postId\": $POST_ID,
            \"content\": \"Comment $i on post $POST_ID\"
        }" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
        ((CREATED_COMMENTS++))
        printf "\r  Progress: %d/50" $CREATED_COMMENTS
    fi
done
echo ""
echo -e "${GREEN}✓ Created $CREATED_COMMENTS comments${NC}"
echo ""

# Summary
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  ✓ SEED DATA COMPLETE                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Created:"
echo "  • Users: $CREATED_USERS"
echo "  • Posts: $CREATED_POSTS"
echo "  • Comments: $CREATED_COMMENTS"
echo ""
echo "System is ready for load testing!"
echo ""