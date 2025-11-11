#!/usr/bin/env python3
"""
📊 Gerador de Relatório Visual para TCC
Compara métricas entre Microsserviços e Monolítico
"""

import json
import sys
import os
from pathlib import Path
from datetime import datetime
import argparse

def load_k6_summary(filepath):
    """Carrega o arquivo summary JSON do K6"""
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except Exception as e:
        print(f"❌ Erro ao carregar {filepath}: {e}")
        return None

def extract_metrics(summary):
    """Extrai métricas importantes do summary do K6"""
    if not summary or 'metrics' not in summary:
        return None
    
    metrics = summary['metrics']
    
    result = {
        'http_reqs': {
            'count': metrics.get('http_reqs', {}).get('values', {}).get('count', 0),
            'rate': metrics.get('http_reqs', {}).get('values', {}).get('rate', 0),
        },
        'http_req_duration': {
            'avg': metrics.get('http_req_duration', {}).get('values', {}).get('avg', 0),
            'med': metrics.get('http_req_duration', {}).get('values', {}).get('med', 0),
            'p90': metrics.get('http_req_duration', {}).get('values', {}).get('p(90)', 0),
            'p95': metrics.get('http_req_duration', {}).get('values', {}).get('p(95)', 0),
            'p99': metrics.get('http_req_duration', {}).get('values', {}).get('p(99)', 0),
            'min': metrics.get('http_req_duration', {}).get('values', {}).get('min', 0),
            'max': metrics.get('http_req_duration', {}).get('values', {}).get('max', 0),
        },
        'http_req_failed': {
            'rate': metrics.get('http_req_failed', {}).get('values', {}).get('rate', 0) * 100,
        },
        'http_req_waiting': {
            'avg': metrics.get('http_req_waiting', {}).get('values', {}).get('avg', 0),
        },
        'iterations': {
            'count': metrics.get('iterations', {}).get('values', {}).get('count', 0),
            'rate': metrics.get('iterations', {}).get('values', {}).get('rate', 0),
        },
        'vus': {
            'max': metrics.get('vus', {}).get('values', {}).get('max', 0),
        },
    }
    
    return result

def generate_markdown_report(micro_metrics, mono_metrics, output_dir):
    """Gera relatório em Markdown"""
    
    report = f"""# 📊 Relatório Comparativo - TCC

**Data de Geração:** {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}

---

## 🎯 Resumo Executivo

Este relatório compara o desempenho entre as arquiteturas **Microsserviços** e **Monolítica** em um sistema de rede social.

---

## 📈 Métricas Gerais

### Total de Requisições

| Arquitetura | Total de Requisições | Taxa (req/s) |
|-------------|---------------------|--------------|
| **Microsserviços** | {micro_metrics['http_reqs']['count']:,} | {micro_metrics['http_reqs']['rate']:.2f} |
| **Monolítico** | {mono_metrics['http_reqs']['count']:,} | {mono_metrics['http_reqs']['rate']:.2f} |

**Diferença:** {((micro_metrics['http_reqs']['rate'] - mono_metrics['http_reqs']['rate']) / mono_metrics['http_reqs']['rate'] * 100):.2f}%

---

## ⏱️ Tempo de Resposta (ms)

### Latência

| Métrica | Microsserviços | Monolítico | Diferença |
|---------|----------------|------------|-----------|
| **Média** | {micro_metrics['http_req_duration']['avg']:.2f} ms | {mono_metrics['http_req_duration']['avg']:.2f} ms | {((micro_metrics['http_req_duration']['avg'] - mono_metrics['http_req_duration']['avg']) / mono_metrics['http_req_duration']['avg'] * 100):.2f}% |
| **Mediana (P50)** | {micro_metrics['http_req_duration']['med']:.2f} ms | {mono_metrics['http_req_duration']['med']:.2f} ms | {((micro_metrics['http_req_duration']['med'] - mono_metrics['http_req_duration']['med']) / mono_metrics['http_req_duration']['med'] * 100):.2f}% |
| **P90** | {micro_metrics['http_req_duration']['p90']:.2f} ms | {mono_metrics['http_req_duration']['p90']:.2f} ms | {((micro_metrics['http_req_duration']['p90'] - mono_metrics['http_req_duration']['p90']) / mono_metrics['http_req_duration']['p90'] * 100):.2f}% |
| **P95** | {micro_metrics['http_req_duration']['p95']:.2f} ms | {mono_metrics['http_req_duration']['p95']:.2f} ms | {((micro_metrics['http_req_duration']['p95'] - mono_metrics['http_req_duration']['p95']) / mono_metrics['http_req_duration']['p95'] * 100):.2f}% |
| **P99** | {micro_metrics['http_req_duration']['p99']:.2f} ms | {mono_metrics['http_req_duration']['p99']:.2f} ms | {((micro_metrics['http_req_duration']['p99'] - mono_metrics['http_req_duration']['p99']) / mono_metrics['http_req_duration']['p99'] * 100):.2f}% |
| **Máximo** | {micro_metrics['http_req_duration']['max']:.2f} ms | {mono_metrics['http_req_duration']['max']:.2f} ms | {((micro_metrics['http_req_duration']['max'] - mono_metrics['http_req_duration']['max']) / mono_metrics['http_req_duration']['max'] * 100):.2f}% |

---

## ✅ Confiabilidade

### Taxa de Erro

| Arquitetura | Taxa de Erro | Taxa de Sucesso |
|-------------|--------------|-----------------|
| **Microsserviços** | {micro_metrics['http_req_failed']['rate']:.2f}% | {100 - micro_metrics['http_req_failed']['rate']:.2f}% |
| **Monolítico** | {mono_metrics['http_req_failed']['rate']:.2f}% | {100 - mono_metrics['http_req_failed']['rate']:.2f}% |

---

## 🔄 Throughput e Escalabilidade

### Iterações de Usuários Virtuais

| Métrica | Microsserviços | Monolítico | Diferença |
|---------|----------------|------------|-----------|
| **Total de Iterações** | {micro_metrics['iterations']['count']:,} | {mono_metrics['iterations']['count']:,} | {((micro_metrics['iterations']['count'] - mono_metrics['iterations']['count']) / mono_metrics['iterations']['count'] * 100):.2f}% |
| **Taxa de Iterações** | {micro_metrics['iterations']['rate']:.2f}/s | {mono_metrics['iterations']['rate']:.2f}/s | {((micro_metrics['iterations']['rate'] - mono_metrics['iterations']['rate']) / mono_metrics['iterations']['rate'] * 100):.2f}% |
| **VUs Máximos** | {micro_metrics['vus']['max']} | {mono_metrics['vus']['max']} | - |

---

## 📊 Análise Comparativa

### Vantagens da Arquitetura de Microsserviços

- ✅ **Escalabilidade Independente:** Cada serviço pode escalar individualmente
- ✅ **Isolamento de Falhas:** Problemas em um serviço não afetam os outros
- ✅ **Tecnologias Heterogêneas:** Liberdade para usar diferentes tecnologias
- ✅ **Deploy Independente:** Atualizações sem downtime completo

### Desvantagens da Arquitetura de Microsserviços

- ❌ **Complexidade Operacional:** Maior overhead de infraestrutura
- ❌ **Latência de Rede:** Comunicação entre serviços adiciona latência
- ❌ **Transações Distribuídas:** Mais complexo garantir consistência
- ❌ **Debugging:** Rastreamento de erros através de múltiplos serviços

### Vantagens da Arquitetura Monolítica

- ✅ **Simplicidade:** Menor complexidade operacional
- ✅ **Menor Latência:** Comunicação interna mais rápida
- ✅ **Transações ACID:** Mais fácil garantir consistência
- ✅ **Debugging:** Mais simples rastrear problemas

### Desvantagens da Arquitetura Monolítica

- ❌ **Escalabilidade:** Toda aplicação precisa escalar junta
- ❌ **Deploy:** Qualquer mudança requer deploy completo
- ❌ **Acoplamento:** Maior risco de dependências entre módulos
- ❌ **Tamanho:** Aplicação pode crescer muito e ficar difícil de manter

---

## 🎓 Conclusões

{"Microsserviços apresentou melhor performance" if micro_metrics['http_req_duration']['avg'] < mono_metrics['http_req_duration']['avg'] else "Monolítico apresentou melhor performance"} em termos de latência média.

A arquitetura de microsserviços é mais adequada para:
- Aplicações grandes com múltiplos times
- Necessidade de escalabilidade granular
- Requisitos de alta disponibilidade
- Evolução tecnológica contínua

A arquitetura monolítica é mais adequada para:
- Aplicações menores ou MVPs
- Times pequenos
- Requisitos de baixa latência crítica
- Menor complexidade operacional aceitável

---

## 📁 Arquivos de Dados

- Microsserviços: Ver arquivos na pasta `test-results/microservices_*`
- Monolítico: Ver arquivos na pasta `test-results/monolithic_*`

---

**Gerado automaticamente pelo script de análise do TCC**
"""
    
    output_file = Path(output_dir) / f"relatorio_comparativo_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    return output_file

def generate_ascii_chart(data, title, max_width=50):
    """Gera um gráfico ASCII simples"""
    if not data:
        return "Sem dados para exibir"
    
    max_value = max(data.values())
    chart = [f"\n{title}\n{'=' * (max_width + 20)}\n"]
    
    for label, value in data.items():
        bar_length = int((value / max_value) * max_width) if max_value > 0 else 0
        bar = '█' * bar_length
        chart.append(f"{label:20} {bar} {value:.2f}\n")
    
    return ''.join(chart)

def main():
    parser = argparse.ArgumentParser(description='Gera relatório comparativo de testes de carga')
    parser.add_argument('results_dir', help='Diretório com os resultados dos testes')
    parser.add_argument('--micro', help='Arquivo summary JSON dos microsserviços', default=None)
    parser.add_argument('--mono', help='Arquivo summary JSON do monolítico', default=None)
    
    args = parser.parse_args()
    
    results_dir = Path(args.results_dir)
    
    if not results_dir.exists():
        print(f"❌ Diretório não encontrado: {results_dir}")
        sys.exit(1)
    
    # Procura arquivos automaticamente se não especificado
    if not args.micro:
        micro_files = list(results_dir.glob('microservices_*_summary.json'))
        if micro_files:
            args.micro = str(sorted(micro_files)[-1])  # Pega o mais recente
            print(f"📊 Arquivo de microsserviços: {args.micro}")
    
    if not args.mono:
        mono_files = list(results_dir.glob('monolithic_*_summary.json'))
        if mono_files:
            args.mono = str(sorted(mono_files)[-1])  # Pega o mais recente
            print(f"📊 Arquivo de monolítico: {args.mono}")
    
    # Valida arquivos
    if not args.micro or not Path(args.micro).exists():
        print("❌ Arquivo de microsserviços não encontrado!")
        print("Execute primeiro: ./run-load-test.sh microservices")
        sys.exit(1)
    
    if not args.mono or not Path(args.mono).exists():
        print("⚠️  Arquivo de monolítico não encontrado!")
        print("Você pode executar: ./run-load-test.sh monolithic")
        print("Por enquanto, gerando relatório apenas de microsserviços...\n")
        
        # Carrega apenas microsserviços
        micro_summary = load_k6_summary(args.micro)
        if not micro_summary:
            sys.exit(1)
        
        micro_metrics = extract_metrics(micro_summary)
        
        # Exibe métricas
        print("\n📊 MÉTRICAS - MICROSSERVIÇOS")
        print("=" * 60)
        print(f"Total de Requisições: {micro_metrics['http_reqs']['count']:,}")
        print(f"Taxa: {micro_metrics['http_reqs']['rate']:.2f} req/s")
        print(f"\nLatência Média: {micro_metrics['http_req_duration']['avg']:.2f} ms")
        print(f"Latência P95: {micro_metrics['http_req_duration']['p95']:.2f} ms")
        print(f"Latência P99: {micro_metrics['http_req_duration']['p99']:.2f} ms")
        print(f"\nTaxa de Erro: {micro_metrics['http_req_failed']['rate']:.2f}%")
        print(f"Taxa de Sucesso: {100 - micro_metrics['http_req_failed']['rate']:.2f}%")
        
        # Gráfico ASCII
        latency_data = {
            'Média': micro_metrics['http_req_duration']['avg'],
            'P90': micro_metrics['http_req_duration']['p90'],
            'P95': micro_metrics['http_req_duration']['p95'],
            'P99': micro_metrics['http_req_duration']['p99'],
        }
        print(generate_ascii_chart(latency_data, "Latências (ms)"))
        
        sys.exit(0)
    
    # Carrega ambos os arquivos
    print("\n🔄 Carregando dados...")
    micro_summary = load_k6_summary(args.micro)
    mono_summary = load_k6_summary(args.mono)
    
    if not micro_summary or not mono_summary:
        print("❌ Erro ao carregar os arquivos!")
        sys.exit(1)
    
    # Extrai métricas
    micro_metrics = extract_metrics(micro_summary)
    mono_metrics = extract_metrics(mono_summary)
    
    if not micro_metrics or not mono_metrics:
        print("❌ Erro ao extrair métricas!")
        sys.exit(1)
    
    # Gera relatório
    print("\n📝 Gerando relatório comparativo...")
    report_file = generate_markdown_report(micro_metrics, mono_metrics, results_dir)
    
    print(f"\n✅ Relatório gerado: {report_file}")
    
    # Exibe resumo no terminal
    print("\n" + "=" * 60)
    print("📊 RESUMO COMPARATIVO")
    print("=" * 60)
    
    print("\n🚀 THROUGHPUT")
    print(f"  Microsserviços: {micro_metrics['http_reqs']['rate']:.2f} req/s")
    print(f"  Monolítico:     {mono_metrics['http_reqs']['rate']:.2f} req/s")
    diff_throughput = ((micro_metrics['http_reqs']['rate'] - mono_metrics['http_reqs']['rate']) / mono_metrics['http_reqs']['rate'] * 100)
    print(f"  Diferença:      {diff_throughput:+.2f}%")
    
    print("\n⏱️  LATÊNCIA MÉDIA")
    print(f"  Microsserviços: {micro_metrics['http_req_duration']['avg']:.2f} ms")
    print(f"  Monolítico:     {mono_metrics['http_req_duration']['avg']:.2f} ms")
    diff_latency = ((micro_metrics['http_req_duration']['avg'] - mono_metrics['http_req_duration']['avg']) / mono_metrics['http_req_duration']['avg'] * 100)
    print(f"  Diferença:      {diff_latency:+.2f}%")
    
    print("\n✅ TAXA DE SUCESSO")
    print(f"  Microsserviços: {100 - micro_metrics['http_req_failed']['rate']:.2f}%")
    print(f"  Monolítico:     {100 - mono_metrics['http_req_failed']['rate']:.2f}%")
    
    # Determina vencedor
    print("\n🏆 VENCEDOR")
    if micro_metrics['http_req_duration']['avg'] < mono_metrics['http_req_duration']['avg']:
        print("  ✨ Microsserviços apresentou menor latência!")
    else:
        print("  ✨ Monolítico apresentou menor latência!")
    
    print("\n" + "=" * 60)
    print(f"\n📄 Relatório completo: {report_file}")
    print("\n🎓 Boa sorte com seu TCC!\n")

if __name__ == '__main__':
    main()