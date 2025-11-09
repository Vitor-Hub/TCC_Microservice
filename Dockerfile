FROM eclipse-temurin:17-jre-alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copiar o JAR da aplicação
COPY target/*.jar app.jar

# Expor a porta (ajuste conforme o serviço)
EXPOSE 8761

# Executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]