#!/bin/bash

PROM_URL="http://localhost:9090"
DURATION=900  # 15 minutos
INTERVAL=5
SNAPSHOTS=$((DURATION / INTERVAL))
OUTPUT_DIR="scripts/metrics-snapshots"

mkdir -p "$OUTPUT_DIR"

echo "🚀 Coletando métricas por ${DURATION}s..."

for i in $(seq 1 $SNAPSHOTS); do
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)

    echo "[$(date +%H:%M:%S)] Snapshot $i de $SNAPSHOTS"

    # Throughput (requisições por segundo)
    curl -s "${PROM_URL}/api/v1/query?query=sum(rate(http_requests_total[1m]))" | \
        jq -r '.data.result[0].value[1] // "0"' > "${OUTPUT_DIR}/throughput_${TIMESTAMP}.json"

    # CPU médio (%)
    curl -s "${PROM_URL}/api/v1/query?query=avg(rate(process_cpu_seconds_total[1m])*100)" | \
        jq -r '.data.result[0].value[1] // "0"' > "${OUTPUT_DIR}/cpu_${TIMESTAMP}.json"

    # Memória média (MB)
    curl -s "${PROM_URL}/api/v1/query?query=avg(process_resident_memory_bytes/1024/1024)" | \
        jq -r '.data.result[0].value[1] // "0"' > "${OUTPUT_DIR}/memory_${TIMESTAMP}.json"

    # Latência P95 (ms)
    curl -s "${PROM_URL}/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_request_duration_seconds_bucket[1m]))by(le))*1000" | \
        jq -r '.data.result[0].value[1] // "0"' > "${OUTPUT_DIR}/latency_p95_${TIMESTAMP}.json"

    sleep $INTERVAL
done

echo "✅ Coleta finalizada! Arquivos em: $OUTPUT_DIR"