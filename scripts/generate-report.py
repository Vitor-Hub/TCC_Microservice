#!/usr/bin/env python3
"""
📊 Gerador de Relatório Visual para TCC - VERSÃO APRIMORADA
Compara métricas entre Microsserviços e Monolítico com análises estatísticas detalhadas
"""

import json
import sys
import os
from pathlib import Path
from datetime import datetime
import argparse
from typing import Dict, Any, Optional, List

def load_k6_summary(filepath: str) -> Optional[Dict[str, Any]]:
    """Carrega o arquivo summary JSON do K6"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"❌ Erro: Arquivo não encontrado: {filepath}")
        return None
    except json.JSONDecodeError as e:
        print(f"❌ Erro ao decodificar JSON: {e}")
        return None
    except Exception as e:
        print(f"❌ Erro ao carregar {filepath}: {e}")
        return None

def extract_metrics(summary: Dict[str, Any]) -> Optional[Dict[str, Any]]:
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
        'http_req_blocked': {
            'avg': metrics.get('http_req_blocked', {}).get('values', {}).get('avg', 0),
        },
        'http_req_connecting': {
            'avg': metrics.get('http_req_connecting', {}).get('values', {}).get('avg', 0),
        },
        'iterations': {
            'count': metrics.get('iterations', {}).get('values', {}).get('count', 0),
            'rate': metrics.get('iterations', {}).get('values', {}).get('rate', 0),
        },
        'vus': {
            'max': metrics.get('vus', {}).get('values', {}).get('max', 0),
            'min': metrics.get('vus', {}).get('values', {}).get('min', 0),
        },
        'data_received': {
            'count': metrics.get('data_received', {}).get('values', {}).get('count', 0),
            'rate': metrics.get('data_received', {}).get('values', {}).get('rate', 0),
        },
        'data_sent': {
            'count': metrics.get('data_sent', {}).get('values', {}).get('count', 0),
            'rate': metrics.get('data_sent', {}).get('values', {}).get('rate', 0),
        },
    }
    
    # Métricas customizadas (se existirem)
    custom_metrics = [
        'user_creation_duration', 'post_creation_duration', 
        'comment_creation_duration', 'like_creation_duration',
        'friendship_creation_duration', 'feed_load_duration',
        'get_operation_duration'
    ]
    
    for metric_name in custom_metrics:
        if metric_name in metrics:
            result[metric_name] = {
                'avg': metrics[metric_name].get('values', {}).get('avg', 0),
                'p95': metrics[metric_name].get('values', {}).get('p(95)', 0),
                'p99': metrics[metric_name].get('values', {}).get('p(99)', 0),
            }
    
    return result

def calculate_percentage_diff(val1: float, val2: float) -> float:
    """Calcula diferença percentual entre dois valores"""
    if val2 == 0:
        return 0.0
    return ((val1 - val2) / val2) * 100

def format_bytes(bytes_value: float) -> str:
    """Formata bytes em unidades legíveis"""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_value < 1024.0:
            return f"{bytes_value:.2f} {unit}"
        bytes_value /= 1024.0
    return f"{bytes_value:.2f} TB"

def generate_ascii_chart(data: Dict[str, float], title: str, max_width: int = 50) -> str:
    """Gera um gráfico ASCII simples"""
    if not data:
        return "Sem dados para exibir"
    
    max_value = max(data.values()) if data.values() else 1
    chart = [f"\n{title}\n{'═' * (max_width + 20)}\n"]
    
    for label, value in data.items():
        bar_length = int((value / max_value) * max_width) if max_value > 0 else 0
        bar = '█' * bar_length
        chart.append(f"{label:20} {bar} {value:.2f}\n")
    
    return ''.join(chart)

def generate_comparison_table(label: str, micro_val: float, mono_val: float, 
                             unit: str = "", lower_is_better: bool = True) -> str:
    """Gera uma linha de comparação formatada"""
    diff = calculate_percentage_diff(micro_val, mono_val)
    
    if lower_is_better:
        winner = "✅ Micro" if micro_val < mono_val else "✅ Mono"
    else:
        winner = "✅ Micro" if micro_val > mono_val else "✅ Mono"
    
    return f"| {label:25} | {micro_val:.2f}{unit:5} | {mono_val:.2f}{unit:5} | {diff:+.2f}% | {winner} |\n"

def generate_markdown_report_single(metrics: Dict[str, Any], arch_name: str, output_dir: Path) -> Path:
    """Gera relatório Markdown apenas para uma arquitetura"""
    
    report = f"""# 📊 Relatório de Desempenho - {arch_name}

**Data de Geração:** {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}

---

## 🎯 Resumo Executivo

Este relatório apresenta métricas de desempenho da arquitetura **{arch_name}** em um sistema de rede social simulada.

---

## 📈 Métricas Gerais

### Total de Requisições

- **Total:** {metrics['http_reqs']['count']:,} requisições
- **Taxa:** {metrics['http_reqs']['rate']:.2f} req/s
- **Iterações de VUs:** {metrics['iterations']['count']:,}
- **Taxa de Iterações:** {metrics['iterations']['rate']:.2f} iter/s
- **VUs Máximos:** {metrics['vus']['max']}

---

## ⏱️ Latência de Requisições

| Métrica | Valor (ms) |
|---------|------------|
| **Média** | {metrics['http_req_duration']['avg']:.2f} |
| **Mediana (P50)** | {metrics['http_req_duration']['med']:.2f} |
| **P90** | {metrics['http_req_duration']['p90']:.2f} |
| **P95** | {metrics['http_req_duration']['p95']:.2f} |
| **P99** | {metrics['http_req_duration']['p99']:.2f} |
| **Mínimo** | {metrics['http_req_duration']['min']:.2f} |
| **Máximo** | {metrics['http_req_duration']['max']:.2f} |

---

## ✅ Confiabilidade

- **Taxa de Sucesso:** {100 - metrics['http_req_failed']['rate']:.2f}%
- **Taxa de Erro:** {metrics['http_req_failed']['rate']:.2f}%

---

## 🌐 Transferência de Dados

- **Dados Recebidos:** {format_bytes(metrics['data_received']['count'])} ({format_bytes(metrics['data_received']['rate'])}/s)
- **Dados Enviados:** {format_bytes(metrics['data_sent']['count'])} ({format_bytes(metrics['data_sent']['rate'])}/s)

---

## 📊 Métricas por Operação
"""

    # Adiciona métricas customizadas se existirem
    custom_ops = {
        'user_creation_duration': 'Criação de Usuário',
        'post_creation_duration': 'Criação de Post',
        'comment_creation_duration': 'Criação de Comentário',
        'like_creation_duration': 'Criação de Like',
        'friendship_creation_duration': 'Criação de Amizade',
        'feed_load_duration': 'Carregamento de Feed',
        'get_operation_duration': 'Operações GET',
    }
    
    has_custom = False
    for key, label in custom_ops.items():
        if key in metrics:
            if not has_custom:
                report += "\n### Duração por Tipo de Operação\n\n"
                report += "| Operação | Média (ms) | P95 (ms) | P99 (ms) |\n"
                report += "|----------|------------|----------|----------|\n"
                has_custom = True
            
            m = metrics[key]
            report += f"| {label} | {m['avg']:.2f} | {m['p95']:.2f} | {m['p99']:.2f} |\n"
    
    report += f"""
---

## 📉 Componentes de Latência

| Componente | Tempo Médio (ms) |
|------------|------------------|
| **Bloqueio (Blocked)** | {metrics.get('http_req_blocked', {}).get('avg', 0):.2f} |
| **Conexão (Connecting)** | {metrics.get('http_req_connecting', {}).get('avg', 0):.2f} |
| **Espera (Waiting)** | {metrics['http_req_waiting']['avg']:.2f} |

---

## 📝 Observações

### Pontos Fortes
- Taxa de sucesso de {100 - metrics['http_req_failed']['rate']:.2f}% indica alta confiabilidade
- Throughput de {metrics['http_reqs']['rate']:.2f} req/s demonstra boa capacidade

### Áreas de Atenção
- P99 em {metrics['http_req_duration']['p99']:.2f}ms pode indicar outliers ocasionais
- Latência máxima de {metrics['http_req_duration']['max']:.2f}ms requer investigação

---

**Gerado automaticamente pelo script de análise do TCC**
"""
    
    output_file = output_dir / f"relatorio_{arch_name.lower().replace(' ', '_')}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    return output_file

def generate_markdown_report_comparison(micro_metrics: Dict[str, Any], mono_metrics: Dict[str, Any], 
                                       output_dir: Path) -> Path:
    """Gera relatório comparativo em Markdown"""
    
    report = f"""# 📊 Relatório Comparativo - Microsserviços vs Monolítico

**Data de Geração:** {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}

---

## 🎯 Resumo Executivo

Este relatório compara o desempenho entre as arquiteturas **Microsserviços** e **Monolítica** em um sistema de rede social simulada, utilizando testes de carga com K6.

### 🏆 Resultado Geral

"""

    # Calcula score geral (menor latência + maior throughput = melhor)
    micro_score = (1000 / micro_metrics['http_req_duration']['avg']) * micro_metrics['http_reqs']['rate']
    mono_score = (1000 / mono_metrics['http_req_duration']['avg']) * mono_metrics['http_reqs']['rate']
    
    winner = "Microsserviços" if micro_score > mono_score else "Monolítico"
    
    report += f"""
**Arquitetura Vencedora em Performance Geral:** ✨ **{winner}** ✨

---

## 📈 Comparação de Métricas Principais

### Throughput (Requisições por Segundo)

| Métrica | Microsserviços | Monolítico | Diferença | Vencedor |
|---------|----------------|------------|-----------|----------|
"""
    
    report += generate_comparison_table(
        "Taxa de Requisições",
        micro_metrics['http_reqs']['rate'],
        mono_metrics['http_reqs']['rate'],
        " req/s",
        lower_is_better=False
    )
    
    report += generate_comparison_table(
        "Total de Requisições",
        micro_metrics['http_reqs']['count'],
        mono_metrics['http_reqs']['count'],
        "",
        lower_is_better=False
    )
    
    report += f"""
---

### ⏱️ Latência (Tempo de Resposta)

| Métrica | Microsserviços | Monolítico | Diferença | Vencedor |
|---------|----------------|------------|-----------|----------|
"""
    
    for metric, label in [('avg', 'Média'), ('med', 'Mediana (P50)'), 
                          ('p90', 'P90'), ('p95', 'P95'), ('p99', 'P99'), ('max', 'Máxima')]:
        report += generate_comparison_table(
            label,
            micro_metrics['http_req_duration'][metric],
            mono_metrics['http_req_duration'][metric],
            " ms",
            lower_is_better=True
        )
    
    report += f"""
---

### ✅ Confiabilidade

| Métrica | Microsserviços | Monolítico | Diferença | Vencedor |
|---------|----------------|------------|-----------|----------|
"""
    
    micro_success = 100 - micro_metrics['http_req_failed']['rate']
    mono_success = 100 - mono_metrics['http_req_failed']['rate']
    
    report += generate_comparison_table(
        "Taxa de Sucesso",
        micro_success,
        mono_success,
        "%",
        lower_is_better=False
    )
    
    report += generate_comparison_table(
        "Taxa de Erro",
        micro_metrics['http_req_failed']['rate'],
        mono_metrics['http_req_failed']['rate'],
        "%",
        lower_is_better=True
    )
    
    report += f"""
---

### 🔄 Escalabilidade

| Métrica | Microsserviços | Monolítico | Diferença | Vencedor |
|---------|----------------|------------|-----------|----------|
"""
    
    report += generate_comparison_table(
        "Iterações Completas",
        micro_metrics['iterations']['count'],
        mono_metrics['iterations']['count'],
        "",
        lower_is_better=False
    )
    
    report += generate_comparison_table(
        "Taxa de Iterações",
        micro_metrics['iterations']['rate'],
        mono_metrics['iterations']['rate'],
        " iter/s",
        lower_is_better=False
    )
    
    report += f"""
---

### 🌐 Transferência de Dados

| Métrica | Microsserviços | Monolítico |
|---------|----------------|------------|
| **Dados Recebidos** | {format_bytes(micro_metrics['data_received']['count'])} | {format_bytes(mono_metrics['data_received']['count'])} |
| **Taxa de Download** | {format_bytes(micro_metrics['data_received']['rate'])}/s | {format_bytes(mono_metrics['data_received']['rate'])}/s |
| **Dados Enviados** | {format_bytes(micro_metrics['data_sent']['count'])} | {format_bytes(mono_metrics['data_sent']['count'])} |
| **Taxa de Upload** | {format_bytes(micro_metrics['data_sent']['rate'])}/s | {format_bytes(mono_metrics['data_sent']['rate'])}/s |

---

## 📊 Análise Comparativa Detalhada

### 🚀 Performance

"""
    
    latency_diff = calculate_percentage_diff(
        micro_metrics['http_req_duration']['avg'],
        mono_metrics['http_req_duration']['avg']
    )
    
    if latency_diff < 0:
        report += f"✅ **Microsserviços apresentou latência {abs(latency_diff):.1f}% MENOR** que o monolítico.\n\n"
    else:
        report += f"⚠️ **Microsserviços apresentou latência {latency_diff:.1f}% MAIOR** que o monolítico.\n\n"
    
    throughput_diff = calculate_percentage_diff(
        micro_metrics['http_reqs']['rate'],
        mono_metrics['http_reqs']['rate']
    )
    
    if throughput_diff > 0:
        report += f"✅ **Microsserviços teve throughput {throughput_diff:.1f}% MAIOR** que o monolítico.\n\n"
    else:
        report += f"⚠️ **Microsserviços teve throughput {abs(throughput_diff):.1f}% MENOR** que o monolítico.\n\n"
    
    report += """
### 🎯 Trade-offs Identificados

#### Vantagens da Arquitetura de Microsserviços

- ✅ **Escalabilidade Independente:** Cada serviço pode escalar individualmente conforme demanda
- ✅ **Isolamento de Falhas:** Problemas em um serviço não afetam necessariamente os outros
- ✅ **Deployment Independente:** Atualizações podem ser feitas sem downtime completo do sistema
- ✅ **Tecnologias Heterogêneas:** Liberdade para usar diferentes tecnologias por serviço
- ✅ **Times Autônomos:** Equipes podem trabalhar independentemente em diferentes serviços

#### Desvantagens da Arquitetura de Microsserviços

- ❌ **Complexidade Operacional:** Maior overhead de infraestrutura e orquestração
- ❌ **Latência de Rede:** Comunicação entre serviços adiciona latência
- ❌ **Transações Distribuídas:** Mais complexo garantir consistência ACID
- ❌ **Debugging Complexo:** Rastreamento de erros através de múltiplos serviços
- ❌ **Overhead de Comunicação:** Serialização/deserialização e chamadas HTTP

#### Vantagens da Arquitetura Monolítica

- ✅ **Simplicidade:** Menor complexidade operacional e de desenvolvimento inicial
- ✅ **Menor Latência:** Chamadas internas são mais rápidas que chamadas HTTP
- ✅ **Transações ACID:** Mais fácil garantir consistência com banco único
- ✅ **Debugging Simples:** Stack traces completos e logs centralizados
- ✅ **Menos Overhead:** Sem overhead de rede entre componentes

#### Desvantagens da Arquitetura Monolítica

- ❌ **Escalabilidade Limitada:** Todo o sistema precisa escalar junto
- ❌ **Acoplamento:** Maior risco de dependências e efeitos colaterais
- ❌ **Deployment Arriscado:** Qualquer mudança requer deploy completo
- ❌ **Tamanho:** Aplicação pode crescer muito e ficar difícil de manter
- ❌ **Tecnologia Única:** Difícil adotar novas tecnologias

---

## 🎓 Conclusões para o TCC

### Quando Usar Microsserviços

A arquitetura de microsserviços é mais adequada para:

1. **Aplicações grandes e complexas** com múltiplos domínios de negócio
2. **Times grandes e distribuídos** que precisam de autonomia
3. **Requisitos de alta disponibilidade** e tolerância a falhas
4. **Necessidade de escalar componentes específicos** independentemente
5. **Evolução tecnológica contínua** com necessidade de experimentação

### Quando Usar Monolítico

A arquitetura monolítica é mais adequada para:

1. **MVPs e protótipos** que precisam ser desenvolvidos rapidamente
2. **Times pequenos** com poucos desenvolvedores
3. **Aplicações com baixa complexidade** de domínio
4. **Requisitos de latência crítica** onde cada milissegundo conta
5. **Recursos limitados** de infraestrutura e DevOps

### Recomendação Final

"""
    
    if micro_score > mono_score:
        report += """
Com base nos testes realizados, **a arquitetura de microsserviços demonstrou melhor performance geral** 
para este caso de uso específico (rede social). No entanto, é importante considerar que:

- A complexidade operacional adicional pode não justificar os ganhos de performance em cenários menores
- Os custos de desenvolvimento e manutenção são significativamente maiores
- A decisão deve considerar não apenas performance, mas também o contexto organizacional e técnico

Para o cenário testado (rede social com múltiplos domínios), microsserviços se mostrou uma escolha adequada.
"""
    else:
        report += """
Com base nos testes realizados, **a arquitetura monolítica demonstrou melhor performance** 
para este caso de uso específico. Isso indica que:

- A simplicidade arquitetural resultou em menor latência
- O overhead de comunicação entre microsserviços impactou a performance
- Para aplicações de médio porte, o monolítico pode ser mais eficiente

Recomenda-se iniciar com monolítico modular e migrar para microsserviços apenas quando:
- A aplicação atingir escala significativa
- Houver necessidade comprovada de escalabilidade independente
- A organização possuir maturidade em DevOps e orquestração
"""
    
    report += f"""
---

## 📁 Arquivos de Dados

- **Microsserviços:** Ver arquivos na pasta `test-results/microservices_*`
- **Monolítico:** Ver arquivos na pasta `test-results/monolithic_*`

---

## 📚 Referências Técnicas

1. Newman, S. (2021). *Building Microservices*. O'Reilly Media.
2. Richardson, C. (2018). *Microservices Patterns*. Manning Publications.
3. Fowler, M. & Lewis, J. (2014). *Microservices: A Definition of This New Architectural Term*.
4. Spring Cloud Documentation. https://spring.io/projects/spring-cloud

---

**Gerado automaticamente pelo script de análise do TCC**  
**Ferramenta de teste:** K6  
**Data:** {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}
"""
    
    output_file = output_dir / f"relatorio_comparativo_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    return output_file

def print_terminal_summary(micro_metrics: Dict[str, Any], mono_metrics: Optional[Dict[str, Any]] = None):
    """Exibe resumo formatado no terminal"""
    print("\n" + "=" * 70)
    print("📊 RESUMO DAS MÉTRICAS")
    print("=" * 70 + "\n")
    
    if mono_metrics:
        # Comparação
        print("🚀 THROUGHPUT")
        print(f"  Microsserviços: {micro_metrics['http_reqs']['rate']:>10.2f} req/s")
        print(f"  Monolítico:     {mono_metrics['http_reqs']['rate']:>10.2f} req/s")
        diff = calculate_percentage_diff(micro_metrics['http_reqs']['rate'], mono_metrics['http_reqs']['rate'])
        print(f"  Diferença:      {diff:>10.2f}%")
        
        print("\n⏱️  LATÊNCIA MÉDIA")
        print(f"  Microsserviços: {micro_metrics['http_req_duration']['avg']:>10.2f} ms")
        print(f"  Monolítico:     {mono_metrics['http_req_duration']['avg']:>10.2f} ms")
        diff = calculate_percentage_diff(micro_metrics['http_req_duration']['avg'], mono_metrics['http_req_duration']['avg'])
        print(f"  Diferença:      {diff:>10.2f}%")
        
        print("\n✅ TAXA DE SUCESSO")
        micro_success = 100 - micro_metrics['http_req_failed']['rate']
        mono_success = 100 - mono_metrics['http_req_failed']['rate']
        print(f"  Microsserviços: {micro_success:>10.2f}%")
        print(f"  Monolítico:     {mono_success:>10.2f}%")
        
        # Determina vencedor
        print("\n🏆 VENCEDOR")
        if micro_metrics['http_req_duration']['avg'] < mono_metrics['http_req_duration']['avg']:
            print("  ✨ Microsserviços apresentou menor latência!")
        else:
            print("  ✨ Monolítico apresentou menor latência!")
    else:
        # Apenas microsserviços
        print("📊 MICROSSERVIÇOS")
        print(f"  Total de Requisições: {micro_metrics['http_reqs']['count']:,}")
        print(f"  Taxa: {micro_metrics['http_reqs']['rate']:.2f} req/s")
        print(f"\n⏱️  LATÊNCIA")
        print(f"  Média: {micro_metrics['http_req_duration']['avg']:.2f} ms")
        print(f"  P95:   {micro_metrics['http_req_duration']['p95']:.2f} ms")
        print(f"  P99:   {micro_metrics['http_req_duration']['p99']:.2f} ms")
        print(f"\n✅ CONFIABILIDADE")
        print(f"  Taxa de Erro:    {micro_metrics['http_req_failed']['rate']:.2f}%")
        print(f"  Taxa de Sucesso: {100 - micro_metrics['http_req_failed']['rate']:.2f}%")
        
        # Gráfico ASCII
        latency_data = {
            'Média': micro_metrics['http_req_duration']['avg'],
            'P90': micro_metrics['http_req_duration']['p90'],
            'P95': micro_metrics['http_req_duration']['p95'],
            'P99': micro_metrics['http_req_duration']['p99'],
        }
        print(generate_ascii_chart(latency_data, "Latências (ms)"))
    
    print("\n" + "=" * 70)

def main():
    parser = argparse.ArgumentParser(
        description='Gera relatório comparativo de testes de carga',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos de uso:
  %(prog)s test-results/
  %(prog)s test-results/ --micro microservices_summary.json
  %(prog)s test-results/ --micro micro.json --mono mono.json
        """
    )
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
    
    # Carrega dados
    print("\n📄 Carregando dados...")
    micro_summary = load_k6_summary(args.micro)
    
    if not micro_summary:
        sys.exit(1)
    
    micro_metrics = extract_metrics(micro_summary)
    
    if not micro_metrics:
        print("❌ Erro ao extrair métricas de microsserviços!")
        sys.exit(1)
    
    # Se tem dados do monolítico, faz comparação
    if args.mono and Path(args.mono).exists():
        mono_summary = load_k6_summary(args.mono)
        
        if mono_summary:
            mono_metrics = extract_metrics(mono_summary)
            
            if mono_metrics:
                # Gera relatório comparativo
                print("\n📝 Gerando relatório comparativo...")
                report_file = generate_markdown_report_comparison(micro_metrics, mono_metrics, results_dir)
                print(f"\n✅ Relatório gerado: {report_file}")
                
                # Exibe resumo no terminal
                print_terminal_summary(micro_metrics, mono_metrics)
                
                print(f"\n📄 Relatório completo: {report_file}")
                print("\n🎓 Boa sorte com seu TCC!\n")
                sys.exit(0)
    
    # Se chegou aqui, só tem dados de microsserviços
    print("\n⚠️  Arquivo de monolítico não encontrado!")
    print("Você pode executar: ./run-load-test.sh monolithic")
    print("Por enquanto, gerando relatório apenas de microsserviços...\n")
    
    # Gera relatório apenas de microsserviços
    report_file = generate_markdown_report_single(micro_metrics, "Microsserviços", results_dir)
    print(f"\n✅ Relatório gerado: {report_file}")
    
    # Exibe resumo no terminal
    print_terminal_summary(micro_metrics)
    
    print(f"\n📄 Relatório: {report_file}")
    print("\n🎓 Execute os testes do monolítico para gerar o relatório comparativo!\n")

if __name__ == '__main__':
    main()
