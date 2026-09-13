# TCC: Comparação Empírica entre Arquitetura Monolítica e Microsserviços

> Trabalho de Conclusão de Curso (TCC), UERJ, Faculdade de Engenharia, Departamento de Engenharia de Sistemas e Computação
> Aluno: Vitor Santos Pereira (matrícula: 201510170911) | Orientador: Robert Mota Oliveira

Este repositório contém **duas versões do mesmo sistema**: uma construída como aplicação única (**monólito**) e outra dividida em serviços independentes (**microsserviços**). As duas fazem exatamente a mesma coisa; a diferença está apenas na arquitetura interna.

O objetivo é medir, com testes de carga automatizados, **qual arquitetura se comporta melhor em cada situação**: uso normal, pico de tráfego, falha de um componente e sobrecarga extrema. Os resultados alimentam o Capítulo 4 da monografia.

Para executar os cenários não é preciso compilar nada. Cada versão publicada traz um **pacote pronto**, com as duas arquiteturas já compiladas, anexado à respectiva release:

**[github.com/vispdev/TCC_Micros_vs_Monolith/releases/latest](https://github.com/vispdev/TCC_Micros_vs_Monolith/releases/latest)**

---

## Parte 1: instalação do necessário

São necessários apenas 2 programas: **Docker**, que roda as aplicações em contêineres, e **K6**, que gera a carga de teste. Cada sistema operacional tem uma seção abaixo. Ao final, vale conferir a seção de **recursos do Docker**, porque sem eles as pilhas não sobem.

### macOS

1. **Abertura do Terminal**: `Cmd + Espaço`, digitar `Terminal` e pressionar Enter.

2. **Instalação do Homebrew** (gerenciador que instala os demais programas). A linha abaixo, colada no Terminal, faz a instalação. A senha do Mac é solicitada no meio do processo e não aparece na tela enquanto é digitada, o que é normal:

   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

3. **Instalação do Docker e do k6**:

   ```bash
   brew install --cask docker && brew install k6
   ```

4. **Abertura do Docker Desktop**: `Cmd + Espaço`, digitar `Docker` e pressionar Enter. Na primeira execução ele pede permissões, que devem ser aceitas. O ícone da baleia na barra superior para de se mexer quando o Docker está pronto.

5. **Verificação**: cada comando abaixo deve produzir o resultado indicado.

   | Comando | Resultado esperado |
   |---|---|
   | `docker --version` | `Docker version 24` ou superior |
   | `k6 version` | qualquer versão |

### Windows

No Windows, o caminho mais simples é o **WSL** (um Linux dentro do Windows, oficial da Microsoft), do qual o Docker Desktop já depende de qualquer forma.

1. **Instalação do WSL**: no menu Iniciar, digitar `PowerShell`, clicar com o botão direito, escolher *Executar como administrador* e colar:

   ```powershell
   wsl --install
   ```

   O computador precisa ser reiniciado quando solicitado. Ao reiniciar, uma janela do Ubuntu abre e pede a criação de um usuário e uma senha, que devem ser anotados.

2. **Instalação do Docker Desktop**: baixar em [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/), instalar com as opções padrão (ele detecta o WSL sozinho) e abrir o programa. Em *Settings → Resources → WSL integration*, confirmar que a integração com o Ubuntu está ligada.

3. **Instalação do k6**: abrir o Ubuntu (menu Iniciar, digitar `Ubuntu`, Enter) e colar as linhas abaixo:

   ```bash
   sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt update && sudo apt install -y k6
   ```

4. **Verificação** com os mesmos comandos da tabela do macOS. **Daqui em diante, tudo é executado dentro da janela do Ubuntu.**

> **Importante:** o pacote deve ser descompactado dentro do sistema de arquivos do próprio Ubuntu (a pasta que abre por padrão, `~`), e **não** em `/mnt/c/...`. Rodar a partir do disco do Windows deixa tudo muito mais lento e costuma causar erros de permissão nos scripts.

### Linux (Ubuntu/Debian)

```bash
# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # em seguida, é preciso sair e entrar na sessão novamente

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

### Passo 1: download do pacote

O pacote fica anexado à release de cada versão. O endereço abaixo leva sempre à mais recente:

**[github.com/vispdev/TCC_Micros_vs_Monolith/releases/latest](https://github.com/vispdev/TCC_Micros_vs_Monolith/releases/latest)**

Dentro da seção *Assets*, o arquivo a baixar é:

**`TCC_Micros_vs_Monolith.zip`**

> **Atenção ao arquivo certo.** Logo abaixo dele o GitHub oferece **Source code (zip)** e **Source code (tar.gz)**, que são gerados automaticamente e contêm apenas o código-fonte, sem as aplicações compiladas. Baixar um desses traz o repositório inteiro e **não** permite executar os cenários.

Pelo terminal, o endereço abaixo baixa sempre a versão mais recente, sem precisar saber o número dela:

```bash
curl -L -O https://github.com/vispdev/TCC_Micros_vs_Monolith/releases/latest/download/TCC_Micros_vs_Monolith.zip
```

Em seguida, basta descompactar e entrar na pasta criada, que leva a versão no nome:

```bash
unzip TCC_Micros_vs_Monolith.zip
cd TCC_Micros_vs_Monolith-*/
```

Dentro dela estão as duas arquiteturas já compiladas, os arquivos do Docker, os scripts de teste e o monitoramento. Nada precisa ser compilado.

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

### Sequência completa, tecla por tecla

Quem quiser apenas rodar, sem ler o resto, pode seguir literalmente as sequências abaixo. Cada linha é uma digitação seguida de Enter, e as ações terminam pedindo Enter para continuar.

**Monólito:**

```
mon     (entra no monitoramento)
1       (sobe Prometheus e Grafana)
0       (volta ao menu principal)
mono    (entra no monólito)
3       (Fresh Start: limpa e sobe do zero)
yes     (confirma)
4       (Health Check: deve mostrar Monolith App ... OK)
6       (abre a lista de cenários)
10      (FULL BATTERY, ~60 min; ou 2 para um teste rápido de 2 min)
```

**Microsserviços:**

```
mon     (se o monitoramento ainda não estiver de pé)
1
0
micro   (entra nos microsserviços)
3       (Fresh Start)
yes     (confirma; aguarda 90 s pelo registro no Eureka)
4       (Health Check: deve mostrar 7/7 services healthy)
6       (abre a lista de cenários)
10      (FULL BATTERY, ~60 min; ou 2 para um teste rápido de 2 min)
```

Para encerrar, dentro do submenu da arquitetura, a opção `7` derruba a pilha.

### Passo 4: subida da arquitetura a ser testada

**Monólito** (mais rápido, convém começar por ele):

1. `mono`, depois `3) Fresh Start`, que apaga dados antigos e sobe tudo do zero. A confirmação é `yes`. Leva cerca de 1 minuto.
2. De volta ao menu, `4) Health Check` deve mostrar `Monolith App ... OK`.

**Microsserviços** (7 aplicações, demora mais):

1. `micro`, depois `3) Fresh Start`, confirmando com `yes`. O console aguarda 90 s para os serviços se registrarem.
2. `4) Health Check` deve mostrar `7/7 services healthy`. Se aparecer menos, basta aguardar 1 minuto e repetir.

> O console avisa na tela que está usando os jars já incluídos no pacote e pula a compilação. Isso é o esperado, não é erro. A decisão é tomada pela presença ou ausência do código-fonte, e não pelo Maven estar instalado, de modo que o pacote funciona igual em máquinas que tenham Maven.

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

A reprodução dos cenários do Capítulo 4 usa a **opção 10** em cada arquitetura, com um Fresh Start entre uma e outra, porque o breakpoint deixa o sistema saturado. Os valores absolutos dependem da máquina, do sistema operacional e da camada de virtualização do Docker, e por isso não devem coincidir com os da monografia; o que se reproduz é o comportamento comparativo entre as duas arquiteturas.

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
| O download trouxe o repositório, sem as aplicações | Foi baixado o **Source code (zip)** em vez do pacote | Baixar o arquivo `TCC_Micros_vs_Monolith.zip` na seção *Assets* da release |
| `Cannot connect to the Docker daemon` | Docker Desktop não está aberto | Abrir o Docker Desktop e aguardar a baleia estabilizar |
| Health Check mostra menos de 7/7 | Serviços ainda registrando no Eureka | Aguardar 1 a 2 minutos e repetir o Health Check |
| Erros 503/405 nos primeiros segundos de teste | Gateway ainda propagando o registro do Eureka (60 a 90 s após ficar healthy) | Aguardar e reiniciar o teste |
| K6 termina "com erro" no Breakpoint | O cenário aborta por desenho ao cruzar 20% de falhas | Comportamento esperado; os resultados foram salvos normalmente |
| `port is already allocated` | Outro programa usando uma das portas | Fechar o programa em conflito ou parar contêineres antigos com `7) Stop` |
| Contêiner morre no meio do teste | Docker sem memória suficiente | Conferir a seção de recursos do Docker acima |

---

## Referência técnica

### O que vem no pacote

```
TCC_Micros_vs_Monolith-<versão>/
│
├── microsservice/                   # Implementação em microsserviços
│   ├── user-ms/                     #   Domínio de usuários (porta 18081)
│   ├── post-ms/                     #   Publicações (porta 18082)
│   ├── comment-ms/                  #   Comentários (porta 18083)
│   ├── like-ms/                     #   Curtidas (porta 18084)
│   ├── friendship-ms/               #   Amizades (porta 18085)
│   ├── eureka-server-ms/            #   Service discovery Netflix Eureka (porta 8761)
│   ├── gateway-service-ms/          #   Spring Cloud Gateway (ponto de entrada, porta 18765)
│   ├── scripts/k6-load-test.js      #   Script K6 (todos os cenários)
│   └── docker-compose.yml           #   Microsserviços + bancos
│
├── monolith/                        # Implementação monolítica (aplicação + banco)
├── monitoring/                      # Prometheus + Grafana (monitora as duas pilhas)
├── COMO-EXECUTAR.md                 # Resumo dos passos
└── start.sh                         # Console de gerenciamento (ponto de entrada)
```

Cada aplicação vem como um jar já compilado, na pasta `target/` do respectivo serviço. As imagens Docker são construídas na primeira execução, a partir desses jars, sobre a base oficial `eclipse-temurin:21-jre-jammy`. Como essa base é multi-arquitetura e bytecode Java é neutro de arquitetura, o mesmo pacote funciona em x86_64 e em ARM (Apple Silicon).

### Publicação de versões

Cada tag `v*` empurrada para o repositório dispara o workflow `.github/workflows/release.yml`, que compila as oito aplicações, monta o pacote e o anexa à release correspondente.

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

### Código-fonte

O código das duas implementações está neste repositório, nas pastas `microsservice/` e `monolith/`. Compilar a partir dele exige Java 21 e Maven; com essas ferramentas presentes, o próprio `start.sh` compila antes de subir as pilhas.
