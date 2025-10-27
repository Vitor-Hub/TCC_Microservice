import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ============================================
// MÉTRICAS CUSTOMIZADAS
// ============================================
const errorRate = new Rate('errors');
const userCreationTime = new Trend('user_creation_time');
const postCreationTime = new Trend('post_creation_time');
const friendshipCreationTime = new Trend('friendship_creation_time');
const likeCreationTime = new Trend('like_creation_time');
const commentCreationTime = new Trend('comment_creation_time');
const getUsersTime = new Trend('get_users_time');
const getPostsTime = new Trend('get_posts_time');

// ============================================
// CONFIGURAÇÃO DE CENÁRIOS
// ============================================
export const options = {
  // Configuração de cenários que simulam uso real
  scenarios: {
    // CENÁRIO 1: Usuários navegando (leitura pesada)
    browsing: {
      executor: 'ramping-vus',
      exec: 'browsingScenario',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },   // Warm up
        { duration: '2m', target: 50 },    // Carga normal
        { duration: '1m', target: 100 },   // Pico
        { duration: '30s', target: 0 },    // Cool down
      ],
      tags: { scenario: 'browsing' },
    },

    // CENÁRIO 2: Usuários criando conteúdo
    content_creation: {
      executor: 'ramping-vus',
      exec: 'contentCreationScenario',
      startTime: '30s',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 15 },
        { duration: '2m', target: 30 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
      tags: { scenario: 'content_creation' },
    },

    // CENÁRIO 3: Interações sociais (likes, comments)
    social_interactions: {
      executor: 'ramping-vus',
      exec: 'socialInteractionScenario',
      startTime: '1m',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 25 },
        { duration: '2m', target: 60 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      tags: { scenario: 'social_interactions' },
    },

    // CENÁRIO 4: Teste de stress (pico súbito)
    stress_test: {
      executor: 'ramping-vus',
      exec: 'stressScenario',
      startTime: '5m',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 200 },  // Subida rápida
        { duration: '1m', target: 200 },   // Mantém
        { duration: '15s', target: 0 },    // Descida rápida
      ],
      tags: { scenario: 'stress' },
    },
  },

  // Limites aceitáveis
  thresholds: {
    'http_req_duration': ['p(95)<1000', 'p(99)<2000'],
    'http_req_failed': ['rate<0.05'],
    'errors': ['rate<0.05'],
  },
};

// ============================================
// CONSTANTES
// ============================================
const BASE_URL = 'http://localhost:8765';
const THINK_TIME = 1; // Tempo de "pensar" entre ações (em segundos)

// ============================================
// FUNÇÕES DE API
// ============================================

// --- USERS ---
function createUser() {
  const username = `user_${Date.now()}_${__VU}_${__ITER}`;
  const payload = JSON.stringify({
    username: username,
    email: `${username}@test.com`,
    password: 'senha123',
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/user-ms/api/users`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_user' },
  });

  userCreationTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200 && res.status !== 201);

  check(res, {
    'user created': (r) => r.status === 200 || r.status === 201,
  });

  return res.status === 200 || res.status === 201 ? JSON.parse(res.body) : null;
}

function getUsers() {
  const startTime = Date.now();
  const res = http.get(`${BASE_URL}/user-ms/api/users`, {
    tags: { operation: 'get_users' },
  });

  getUsersTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200);

  return res.status === 200 ? JSON.parse(res.body) : [];
}

function getUserById(userId) {
  const res = http.get(`${BASE_URL}/user-ms/api/users/${userId}`, {
    tags: { operation: 'get_user' },
  });

  errorRate.add(res.status !== 200);
  return res.status === 200 ? JSON.parse(res.body) : null;
}

// --- POSTS ---
function createPost(userId) {
  const payload = JSON.stringify({
    content: `Post do teste de carga - ${new Date().toISOString()}`,
    user: { id: userId },
    comments: [],
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/post-ms/api/posts`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_post' },
  });

  postCreationTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200 && res.status !== 201);

  check(res, {
    'post created': (r) => r.status === 200 || r.status === 201,
  });

  return res.status === 200 || res.status === 201 ? JSON.parse(res.body) : null;
}

function getPosts() {
  const startTime = Date.now();
  const res = http.get(`${BASE_URL}/post-ms/api/posts`, {
    tags: { operation: 'get_posts' },
  });

  getPostsTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200);

  return res.status === 200 ? JSON.parse(res.body) : [];
}

function getPostsByUser(userId) {
  const res = http.get(`${BASE_URL}/post-ms/api/posts/user/${userId}`, {
    tags: { operation: 'get_posts_by_user' },
  });

  errorRate.add(res.status !== 200);
  return res.status === 200 ? JSON.parse(res.body) : [];
}

// --- LIKES ---
function createLike(userId, postId) {
  const payload = JSON.stringify({
    userId: userId,
    postId: postId,
    commentId: null,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/like-ms/api/likes`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_like' },
  });

  likeCreationTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200 && res.status !== 201);

  check(res, {
    'like created': (r) => r.status === 200 || r.status === 201,
  });
}

// --- COMMENTS ---
function createComment(userId, postId) {
  const payload = JSON.stringify({
    content: `Comentário de teste - ${Date.now()}`,
    userId: userId,
    postId: postId,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/comment-ms/api/comments`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_comment' },
  });

  commentCreationTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200 && res.status !== 201);

  check(res, {
    'comment created': (r) => r.status === 200 || r.status === 201,
  });
}

function getCommentsByPost(postId) {
  const res = http.get(`${BASE_URL}/comment-ms/api/comments/post/${postId}`, {
    tags: { operation: 'get_comments' },
  });

  errorRate.add(res.status !== 200);
  return res.status === 200 ? JSON.parse(res.body) : [];
}

// --- FRIENDSHIPS ---
function createFriendship(userId1, userId2) {
  const payload = JSON.stringify({
    userId1: userId1,
    userId2: userId2,
    status: 'pending',
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/friendship-ms/api/friendships`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_friendship' },
  });

  friendshipCreationTime.add(Date.now() - startTime);
  errorRate.add(res.status !== 200 && res.status !== 201);

  check(res, {
    'friendship created': (r) => r.status === 200 || r.status === 201,
  });
}

// ============================================
// CENÁRIOS DE TESTE
// ============================================

// CENÁRIO 1: Navegação (leitura)
export function browsingScenario() {
  group('Browsing - User Feed', () => {
    // 1. Ver todos os posts
    const posts = getPosts();
    sleep(THINK_TIME);

    // 2. Ver usuários
    const users = getUsers();
    sleep(THINK_TIME);

    // 3. Se houver posts, ver comentários de um aleatório
    if (posts.length > 0) {
      const randomPost = posts[Math.floor(Math.random() * posts.length)];
      getCommentsByPost(randomPost.id);
      sleep(THINK_TIME);
    }
  });
}

// CENÁRIO 2: Criação de conteúdo
export function contentCreationScenario() {
  group('Content Creation', () => {
    // 1. Criar ou pegar usuário
    let users = getUsers();
    let user;

    if (users.length > 0 && Math.random() > 0.3) {
      user = users[Math.floor(Math.random() * users.length)];
    } else {
      user = createUser();
    }

    if (!user) return;
    sleep(THINK_TIME);

    // 2. Criar post
    const post = createPost(user.id);
    sleep(THINK_TIME);

    // 3. Ver seus próprios posts
    if (user.id) {
      getPostsByUser(user.id);
      sleep(THINK_TIME);
    }
  });
}

// CENÁRIO 3: Interações sociais
export function socialInteractionScenario() {
  group('Social Interactions', () => {
    const users = getUsers();
    const posts = getPosts();

    if (users.length === 0 || posts.length === 0) return;

    const randomUser = users[Math.floor(Math.random() * users.length)];
    const randomPost = posts[Math.floor(Math.random() * posts.length)];

    // 1. Dar like
    if (Math.random() > 0.4) {
      createLike(randomUser.id, randomPost.id);
      sleep(THINK_TIME * 0.5);
    }

    // 2. Comentar
    if (Math.random() > 0.6) {
      createComment(randomUser.id, randomPost.id);
      sleep(THINK_TIME);
    }

    // 3. Criar amizade
    if (users.length >= 2 && Math.random() > 0.7) {
      const randomUser2 = users[Math.floor(Math.random() * users.length)];
      if (randomUser.id !== randomUser2.id) {
        createFriendship(randomUser.id, randomUser2.id);
        sleep(THINK_TIME);
      }
    }
  });
}

// CENÁRIO 4: Stress test (mistura tudo)
export function stressScenario() {
  const action = Math.random();

  if (action < 0.5) {
    browsingScenario();
  } else if (action < 0.75) {
    contentCreationScenario();
  } else {
    socialInteractionScenario();
  }

  sleep(Math.random() * 2); // Variação aleatória
}

// ============================================
// RESUMO CUSTOMIZADO
// ============================================
export function handleSummary(data) {
    const errorRate = (data.metrics.errors.values.rate || 0) * 100;
    const p95Latency = data.metrics.http_req_duration.values['p(95)'] || 0;
    const p99Latency = data.metrics.http_req_duration.values['p(99)'];  // pode ser undefined
    const failRate = (data.metrics.http_req_failed.values.rate || 0) * 100;

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'summary.html': htmlReport(data),
        [`results/microservices-${__ENV.TIMESTAMP}.json`]: JSON.stringify(data),
        'tcc-report-microservices.json': JSON.stringify({
            timestamp: new Date().toISOString(),
            summary: {
                total_requests: data.metrics.http_reqs.values.count,
                error_rate: errorRate.toFixed(2) + '%',
                http_failure_rate: failRate.toFixed(2) + '%',
                p95_latency: p95Latency.toFixed(2) + 'ms',
                p99_latency: p99Latency ? p99Latency.toFixed(2) + 'ms' : 'N/A',  // FIX HERE
                throughput: (data.metrics.http_reqs.values.rate || 0).toFixed(2) + ' req/s'
            },
            thresholds: {
                errors_passed: data.metrics.errors.thresholds['rate<0.05'].ok,
                latency_p95_passed: data.metrics.http_req_duration.thresholds['p(95)<1000'].ok,
                latency_p99_passed: data.metrics.http_req_duration.thresholds['p(99)<2000'].ok,
                http_failures_passed: data.metrics.http_req_failed.thresholds['rate<0.05'].ok
            }
        }, null, 2)
    };
}

function textSummary(data) {
  return `
╔═══════════════════════════════════════════════════════════╗
║          📊 TESTE DE CARGA - REDE SOCIAL TCC              ║
╠═══════════════════════════════════════════════════════════╣
║ ⏱️  Duração: ${(data.state.testRunDurationMs / 1000).toFixed(0)}s
║ 👥 VUs Máximo: ${data.metrics.vus_max.values.max}
║ 📨 Requisições Totais: ${data.metrics.http_reqs.values.count}
║ 🚀 Throughput: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s
║ ❌ Taxa de Erro: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%
╠═══════════════════════════════════════════════════════════╣
║ ⚡ LATÊNCIA HTTP
║    P50 (mediana): ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)}ms
║    P95:           ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
║    P99:           ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms
║    Média:         ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
║    Máxima:        ${data.metrics.http_req_duration.values.max.toFixed(2)}ms
╠═══════════════════════════════════════════════════════════╣
║ 📝 OPERAÇÕES CUSTOMIZADAS (média)
║    Criar User:    ${data.metrics.user_creation_time?.values.avg.toFixed(2) || 'N/A'}ms
║    Criar Post:    ${data.metrics.post_creation_time?.values.avg.toFixed(2) || 'N/A'}ms
║    Criar Like:    ${data.metrics.like_creation_time?.values.avg.toFixed(2) || 'N/A'}ms
║    Criar Comment: ${data.metrics.comment_creation_time?.values.avg.toFixed(2) || 'N/A'}ms
╚═══════════════════════════════════════════════════════════╝
  `;
}

function htmlReport(report) {
  return `<!DOCTYPE html>
<html>
<head>
  <title>Relatório de Teste - TCC</title>
  <style>
    body { font-family: Arial; padding: 20px; background: #f5f5f5; }
    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; }
    h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }
    .metric { margin: 15px 0; padding: 15px; background: #f9f9f9; border-left: 4px solid #4CAF50; }
    .metric h3 { margin-top: 0; color: #4CAF50; }
    .value { font-size: 24px; font-weight: bold; color: #333; }
  </style>
</head>
<body>
  <div class="container">
    <h1>📊 Relatório de Teste de Carga - Microsserviços</h1>

    <div class="metric">
      <h3>⏱️ Informações do Teste</h3>
      <p>Duração: <span class="value">${report.test_info.duration_seconds}s</span></p>
      <p>VUs Máximo: <span class="value">${report.test_info.max_vus}</span></p>
    </div>

    <div class="metric">
      <h3>📨 Requisições</h3>
      <p>Total: <span class="value">${report.requests.total}</span></p>
      <p>Por segundo: <span class="value">${report.requests.per_second}</span></p>
      <p>Taxa de falha: <span class="value">${report.requests.failed_percentage}%</span></p>
    </div>

    <div class="metric">
      <h3>⚡ Latência</h3>
      <p>P50: <span class="value">${report.latency.p50}ms</span></p>
      <p>P95: <span class="value">${report.latency.p95}ms</span></p>
      <p>P99: <span class="value">${report.latency.p99}ms</span></p>
    </div>
  </div>
</body>
</html>`;
}