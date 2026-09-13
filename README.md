# TCC: Comparação Empírica entre Arquitetura Monolítica e Microsserviços

> Trabalho de Conclusão de Curso (TCC), UERJ, Faculdade de Engenharia, Departamento de Engenharia de Sistemas e Computação
> Aluno: Vitor Santos Pereira (matrícula: 201510170911) | Orientador: Robert Mota Oliveira

## O que é isto?

Este repositório contém **duas versões do mesmo sistema**: uma construída como aplicação única (**monólito**) e outra dividida em serviços independentes (**microsserviços**). As duas fazem exatamente a mesma coisa; a diferença está apenas na arquitetura interna.

O objetivo é medir, com testes de carga automatizados, **qual arquitetura se comporta melhor em cada situação**: uso normal, pico de tráfego, falha de um componente e sobrecarga extrema. Os resultados alimentam o Capítulo 4 da monografia.

---

## Parte 1: instalação do necessário

São necessários 5 programas: **Git** (baixa o projeto), **Docker** (roda as aplicações em contêineres), **Java 21** e **Maven** (compilam o código) e **K6** (gera a carga de teste). Cada sistema operacional tem uma seção abaixo. Ao final, vale conferir a seção de **recursos do Docker**, porque sem eles as pilhas não sobem.

### macOS

1. **Abertura do Terminal**: `Cmd + Espaço`, digitar `Terminal` e pressionar Enter.

2. **Instalação do Homebrew** (gerenciador que instala os demais programas). A linha abaixo, colada no Terminal, faz a instalação. A senha do Mac é solicitada no meio do processo e não aparece na tela enquanto é digitada, o que é normal:

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

3. **Instalação de Git, Docker, Java, Maven e K6** (uma linha só):

   ```bash
   brew install --cask docker && brew install git openjdk@21 maven k6
   ```

4. **Abertura do Docker Desktop**: `Cmd + Espaço`, digitar `Docker` e pressionar Enter. Na primeira execução ele pede permissões, que devem ser aceitas. O ícone da baleia na barra superior para de se mexer quando o Docker está pronto.

5. **Verificação**: cada comando abaixo deve produzir o resultado indicado.

   | Comando | Resultado esperado |
   |---|---|
   | `docker --version` | `Docker version 24` ou superior |
   | `java -version` | menção a `21` |
   | `mvn -version` | `Apache Maven 3.9` ou superior |
   | `k6 version` | qualquer versão |
   | `git --version` | qualquer versão |

### Windows

No Windows, o caminho mais simples é o **WSL** (um Linux dentro do Windows, oficial da Microsoft), do qual o Docker Desktop já depende de qualquer forma.

1. **Instalação do WSL**: no menu Iniciar, digitar `PowerShell`, clicar com o botão direito, escolher *Executar como administrador* e colar:

   ```powershell
   wsl --install
   ```

   O computador precisa ser reiniciado quando solicitado. Ao reiniciar, uma janela do Ubuntu abre e pede a criação de um usuário e uma senha, que devem ser anotados.

2. **Instalação do Docker Desktop**: baixar em [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/), instalar com as opções padrão (ele detecta o WSL sozinho) e abrir o programa. Em *Settings → Resources → WSL integration*, confirmar que a integração com o Ubuntu está ligada.

3. **Instalação de Git, Java, Maven e K6**: abrir o Ubuntu (menu Iniciar, digitar `Ubuntu`, Enter) e colar as linhas abaixo:

   ```bash
   sudo apt update && sudo apt install -y git openjdk-21-jdk maven
   sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt update && sudo apt install -y k6
   ```

4. **Verificação** com os mesmos comandos da tabela do macOS. **Daqui em diante, tudo é executado dentro da janela do Ubuntu.**

> **Importante:** o projeto deve ser baixado dentro do sistema de arquivos do próprio Ubuntu (a pasta que abre por padrão, `~`), e **não** em `/mnt/c/...`. Rodar a partir do disco do Windows deixa a compilação muito mais lenta e costuma causar erros de permissão nos scripts.

### Linux (Ubuntu/Debian)

```bash
# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # em seguida, é preciso sair e entrar na sessão novamente

# Git, Java 21 e Maven
sudo apt update && sudo apt install -y git openjdk-21-jdk maven

# K6
sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update && sudo apt install -y k6
```

A verificação usa os mesmos comandos da tabela do macOS.

### Recursos que o Docker precisa ter

As duas pilhas declaram limites de CPU e memória por contêiner, e é isso que torna a comparação justa. Somados, os limites exigem:

| Pilha | Contêineres | CPU declarada | Memória declarada |
|---|---|---|---|
| Microsserviços | 12 (Eureka + 5 serviços + gateway + 5 bancos) | 4,5 | 4,5 GB |
| Monólito | 2 (aplicação + banco) | 3,5 | 3,5 GB |

Com **uma pilha por vez**, que é como a monografia coleta os dados, o Docker precisa de pelo menos **6 CPUs e 8 GB de memória**, já contando o Prometheus, o Grafana e uma folga para o sistema. Para as duas pilhas simultâneas, 8 CPUs e 10 GB.

No macOS e no Windows o ajuste fica no Docker Desktop, em *Settings → Resources*. No Linux não há nada a configurar, porque os contêineres usam os recursos da máquina diretamente. Abaixo desses valores, os contêineres são encerrados por falta de memória no meio dos testes e os resultados deixam de ser comparáveis.

---

## Parte 2: execução dos testes

Todos os comandos abaixo vão no Terminal (macOS e Linux) ou na janela do Ubuntu (Windows).

### Passo 1: obtenção do projeto

```bash
git clone https://github.com/Vitor-Hub/TCC_Micros_vs_Monolith.git
cd TCC_Micros_vs_Monolith
```

### Passo 2: abertura do console de gerenciamento

```bash
chmod +x start.sh
./start.sh
```

O menu principal aparece assim:

```
TCC — MANAGEMENT CONSOLE

  [micro] Microsservices    [mono] Monolith
  [mon]   Monitoring        [0]    Exit
```

A navegação é feita digitando a opção (por exemplo, `mon`) e pressionando Enter.

### Passo 3: monitoramento

A sequência `mon`, Enter, `1`, Enter sobe o Prometheus e o Grafana, que gravam as métricas dos testes. Em seguida, `0` volta ao menu principal.

### Passo 4: subida da arquitetura a ser testada

**Monólito** (mais rápido, convém começar por ele):

1. `mono`, depois `3) Fresh Start`, que compila, apaga dados antigos e sobe tudo do zero. A confirmação é `yes`. Leva de 2 a 3 minutos.
2. De volta ao menu, `4) Health Check` deve mostrar `Monolith App ... OK`.

**Microsserviços** (7 aplicações, demora mais):

1. `micro`, depois `3) Fresh Start`, confirmando com `yes`. Leva de 5 a 8 minutos, porque compila 7 projetos e aguarda 90 s para os serviços se registrarem.
2. `4) Health Check` deve mostrar `7/7 services healthy`. Se aparecer menos, basta aguardar 1 minuto e repetir.

> As duas pilhas podem subir ao mesmo tempo, porque as portas não conflitam. Para os testes da monografia, porém, convém rodar uma bateria por vez.

### Passo 5: execução do teste

No submenu da arquitetura escolhida, a opção `6) Stress Test (K6)` abre a lista de cenários:

| Opção | Cenário | Duração | O que faz |
|---|---|---|---|
| 1 | Full suite | ~18 min | Carga mista: linha de base, carga constante, stress, pico e leitura intensiva |
| 2 | Baseline | 2 min | 5 usuários constantes (referência de latência) |
| 3 | Steady load | 3 min | 20 usuários constantes |
| 4 | Stress test | ~8 min | Rampa progressiva até 150 usuários |
| 5 | Spike test | ~3 min | Pico súbito de 200 usuários |
| 6 | Read-heavy | 2 min | Só leituras |
| 7 | Breakpoint | ~14 min | Rampa até 1500 usuários. **Aborta sozinho ao passar de 20% de erro, que é o comportamento esperado e não um defeito** |
| 8 | Hotspot | ~7 min | Carga concentrada em posts e curtidas |
| 9 | Failure injection | ~6 min | Derruba um componente no meio do teste e mede a disponibilidade (nos microsserviços cai só o `comment-ms`; no monólito cai a aplicação inteira) |
| 10 | **FULL BATTERY** | ~60 min | Os 4 cenários da monografia em sequência (full suite, hotspot, failure injection e breakpoint), com 5 min de descanso entre eles para o sistema estabilizar |

A reprodução dos dados do Capítulo 4 usa a **opção 10** em cada arquitetura, com um Fresh Start entre uma e outra, porque o breakpoint deixa o sistema saturado.

### Passo 6: leitura dos resultados

- **Arquivos**: cada teste salva um resumo em `microsservice/scripts/test-results/` (arquivos `*_summary.json`, nomeados com data e hora).
- **Gráficos**: [http://localhost:3000](http://localhost:3000) no navegador (usuário `admin`, senha `admin`). Os painéis são `TCC — Monolith`, com as métricas do monólito, e `TCC — Microservices`, com métricas por serviço e o estado dos circuit breakers.
- **Métricas cruas**: [http://localhost:9090](http://localhost:9090) (Prometheus).

### Encerramento

Em cada submenu, a opção `7) Stop` derruba a pilha correspondente. Fechar o Docker Desktop tem o mesmo efeito.

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop não está aberto | Abrir o Docker Desktop e aguardar a baleia estabilizar |
| Health Check mostra menos de 7/7 | Serviços ainda registrando no Eureka | Aguardar 1 a 2 minutos e repetir o Health Check |
| Erros 503/405 nos primeiros segundos de teste | Gateway ainda propagando o registro do Eureka (60 a 90 s após ficar healthy) | Aguardar e reiniciar o teste |
| K6 termina "com erro" no Breakpoint | O cenário aborta por desenho ao cruzar 20% de falhas | Comportamento esperado; os resultados foram salvos normalmente |
| `port is already allocated` | Outro programa usando uma das portas | Fechar o programa em conflito ou parar contêineres antigos com `7) Stop` |
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
│   ├── gateway-service-ms/          #   Spring Cloud Gateway (ponto de entrada, porta 18765)
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
| Portas dos serviços | 18081 a 18085 | não se aplica |
| PostgreSQL (host) | 5433 a 5437 (um por serviço) | 5438 |
| Rede Docker | `mstcc-net` | `mono-net` |
| Prefixo dos contêineres | `mstcc_` | `mono_` |

O Prometheus coleta das duas pilhas via `host.docker.internal` com o rótulo `stack` (`microservices` ou `monolith`). No Linux, o `start.sh` adiciona automaticamente `--add-host=host.docker.internal:host-gateway`.

### Comparação das arquiteturas

**Microsserviços**
- 5 serviços Spring Boot independentes, mais API Gateway e Eureka
- Um banco PostgreSQL por serviço (padrão *database-per-service*)
- Comunicação entre serviços via Spring Cloud OpenFeign (HTTP/REST)
- Chamadas paralelas via `CompletableFuture` para evitar latência aditiva
- Circuit breakers com Resilience4j (fechado, aberto e meio-aberto)

**Monólito**
- Aplicação Spring Boot única, Java 21
- Banco PostgreSQL compartilhado, com os 5 domínios em um schema
- Comunicação entre domínios por injeção direta de `@Service`, sem overhead de rede
- Mesmos parâmetros de cache Caffeine e mesma observabilidade Micrometer/Prometheus dos microsserviços, o que torna as métricas comparáveis por construção
- Sem Feign, sem Eureka, sem Resilience4j

Ambas as pilhas recebem exatamente as mesmas requisições do K6: cadastro, publicações, feed, comentários, curtidas e amizades. Qualquer diferença medida é atribuível à arquitetura, não à funcionalidade.
