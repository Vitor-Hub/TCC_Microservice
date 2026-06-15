# 🚀 TCC Microservices - Quick Start Guide

This project implements a microservices-based social media system for academic comparison with monolithic architecture.

## 📋 Prerequisites

- **Docker** & **Docker Compose** (required)
- **Java 17+** (required for building)
- **Maven 3.6+** (required for building)
- **K6** (optional, for load testing)
- **Python 3** (optional, for reports)

## 🎯 Quick Start (3 Steps)

### 1. Make script executable
```bash
cd scripts
chmod +x manage.sh
```

### 2. Run management console
```bash
./manage.sh
```

### 3. Select option
```
1) Build All Services      # First time
2) Deploy System          # After build
3) Fresh Start            # Clean + Build + Deploy (recommended for first time)
```

## 📊 Async Validation

The system uses async operations for better performance. To validate:

### Option 1: Use Management Console
```bash
./manage.sh
# Select: 4) Validate Async Configuration
```

### Option 2: Manual Validation

**Check logs for async threads:**
```bash
# Should see threads named: LikeAsync-1, PostAsync-2, etc
docker logs micro-like-ms | grep "Async-"
```

**Check thread pool metrics:**
```bash
# Should return pool size
curl http://localhost:18084/actuator/metrics/executor.pool.size
```

**Test response time:**
```bash
# Async (parallel): <2s
# Sync (sequential): >5s
time curl -X POST http://localhost:18765/api/likes \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"postId":1}'
```

## 🏥 Health Check

```bash
./manage.sh
# Select: 5) Check System Health
```

Or manually:
```bash
curl http://localhost:18765/actuator/health
curl http://localhost:18081/actuator/health  # User
curl http://localhost:18082/actuator/health  # Post
curl http://localhost:18083/actuator/health  # Comment
curl http://localhost:18084/actuator/health  # Like
curl http://localhost:18085/actuator/health  # Friendship
```

## 📈 Monitoring Dashboards

- **Grafana**: http://localhost:3000 (admin/admin)
- **Eureka**: http://localhost:8761
- **Prometheus**: http://localhost:9090

## 🧪 Load Testing

```bash
./manage.sh
# Select: 7) Run Load Test
```

Expected results with async optimization:
- **Response time P95**: ~2-5s (vs 30s without async)
- **Error rate**: <5% (vs 16% without async)
- **Throughput**: 15K+ req/s (vs 6K without async)

## 📁 Project Structure

```
.
├── scripts/
│   └── manage.sh              # Main management console
├── user-ms/                   # User microservice (no async needed)
├── post-ms/                   # Post microservice (async enabled)
├── comment-ms/                # Comment microservice (async enabled)
├── like-ms/                   # Like microservice (async enabled)
├── friendship-ms/             # Friendship microservice (async enabled)
├── gateway-service-ms/        # API Gateway
├── eureka-server-ms/          # Service discovery
└── docker-compose*.yml        # Docker configurations
```

## 🔍 Verifying Async is Working

### Signs async is WORKING ✅
1. **Thread names in logs**: `LikeAsync-1`, `PostAsync-2`, etc
2. **Response time**: <2s for like creation
3. **Parallel logs**: Multiple "START - Fetching" logs with same timestamp
4. **Thread pool metrics**: Pool size > 0

### Signs async is NOT working ❌
1. **Sequential logs**: One "START" followed by one "SUCCESS" before next "START"
2. **Response time**: >5s for like creation
3. **No async threads**: Only `http-nio` threads in logs
4. **Missing AsyncConfig**: No "AsyncConfig initialized" in logs

## 🐛 Troubleshooting

### Services won't start
```bash
./manage.sh
# Select: 10) Clean Everything
# Then: 3) Fresh Start
```

### Async not working
Check application.yml has:
```yaml
spring:
  cache:
    type: caffeine
```

Check main class has:
```java
@EnableAsync  // For services that call others
```

### Build fails
```bash
# Check Java version
java -version  # Should be 17+

# Clean and rebuild
cd <service-directory>
mvn clean package -DskipTests
```

## 📝 For TCC Documentation

Key metrics to collect:
1. **Response time** (P50, P95, P99)
2. **Throughput** (requests/second)
3. **Error rate** (%)
4. **Resource usage** (CPU, Memory)
5. **Cache hit rate** (%)
6. **Thread pool utilization** (%)

All metrics available in Grafana dashboards.

## 🎓 Academic Comparison Points

### Microservices (This Project)
- ✅ Scalability (independent scaling)
- ✅ Resilience (circuit breakers)
- ✅ Technology diversity (different stacks per service)
- ❌ Complexity (distributed tracing, async coordination)
- ❌ Latency (network calls between services)

### Performance Optimizations Applied
1. **Async/Parallel Processing**: CompletableFuture for non-blocking calls
2. **Caching**: Caffeine cache with TTL
3. **Connection Pooling**: HikariCP optimized
4. **Thread Pooling**: Custom executors per service
5. **Circuit Breakers**: Resilience4j patterns

## 💡 Tips

- Always run **Fresh Start** when changing configurations
- Use **Validate Async** after code changes
- Monitor **Grafana** during load tests
- Check **logs** if something doesn't work

## 📚 Additional Resources

- Spring Boot Async: https://spring.io/guides/gs/async-method/
- Caffeine Cache: https://github.com/ben-manes/caffeine
- K6 Load Testing: https://k6.io/docs/
- Resilience4j: https://resilience4j.readme.io/

---

**Need help?** Check the management console menu or logs in `/tmp/build_*.log`