#!/bin/bash
set +e

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${RED}🛑 Stopping all services...${NC}"
docker compose down

echo -e "${GREEN}✅ All services stopped!${NC}"
echo ""
echo "💡 To remove volumes as well, run: docker compose down -v"
echo "💡 To clean everything, run: docker compose down -v --rmi all"