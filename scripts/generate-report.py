import json
import glob
from pathlib import Path

def main():
    print("\n" + "="*60)
    print("📊 RELATÓRIO DE PERFORMANCE - MICROSSERVIÇOS")
    print("="*60)

    # Carregar apenas dados do K6
    k6_files = glob.glob('testes_carga/results/microservices-*.json')

    if not k6_files:
        print("❌ Nenhum resultado K6 encontrado!")
        return

    latest_k6 = max(k6_files, key=lambda x: Path(x).stat().st_mtime)

    with open(latest_k6, 'r') as f:
        k6_data = json.load(f)

    # Extrair métricas
    metrics = k6_data['metrics']

    total_reqs = metrics['http_reqs']['values']['count']
    error_rate = metrics['errors']['values']['rate'] * 100
    fail_rate = metrics['http_req_failed']['values']['rate'] * 100

    duration = metrics['http_req_duration']['values']
    throughput = metrics['http_reqs']['values']['rate']

    iterations = metrics['iterations']['values']['count']
    iteration_duration = metrics['iteration_duration']['values']

    # VUs
    vus_max = metrics['vus_max']['values']['max']
    vus_avg = metrics['vus']['values']['value']

    # Data transfer
    data_received = metrics['data_received']['values']['count'] / (1024 * 1024)  # MB
    data_sent = metrics['data_sent']['values']['count'] / 1024  # KB

    # Relatório
    print(f"\n📈 CARGA")
    print(f"  VUs Máximo: {vus_max:.0f}")
    print(f"  VUs Médio: {vus_avg:.0f}")
    print(f"  Iterações Completadas: {iterations:.0f}")

    print(f"\n🔥 THROUGHPUT")
    print(f"  Total de Requisições: {total_reqs:.0f}")
    print(f"  Taxa: {throughput:.2f} req/s")
    print(f"  Dados Recebidos: {data_received:.2f} MB")
    print(f"  Dados Enviados: {data_sent:.2f} KB")

    print(f"\n⚡ LATÊNCIA")
    print(f"  Média: {duration['avg']:.2f} ms")
    print(f"  Mínima: {duration['min']:.2f} ms")
    print(f"  Máxima: {duration['max']:.2f} ms")
    print(f"  P50 (Mediana): {duration['med']:.2f} ms")
    print(f"  P90: {duration['p(90)']:.2f} ms")
    print(f"  P95: {duration['p(95)']:.2f} ms")

    p99 = duration.get('p(99)', 0)
    if p99:
        print(f"  P99: {p99:.2f} ms")

    print(f"\n❌ ERROS")
    print(f"  Taxa de Erro (custom): {error_rate:.2f}%")
    print(f"  Taxa de Falha HTTP: {fail_rate:.2f}%")

    print(f"\n⏱️  DURAÇÃO ITERAÇÃO")
    print(f"  Média: {iteration_duration['avg']/1000:.2f}s")
    print(f"  P95: {iteration_duration['p(95)']/1000:.2f}s")

    print("="*60 + "\n")

    # Salvar relatório JSON
    report = {
        "timestamp": k6_data.get('state', {}).get('testRunDurationMs', 0) / 1000,
        "architecture": "microservices",
        "load": {
            "max_vus": vus_max,
            "avg_vus": vus_avg,
            "iterations": iterations
        },
        "throughput": {
            "total_requests": total_reqs,
            "rate_per_second": round(throughput, 2),
            "data_received_mb": round(data_received, 2),
            "data_sent_kb": round(data_sent, 2)
        },
        "latency": {
            "avg_ms": round(duration['avg'], 2),
            "min_ms": round(duration['min'], 2),
            "max_ms": round(duration['max'], 2),
            "p50_ms": round(duration['med'], 2),
            "p90_ms": round(duration['p(90)'], 2),
            "p95_ms": round(duration['p(95)'], 2),
            "p99_ms": round(p99, 2) if p99 else None
        },
        "errors": {
            "custom_error_rate_percent": round(error_rate, 2),
            "http_failure_rate_percent": round(fail_rate, 2)
        },
        "iteration_duration": {
            "avg_seconds": round(iteration_duration['avg']/1000, 2),
            "p95_seconds": round(iteration_duration['p(95)']/1000, 2)
        }
    }

    output_file = 'tcc-report-microservices.json'
    with open(output_file, 'w') as f:
        json.dump(report, f, indent=2)

    print(f"✅ Relatório salvo em: {output_file}\n")

if __name__ == '__main__':
    main()