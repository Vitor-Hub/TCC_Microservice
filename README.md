# TCC — Comparação Empírica: Arquitetura Monolítica vs. Microsserviços

> Trabalho de Conclusão de Curso (TCC) — UERJ, Faculdade de Engenharia, Departamento de Engenharia de Sistemas e Computação
> Aluno: Vitor Santos Pereira (matrícula: 201510170911) | Orientador: Robert Mota Oliveira

## O que é isto?

Este repositório contém **duas versões do mesmo sistema**: uma construída como aplicação única (**monólito**) e outra dividida em serviços independentes (**microsserviços**). As duas fazem exatamente a mesma coisa — a diferença é só a arquitetura interna.

O objetivo é medir, com testes de carga automatizados, **qual arquitetura se comporta melhor em cada situação**: uso normal, pico de tráfego, falha de um componente e sobrecarga extrema. Os resultados alimentam o Capítulo 4 da monografia.

Você não precisa saber programar para rodar os testes: um menu interativo no terminal faz todo o trabalho. Este guia parte do zero absoluto.

---

## Parte 1 — Instalando o necessário

Você vai precisar de 4 programas: **Docker** (roda as aplicações em contêineres), **Java 21** e **Maven** (compilam o código) e **K6** (gera a carga de teste). Siga a seção do seu sistema operacional.

### macOS

1. **Abra o Terminal**: aperte `Cmd + Espaço`, digite `Terminal` e aperte Enter.

2. **Instale o Homebrew** (gerenciador que instala os demais programas). Cole a linha abaixo no Terminal e aperte Enter — ele vai pedir sua senha do Mac (a senha não aparece enquanto digita, é normal):

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

3. **Instale Docker, Java, Maven e K6** (uma linha só):

   ```bash
   brew install --cask docker && brew install openjdk@21 maven k6
   ```

4. **Abra o Docker Desktop**: aperte `Cmd + Espaço`, digite `Docker` e aperte Enter. Na primeira vez ele pede permissões — aceite. Espere o ícone da baleia na barra superior parar de se mexer (significa que está pronto).

5. **Confira se deu certo** — cole cada linha e compare com o resultado esperado:

   | Comando | Resultado esperado |
   |---|---|
   | `docker --version` | `Docker version 24` ou superior |
   | `java -version` | menção a `21` |
   | `mvn -version` | `Apache Maven 3.9` ou superior |
   | `k6 version` | qualquer versão |

### Windows

No Windows, o caminho mais simples é usar o **WSL** (um Linux dentro do Windows, oficial da Microsoft) — o Docker Desktop já depende dele de qualquer forma.

1. **Instale o WSL**: clique no menu Iniciar, digite `PowerShell`, clique com o botão direito → *Executar como administrador*, e cole:

   ```powershell
   wsl --install
   ```

   Reinicie o computador quando pedir. Ao reiniciar, uma janela do Ubuntu abre e pede para criar um usuário e senha — anote-os.

2. **Instale o Docker Desktop**: baixe em [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/), instale com as opções padrão (ele detecta o WSL sozinho) e abra o programa. Em *Settings → Resources → WSL integration*, confirme que a integração com o Ubuntu está ligada.

3. **Abra o Ubuntu** (menu Iniciar → digite `Ubuntu` → Enter) e instale Java, Maven e K6 colando as linhas abaixo:

   ```bash
   sudo apt update && sudo apt install -y openjdk-21-jdk maven
   sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt update && sudo apt install -y k6
   ```

4. **Confira** com os mesmos comandos da tabela do macOS acima. **Daqui em diante, execute tudo dentro da janela do Ubuntu.**

### Linux (Ubuntu/Debian)

```bash
# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # depois disso, faça logout e login de novo

# Java 21 e Maven
sudo apt update && sudo apt install -y openjdk-21-jdk maven

# K6
sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update && sudo apt install -y k6
```

Confira com os mesmos comandos da tabela do macOS.

---

## Parte 2 — Rodando os testes

Todos os comandos abaixo vão no Terminal (macOS/Linux) ou na janela do Ubuntu (Windows).

### Passo 1: baixe o projeto

```bash
git clone https://github.com/Vitor-Hub/TCC_Micros_vs_Monolith.git
cd TCC_Micros_vs_Monolith
```

### Passo 2: abra o console de gerenciamento

```bash
chmod +x start.sh
./start.sh
```

Aparece o menu principal:

```
TCC — MANAGEMENT CONSOLE

  [micro] Microsservices    [mono] Monolith
  [mon]   Monitoring        [0]    Exit
```

Você navega digitando a opção (ex: `mon`) e apertando Enter.

### Passo 3: ligue o monitoramento

Digite `mon` → Enter → `1` → Enter. Isso sobe o Prometheus e o Grafana, que gravam as métricas dos testes. Depois volte ao menu principal (`0`).

### Passo 4: suba a arquitetura que quer testar

**Monólito** (mais rápido, comece por ele):

1. `mono` → `3) Fresh Start` — compila, apaga dados antigos e sobe tudo do zero. Confirme com `yes`. Demora uns 2–3 minutos.
2. Quando voltar ao menu, `4) Health Check` deve mostrar `Monolith App ... OK`.

**Microsserviços** (7 aplicações, demora mais):

1. `micro` → `3) Fresh Start` — confirme com `yes`. Demora uns 5–8 minutos (compila 7 projetos e espera 90 s para os serviços se registrarem).
2. `4) Health Check` deve mostrar `7/7 services healthy`. Se aparecer menos, espere 1 minuto e tente de novo.

> Pode subir as duas ao mesmo tempo — as portas não conflitam. Mas para os testes da monografia, rode uma bateria por vez.

### Passo 5: rode o teste

No submenu da arquitetura escolhida, digite `6) Stress Test (K6)`. Aparece a lista de cenários:

| Opção | Cenário | Duração | O que faz |
|---|---|---|---|
| 1 | Full suite | ~18 min | Carga mista: linha de base, carga constante, stress, pico e leitura intensiva |
| 2 | Baseline | 2 min | 5 usuários constantes — referência de latência |
| 3 | Steady load | 3 min | 20 usuários constantes |
| 4 | Stress test | ~8 min | Rampa progressiva até 150 usuários |
| 5 | Spike test | ~3 min | Pico súbito de 200 usuários |
| 6 | Read-heavy | 2 min | Só leituras |
| 7 | Breakpoint | ~14 min | Rampa até 1500 usuários; **aborta sozinho ao passar de 20% de erro — isso é o comportamento esperado, não um defeito** |
| 8 | Hotspot | ~7 min | Carga concentrada em posts/curtidas |
| 9 | Failure injection | ~6 min | Derruba um componente no meio do teste e mede a disponibilidade |
| 10 | **FULL BATTERY** | ~45 min | Os 4 cenários da monografia em sequência (full suite → hotspot → failure → breakpoint) |

Para reproduzir os dados do Capítulo 4, use a **opção 10** em cada arquitetura (com Fresh Start entre uma e outra, porque o breakpoint deixa o sistema saturado).

### Passo 6: veja os resultados

- **Arquivos**: cada teste salva um resumo em `microsservice/scripts/test-results/` (arquivos `*_summary.json`, nomeados com data e hora).
- **Gráficos**: abra [http://localhost:3000](http://localhost:3000) no navegador (usuário `admin`, senha `admin`). Dashboards:
  - **TCC — Monolith**: métricas do monólito
  - **TCC — Microservices**: métricas por serviço + estado dos circuit breakers
- **Métricas cruas**: [http://localhost:9090](http://localhost:9090) (Prometheus).

### Para desligar tudo

Em cada submenu, opção `7) Stop`. Ou feche o Docker Desktop.

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop não está aberto | Abra o Docker Desktop e espere a baleia estabilizar |
| Health Check mostra menos de 7/7 | Serviços ainda registrando no Eureka | Espere 1–2 min e rode o Health Check de novo |
| Erros 503/405 nos primeiros segundos de teste | Gateway ainda propagando o registro do Eureka (~60–90 s após ficar healthy) | Aguarde e reinicie o teste |
| K6 termina "com erro" no Breakpoint | O cenário aborta por desenho ao cruzar 20% de falhas | Comportamento esperado — os resultados foram salvos normalmente |
| `port is already allocated` | Outro programa usando uma das portas | Feche o programa em conflito ou pare contêineres antigos com `7) Stop` |
| Build falha com erro de Java | Versão errada do Java | `java -version` deve mostrar 21 |

---

## Referência técnica

### Estrutura do repositório

```
TCC_Micros_vs_Monolith/
│
├── microsservice/                   # Implementação em microsserviços
│   ├── user-ms/                     #   Domínio de usuários (porta 18081)
│   ├── post-ms/                     #   Publicações (porta 18082)
│   ├── comment-ms/                  #   Comentários (porta 18083)
│   ├── like-ms/                     #   Curtidas (porta 18084)
│   ├── friendship-ms/               #   Amizades (porta 18085)
│   ├── eureka-server-ms/            #   Service discovery Netflix Eureka (porta 8761)
│   ├── gateway-service-ms/          #   Spring Cloud Gateway — ponto de entrada (porta 18765)
│   ├── scripts/
│   │   ├── k6-load-test.js          #   Script K6 (todos os cenários)
│   │   └── test-results/            #   Resultados dos testes (JSON)
│   └── docker-compose.yml           #   Microsserviços + bancos
│
├── monolith/                        # Implementação monolítica
│   ├── src/main/java/com/mstcc/monolith/
│   │   ├── user/ post/ comment/ like/ friendship/   # Os mesmos 5 domínios
│   │   ├── config/                  #   Cache e observabilidade
│   │   └── exception/               #   Tratamento de erros
│   └── docker-compose.yml           #   Monólito + PostgreSQL
│
├── monitoring/                      # Prometheus + Grafana (monitora as duas pilhas)
├── docker-compose.monitoring.yml
├── start.sh                         # Console de gerenciamento (ponto de entrada)
└── README.md
```

### Portas e coexistência

As duas pilhas foram desenhadas para rodar simultaneamente sem conflito:

| Recurso | Microsserviços | Monólito |
|---|---|---|
| Ponto de entrada | `http://localhost:18765` (gateway) | `http://localhost:8080` |
| Portas dos serviços | 18081 – 18085 | — |
| PostgreSQL (host) | 5433 – 5437 (um por serviço) | 5438 |
| Rede Docker | `mstcc-net` | `mono-net` |
| Prefixo dos contêineres | `mstcc_` | `mono_` |

O Prometheus coleta das duas pilhas via `host.docker.internal` com o rótulo `stack` (`microservices` ou `monolith`). No Linux, o `start.sh` adiciona automaticamente `--add-host=host.docker.internal:host-gateway`.

### Comparação das arquiteturas

**Microsserviços**
- 5 serviços Spring Boot independentes + API Gateway + Eureka
- Um banco PostgreSQL por serviço (padrão *database-per-service*)
- Comunicação entre serviços via Spring Cloud OpenFeign (HTTP/REST)
- Chamadas paralelas via `CompletableFuture` para evitar latência aditiva
- Circuit breakers com Resilience4j (fechado / aberto / meio-aberto)

**Monólito**
- Aplicação Spring Boot única, Java 21
- Banco PostgreSQL compartilhado — os 5 domínios em um schema
- Comunicação entre domínios por injeção direta de `@Service` — zero overhead de rede
- Mesmos parâmetros de cache Caffeine e mesma observabilidade Micrometer/Prometheus dos microsserviços — métricas comparáveis por construção
- Sem Feign, sem Eureka, sem Resilience4j

Ambas as pilhas recebem exatamente as mesmas requisições do K6: cadastro, publicações, feed, comentários, curtidas e amizades. Qualquer diferença medida é atribuível à arquitetura, não à funcionalidade.
