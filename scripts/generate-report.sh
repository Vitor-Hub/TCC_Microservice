#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# TCC - GERADOR DE RELATORIO DE COMPARACAO
# Lê os arquivos _summary_*.json gerados pelo k6 e gera um
# relatório formatado para comparação Monolito vs Microsserviços
# ═══════════════════════════════════════════════════════════════

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/test-results"
REPORT_FILE="$RESULTS_DIR/comparacao_$(date +%Y%m%d_%H%M%S).txt"
CSV_FILE="$RESULTS_DIR/comparacao_$(date +%Y%m%d_%H%M%S).csv"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     TCC - RELATORIO DE COMPARACAO DE DESEMPENHO        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# ─────────────────────────────────────────────
# Verifica dependências
# ─────────────────────────────────────────────
if ! command -v python3 &>/dev/null; then
    echo -e "${RED}python3 nao encontrado. Instale Python 3.x${NC}"
    exit 1
fi

# ─────────────────────────────────────────────
# Lista summaries disponíveis
# ─────────────────────────────────────────────
SUMMARIES=()
while IFS= read -r f; do
    SUMMARIES+=("$f")
done < <(find "$RESULTS_DIR" -name "*_summary_*.json" -o -name "*summary*.json" 2>/dev/null | sort)

if [ ${#SUMMARIES[@]} -eq 0 ]; then
    echo -e "${RED}Nenhum resultado encontrado em: $RESULTS_DIR${NC}"
    echo ""
    echo "Execute um teste de carga primeiro (opcoes 6-11 no manage.sh)"
    exit 1
fi

echo -e "${CYAN}Arquivos de resultado encontrados:${NC}"
for i in "${!SUMMARIES[@]}"; do
    fname=$(basename "${SUMMARIES[$i]}")
    echo "  [$((i+1))] $fname"
done
echo ""

# ─────────────────────────────────────────────
# Gera relatório com Python
# ─────────────────────────────────────────────
python3 - "${SUMMARIES[@]}" "$REPORT_FILE" "$CSV_FILE" << 'PYEOF'
import sys
import json
import os
from datetime import datetime

files = sys.argv[1:-2]
report_path = sys.argv[-2]
csv_path = sys.argv[-1]

def safe_get(d, *keys, default=0):
    for k in keys:
        if isinstance(d, dict):
            d = d.get(k, {})
        else:
            return default
    return d if d != {} else default

def fmt(val, decimals=2):
    try:
        return f"{float(val):.{decimals}f}"
    except Exception:
        return str(val)

results = []

for fpath in files:
    try:
        with open(fpath) as f:
            data = json.load(f)
    except Exception as e:
        print(f"  ERRO ao ler {fpath}: {e}")
        continue

    fname = os.path.basename(fpath)
    metrics = data.get("metrics", {})

    req_dur   = metrics.get("http_req_duration", {})
    req_fail  = metrics.get("http_req_failed", {})
    reqs      = metrics.get("http_reqs", {})
    vus_max   = metrics.get("vus_max", {})
    iter_dur  = metrics.get("iteration_duration", {})

    results.append({
        "file":         fname,
        "vus_max":      safe_get(vus_max, "max"),
        "total_reqs":   safe_get(reqs, "count"),
        "rps":          safe_get(reqs, "rate"),
        "p50_ms":       safe_get(req_dur, "values", "med"),
        "p95_ms":       safe_get(req_dur, "values", "p(95)"),
        "p99_ms":       safe_get(req_dur, "values", "p(99)"),
        "avg_ms":       safe_get(req_dur, "values", "avg"),
        "min_ms":       safe_get(req_dur, "values", "min"),
        "max_ms":       safe_get(req_dur, "values", "max"),
        "error_rate":   safe_get(req_fail, "values", "rate") * 100,
        "iter_avg_ms":  safe_get(iter_dur, "values", "avg"),
    })

if not results:
    print("Nenhum dado valido encontrado nos arquivos.")
    sys.exit(1)

# ─── Relatório texto ───────────────────────────────────────────
header = f"""
╔══════════════════════════════════════════════════════════════════════════════╗
║         TCC - RELATORIO DE DESEMPENHO - MICROSSERVICOS                      ║
║         Gerado em: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}                              ║
╚══════════════════════════════════════════════════════════════════════════════╝

INSTRUCOES:
  Compare as metricas abaixo com os resultados equivalentes do monolito.
  Insira os valores do monolito na coluna 'Monolito' para gerar a tabela
  comparativa final no seu TCC.

"""

col_w = 28
def row(label, *vals):
    parts = [f"{label:<26}"]
    for v in vals:
        parts.append(f"{str(v):>{col_w}}")
    return "  " + "  ".join(parts)

lines = [header]
lines.append("=" * 80)
lines.append(row("Arquivo / Cenario", *[r['file'][:col_w] for r in results]))
lines.append("=" * 80)
lines.append(row("VUs maximos",         *[fmt(r['vus_max'],     0) for r in results]))
lines.append(row("Total requisicoes",   *[fmt(r['total_reqs'],  0) for r in results]))
lines.append(row("Req/s (throughput)",  *[fmt(r['rps'],         2) for r in results]))
lines.append("-" * 80)
lines.append(row("Latencia media (ms)", *[fmt(r['avg_ms'])  for r in results]))
lines.append(row("Latencia P50 (ms)",   *[fmt(r['p50_ms'])  for r in results]))
lines.append(row("Latencia P95 (ms)",   *[fmt(r['p95_ms'])  for r in results]))
lines.append(row("Latencia P99 (ms)",   *[fmt(r['p99_ms'])  for r in results]))
lines.append(row("Latencia min (ms)",   *[fmt(r['min_ms'])  for r in results]))
lines.append(row("Latencia max (ms)",   *[fmt(r['max_ms'])  for r in results]))
lines.append("-" * 80)
lines.append(row("Taxa de erro (%)",    *[fmt(r['error_rate'],  2) for r in results]))
lines.append(row("Duracao iter. (ms)",  *[fmt(r['iter_avg_ms']) for r in results]))
lines.append("=" * 80)

# SLA thresholds (k6 padrao do projeto)
lines.append("")
lines.append("THRESHOLDS (metas do TCC):")
lines.append("  P95 < 3000ms    P99 < 5000ms    Taxa de erro < 10%")
lines.append("")
for r in results:
    p95_ok  = float(r['p95_ms'])  < 3000
    p99_ok  = float(r['p99_ms'])  < 5000
    err_ok  = float(r['error_rate']) < 10
    status  = "PASSOU" if (p95_ok and p99_ok and err_ok) else "FALHOU"
    lines.append(f"  {r['file'][:50]:<52} -> {status}")
    if not p95_ok:
        lines.append(f"    [!] P95 = {fmt(r['p95_ms'])}ms  (limite: 3000ms)")
    if not p99_ok:
        lines.append(f"    [!] P99 = {fmt(r['p99_ms'])}ms  (limite: 5000ms)")
    if not err_ok:
        lines.append(f"    [!] Erros = {fmt(r['error_rate'],1)}%  (limite: 10%)")

report_text = "\n".join(lines)
print(report_text)

with open(report_path, "w") as f:
    f.write(report_text)

# ─── CSV para importar no Excel/Google Sheets ──────────────────
csv_rows = [
    "arquivo,vus_max,total_reqs,rps,avg_ms,p50_ms,p95_ms,p99_ms,min_ms,max_ms,error_rate_pct"
]
for r in results:
    csv_rows.append(
        f"{r['file']},{fmt(r['vus_max'],0)},{fmt(r['total_reqs'],0)},"
        f"{fmt(r['rps'])},{fmt(r['avg_ms'])},{fmt(r['p50_ms'])},"
        f"{fmt(r['p95_ms'])},{fmt(r['p99_ms'])},{fmt(r['min_ms'])},"
        f"{fmt(r['max_ms'])},{fmt(r['error_rate'],2)}"
    )

with open(csv_path, "w") as f:
    f.write("\n".join(csv_rows))

print(f"\nRelatorio salvo em: {report_path}")
print(f"CSV salvo em:       {csv_path}")
PYEOF

echo ""
echo -e "${GREEN}Relatorio gerado com sucesso!${NC}"
echo -e "  TXT: ${CYAN}$REPORT_FILE${NC}"
echo -e "  CSV: ${CYAN}$CSV_FILE${NC}"
echo ""
echo -e "${YELLOW}Dica: importe o CSV no Excel ou Google Sheets para gerar graficos${NC}"
echo -e "      comparativos entre Microsservicos e Monolito no TCC."
echo ""
