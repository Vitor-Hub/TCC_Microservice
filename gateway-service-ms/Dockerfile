# Use a imagem base do Java 17
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia o JAR gerado
COPY target/*.jar app.jar

# Expõe a porta
EXPOSE 8765

# Comando de inicialização
ENTRYPOINT ["java", "-jar", "app.jar"]
