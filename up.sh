#!/bin/bash

# Database Optimization Script
# Applies indexes and optimizations to Post Service database

set -e

echo "======================================"
echo "📊 Post Service DB Optimization"
echo "======================================"
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Check if container is running
echo -e "${YELLOW}🔍 Checking if post_ms_db container is running...${NC}"

if ! docker ps | grep -q "post_ms_db"; then
    echo -e "${RED}❌ Error: post_ms_db container is not running${NC}"
    echo "Please start the services first:"
    echo "  ./deploy-complete-v2.sh"
    exit 1
fi

echo -e "${GREEN}✅ Container is running${NC}"
echo ""

# Create SQL script inside container
echo -e "${YELLOW}📝 Creating optimization script...${NC}"

docker exec -i post_ms_db bash -c 'cat > /tmp/optimize.sql' << 'EOF'
-- Create indexes
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_user_created ON posts(user_id, created_at DESC);

-- Analyze tables
ANALYZE posts;

-- Show indexes
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'posts'
ORDER BY indexname;
EOF

echo -e "${GREEN}✅ Script created${NC}"
echo ""

# Execute optimization
echo -e "${YELLOW}⚙️ Applying optimizations...${NC}"
echo ""

docker exec -i post_ms_db psql -U post -d postdb -f /tmp/optimize.sql

echo ""
echo -e "${GREEN}✅ Optimization applied successfully!${NC}"
echo ""

# Test query performance
echo -e "${YELLOW}🧪 Testing query performance...${NC}"
echo ""

docker exec -i post_ms_db psql -U post -d postdb << 'EOF'
-- Show table stats
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE tablename = 'posts';

-- Test query with EXPLAIN
EXPLAIN ANALYZE 
SELECT * FROM posts 
WHERE user_id = 1 
ORDER BY created_at DESC 
LIMIT 10;
EOF

echo ""
echo "======================================"
echo -e "${GREEN}✅ Database Optimization Complete!${NC}"
echo "======================================"
echo ""
echo "📊 What was optimized:"
echo "  ✅ Added index on user_id"
echo "  ✅ Added index on created_at"
echo "  ✅ Added composite index on (user_id, created_at)"
echo "  ✅ Updated table statistics"
echo ""
echo "🚀 Next steps:"
echo "  1. Restart post-ms service:"
echo "     docker-compose restart post-ms"
echo ""
echo "  2. Run load tests again:"
echo "     cd scripts && ./run-load-test.sh"
echo ""
echo "  3. Compare results in Grafana"
echo ""