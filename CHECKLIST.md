# ✅ Checklist de Implementação do TCC

Use este checklist para acompanhar o progresso da implementação do seu projeto.

---

## 📦 Fase 1: Setup Inicial

### Pré-requisitos
- [ ] Docker Desktop instalado (6-8GB RAM alocado)
- [ ] Java 17+ instalado (`java -version`)
- [ ] Maven 3.8+ instalado (`mvn -version`)
- [ ] K6 instalado (`k6 version`)
- [ ] Python 3 instalado (`python3 --version`)
- [ ] Git instalado

### Arquivos do Projeto
- [ ] Todos os arquivos criados/corrigidos copiados para o projeto
- [ ] Scripts com permissão de execução (`chmod +x *.sh`)
- [ ] Estrutura de diretórios organizada
- [ ] `.gitignore` configurado

---

## 🏗️ Fase 2: Build e Configuração

### Build dos Microsserviços
- [ ] `./build-all.sh` executado com sucesso
- [ ] Todos os 7 serviços compilados (0 erros)
- [ ] JARs gerados em cada `target/`
- [ ] Imagens Docker criadas (verificar com `docker images`)

### Configuração de Rede
- [ ] Rede `mstcc-net` criada
- [ ] Todos os containers na mesma rede
- [ ] Resolução DNS funcionando entre containers

---

## 🗄️ Fase 3: Bancos de Dados

### Inicialização
- [ ] `docker-compose -f docker-compose.db.yml up -d` executado
- [ ] 5 containers PostgreSQL rodando
- [ ] Todos os bancos passando healthcheck

### Validação
- [ ] Conectar em cada banco: `docker exec -it user_ms_db psql -U user -d userdb -c "SELECT 1;"`
- [ ] user-ms-db (porta 5433) ✅
- [ ] post-ms-db (porta 5434) ✅
- [ ] comment-ms-db (porta 5435) ✅
- [ ] like-ms-db (porta 5436) ✅
- [ ] friendship-ms-db (porta 5437) ✅

---

## 🚀 Fase 4: Microsserviços

### Eureka Server
- [ ] Container `microeureka` rodando
- [ ] Dashboard acessível: http://localhost:8761
- [ ] Healthcheck passando

### API Gateway
- [ ] Container `micro-api-gateway` rodando
- [ ] Healthcheck passando: http://localhost:8765/actuator/health
- [ ] Registrado no Eureka

### Serviços de Negócio
- [ ] User Service (18081) registrado e healthy
- [ ] Post Service (18082) registrado e healthy
- [ ] Comment Service (18083) registrado e healthy
- [ ] Friendship Service (18084) registrado e healthy
- [ ] Like Service (18085) registrado e healthy

### Validação Completa
- [ ] `./validate-environment.sh` executado
- [ ] Todos os testes passando ✅
- [ ] 0 erros críticos
- [ ] Máximo de avisos aceitável (<3)

---

## 🧪 Fase 5: Testes Funcionais

### Testes Manuais
- [ ] Criar usuário via Gateway
- [ ] Listar usuários
- [ ] Buscar usuário por ID
- [ ] Criar post
- [ ] Criar comentário
- [ ] Dar like
- [ ] Criar amizade
- [ ] Todas as operações CRUD funcionando

### Testes Via cURL
```bash
# User
curl -X POST http://localhost:8765/user-ms/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","bio":"Testing"}'

# Post
curl http://localhost:8765/post-ms/api/posts
```

- [ ] Todos os endpoints respondendo corretamente
- [ ] Status code 2xx para operações bem-sucedidas
- [ ] JSON válido retornado

---

## 📊 Fase 6: Monitoramento (Opcional mas Recomendado)

### Prometheus
- [ ] `docker-compose -f docker-compose.monitoring.yml up -d` executado
- [ ] Prometheus acessível: http://localhost:9090
- [ ] Todos os targets "UP"
- [ ] Métricas sendo coletadas

### Grafana
- [ ] Grafana acessível: http://localhost:3000
- [ ] Login com admin/admin
- [ ] Prometheus datasource configurado
- [ ] Dashboard importado (`grafana-dashboard-microservices.json`)
- [ ] Painéis mostrando dados

---

## 🎯 Fase 7: Testes de Carga

### Preparação
- [ ] Todos os serviços estáveis (rodando há pelo menos 5 minutos)
- [ ] Memória e CPU com uso normal (<50%)
- [ ] Nenhum erro nos logs
- [ ] `./validate-environment.sh` passou

### Execução
- [ ] `./run-load-test.sh microservices` executado
- [ ] Teste rodou por ~18 minutos
- [ ] Arquivos salvos em `test-results/`:
  - [ ] `*_k6.json`
  - [ ] `*_summary.json`
  - [ ] `*_output.log`
  - [ ] `*_initial.txt`
  - [ ] `*_final.txt`

### Validação dos Resultados
- [ ] Taxa de erro < 10%
- [ ] Nenhum crash de serviço durante teste
- [ ] Métricas coletadas corretamente
- [ ] Summary JSON válido

---

## 📈 Fase 8: Análise de Resultados

### Geração de Relatório
- [ ] `python3 generate-report.py test-results/` executado
- [ ] Relatório Markdown gerado
- [ ] Gráficos ASCII exibidos no terminal
- [ ] Métricas comparativas calculadas

### Análise Manual
- [ ] Latência média registrada: _____ ms
- [ ] Throughput médio: _____ req/s
- [ ] Taxa de sucesso: _____ %
- [ ] P95 latência: _____ ms
- [ ] P99 latência: _____ ms

### Dashboards
- [ ] Screenshots do Grafana salvos
- [ ] Gráficos de latência capturados
- [ ] Métricas de memória documentadas
- [ ] CPU usage registrado

---

## 📝 Fase 9: Documentação para o TCC

### Dados Coletados
- [ ] Tabela de métricas de performance
- [ ] Gráficos de latência (P50, P95, P99)
- [ ] Gráfico de throughput
- [ ] Uso de recursos (CPU, memória)
- [ ] Taxa de erro sob diferentes cargas

### Análise Qualitativa
- [ ] Complexidade operacional documentada
- [ ] Processo de deployment descrito
- [ ] Debugging e troubleshooting explicado
- [ ] Trade-offs identificados

### Escrita do TCC
- [ ] Seção de Metodologia escrita
- [ ] Seção de Implementação completa
- [ ] Resultados tabelados e grafados
- [ ] Análise e discussão elaborada
- [ ] Conclusões baseadas em dados
- [ ] Recomendações formuladas

---

## 🔄 Fase 10: Implementação do Monolítico

### Build
- [ ] Repositório do monolítico clonado
- [ ] Build executado com sucesso
- [ ] Imagem Docker criada

### Deploy
- [ ] Container do monolítico rodando
- [ ] Banco de dados único configurado
- [ ] Aplicação acessível

### Testes
- [ ] Mesmos testes funcionais executados
- [ ] Testes de carga K6 executados
- [ ] Duração e VUs idênticos aos microsserviços
- [ ] Resultados salvos em `test-results/`

---

## 📊 Fase 11: Comparação Final

### Geração de Relatório Comparativo
- [ ] Ambos os arquivos `*_summary.json` disponíveis
- [ ] `python3 generate-report.py test-results/ --micro <file> --mono <file>` executado
- [ ] Relatório comparativo gerado
- [ ] Tabelas de comparação criadas

### Análise Comparativa
- [ ] Diferenças de latência calculadas
- [ ] Diferenças de throughput calculadas
- [ ] Trade-offs identificados
- [ ] Vencedor por métrica determinado
- [ ] Contextos de uso recomendados

### Documentação
- [ ] Tabela comparativa no TCC
- [ ] Gráficos lado-a-lado
- [ ] Análise de cada métrica
- [ ] Discussão sobre trade-offs
- [ ] Recomendações baseadas em cenários

---

## 🎓 Fase 12: Finalização do TCC

### Revisão
- [ ] Todos os dados verificados
- [ ] Gráficos revisados
- [ ] Conclusões alinhadas com dados
- [ ] Referências bibliográficas completas
- [ ] Formatação de acordo com normas

### Apresentação
- [ ] Slides preparados
- [ ] Demonstração ao vivo testada
- [ ] Backup dos dados preparado
- [ ] Respostas para perguntas comuns ensaiadas

---

## 🚨 Troubleshooting

Se algo der errado em qualquer fase:

1. ✅ Consulte [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. ✅ Execute `./validate-environment.sh`
3. ✅ Verifique logs: `docker-compose logs <service>`
4. ✅ Reinicie serviços problemáticos: `docker restart <container>`
5. ✅ Em último caso: reset completo (veja TROUBLESHOOTING.md)

---

## 📈 Progresso Geral

Marque conforme completa cada fase:

- [ ] Fase 1: Setup Inicial (0/6)
- [ ] Fase 2: Build e Configuração (0/6)
- [ ] Fase 3: Bancos de Dados (0/7)
- [ ] Fase 4: Microsserviços (0/10)
- [ ] Fase 5: Testes Funcionais (0/9)
- [ ] Fase 6: Monitoramento (0/7)
- [ ] Fase 7: Testes de Carga (0/10)
- [ ] Fase 8: Análise de Resultados (0/11)
- [ ] Fase 9: Documentação TCC (0/11)
- [ ] Fase 10: Monolítico (0/7)
- [ ] Fase 11: Comparação Final (0/9)
- [ ] Fase 12: Finalização TCC (0/9)

**Progresso Total: 0/102 itens**

---

## 🎯 Tempo Estimado por Fase

| Fase | Tempo Estimado |
|------|----------------|
| 1. Setup | 15 min |
| 2. Build | 10 min |
| 3. Bancos | 5 min |
| 4. Microsserviços | 10 min |
| 5. Testes Funcionais | 15 min |
| 6. Monitoramento | 10 min |
| 7. Testes de Carga | 20 min |
| 8. Análise | 30 min |
| 9. Documentação TCC | 4-6 horas |
| 10. Monolítico | 30 min |
| 11. Comparação | 1 hora |
| 12. Finalização | 2-4 horas |
| **TOTAL** | **~10-14 horas** |

---

**Última atualização:** 2024  
**Projeto:** TCC - Microsserviços vs. Monolítico

**Boa sorte! 🎓**
