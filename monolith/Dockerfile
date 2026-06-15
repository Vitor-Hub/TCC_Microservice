FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/monolith-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", \
    "-Xmx768m", \
    "-Xms384m", \
    "-XX:+UseG1GC", \
    "app.jar"]
