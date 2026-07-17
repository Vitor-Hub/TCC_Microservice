# Unified runtime across both architectures: same base image and JVM version
# as the monolith, so any measured difference is architectural, not runtime.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 18082

# Heap settings come from docker-compose via JAVA_OPTS (resource envelope).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
