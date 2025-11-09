#!/bin/bash

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ -z "$1" ]; then
  echo -e "${YELLOW}Usage: ./logs.sh [service-name]${NC}"
  echo ""
  echo "Available services:"
  echo "  - mysql"
  echo "  - eureka"
  echo "  - gateway"
  echo "  - user"
  echo "  - post"
  echo "  - comment"
  echo "  - friendship"
  echo "  - like"
  echo "  - all (show all logs)"
  exit 1
fi

case $1 in
  mysql)
    docker logs -f tcc_mysql
    ;;
  eureka)
    docker logs -f microeureka
    ;;
  gateway)
    docker logs -f micro_api_gateway
    ;;
  user)
    docker logs -f micro_user_service
    ;;
  post)
    docker logs -f micro_post_service
    ;;
  comment)
    docker logs -f micro_comment_service
    ;;
  friendship)
    docker logs -f micro_friendship_service
    ;;
  like)
    docker logs -f micro_like_service
    ;;
  all)
    docker compose logs -f
    ;;
  *)
    echo -e "${YELLOW}Unknown service: $1${NC}"
    exit 1
    ;;
esac