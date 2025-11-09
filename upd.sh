#!/bin/bash
# ===========================================================
# 🧠 convert-properties-to-yml-macos.sh
# Compatível com o Bash 3.x do macOS
# ===========================================================

echo "🔄 Iniciando conversão de application.properties para application.yml..."

# Cada linha: nome_da_pasta porta db_host db_name db_user db_pass
services=(
  "user-ms 18081 user-ms-db userdb user user123"
  "post-ms 18082 post-ms-db postdb post post123"
  "comment-ms 18083 comment-ms-db commentdb comment comment123"
  "like-ms 18085 like-ms-db likedb like like123"
  "friendship-ms 18084 friendship-ms-db friendshipdb friendship friendship123"
)

for entry in "${services[@]}"; do
  set -- $entry
  svc=$1
  port=$2
  db_host=$3
  db_name=$4
  db_user=$5
  db_pass=$6

  path="./$svc/src/main/resources"

  if [ ! -d "$path" ]; then
    echo "⚠️  Diretório não encontrado: $path"
    continue
  fi

  echo "🛠️  Convertendo $svc ..."

  cat > "$path/application.yml" <<EOF
spring:
  application:
    name: ${svc}
  datasource:
    url: jdbc:postgresql://${db_host}:5432/${db_name}
    username: ${db_user}
    password: ${db_pass}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: ${port}

eureka:
  client:
    service-url:
      defaultZone: http://microeureka:8761/eureka/
    healthcheck:
      enabled: true
  instance:
    prefer-ip-address: false
    hostname: micro_${svc/_/-}_service
    instance-id: \${spring.application.name}:\${spring.application.instance-id:\${random.value}}
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

feign:
  circuitbreaker:
    enabled: true
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 2000
            readTimeout: 4000
    loadbalancer:
      enabled: true

resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 5
        waitDurationInOpenState: 10s
  retry:
    instances:
      default:
        maxAttempts: 2
        waitDuration: 200ms
EOF

  rm -f "$path/application.properties"
  echo "✅ $svc convertido com sucesso!"
done

echo "-------------------------------------------------------------"
echo "🎉 Conversão concluída! Todos os application.yml foram gerados."
echo "🗂️  Verifique em: <microserviço>/src/main/resources/application.yml"
