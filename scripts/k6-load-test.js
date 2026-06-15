import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ═══════════════════════════════════════════════════════════════
// 📊 MÉTRICAS CUSTOMIZADAS
// ═══════════════════════════════════════════════════════════════
const errorRate = new Rate('errors');
const userCreationTime = new Trend('user_creation_duration');
const postCreationTime = new Trend('post_creation_duration');
const likeCreationTime = new Trend('like_creation_duration');
const commentCreationTime = new Trend('comment_creation_duration');
const friendshipCreationTime = new Trend('friendship_creation_duration');
const feedLoadTime = new Trend('feed_load_duration');
const getOperationTime = new Trend('get_operation_duration');
const totalRequests = new Counter('total_requests');
const successfulRequests = new Counter('successful_requests');
const failedRequests = new Counter('failed_requests');

// ═══════════════════════════════════════════════════════════════
// ⚙️ CONFIGURAÇÃO DE CARGA
// ═══════════════════════════════════════════════════════════════
export const options = {
  scenarios: {
    // Cenário 1: Carga Constante Baixa (Baseline)
    baseline_load: {
      executor: 'constant-vus',
      vus: 5,
      duration: '2m',
      startTime: '0s',
      tags: { scenario: 'baseline' },
      gracefulStop: '30s',
    },
    
    // Cenário 2: Carga Constante Média
    steady_load: {
      executor: 'constant-vus',
      vus: 20,
      duration: '3m',
      startTime: '2m',
      tags: { scenario: 'steady' },
      gracefulStop: '30s',
    },
    
    // Cenário 3: Rampa Progressiva (Stress Test)
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },   // Aquecimento
        { duration: '2m', target: 60 },   // Carga média
        { duration: '2m', target: 100 },  // Carga alta
        { duration: '1m', target: 150 },  // Pico de estresse
        { duration: '1m', target: 50 },   // Cooldown
        { duration: '1m', target: 0 },    // Finalização
      ],
      startTime: '5m',
      tags: { scenario: 'stress' },
      gracefulStop: '30s',
    },
    
    // Cenário 4: Spike Test (Picos repentinos)
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '15s', target: 200 },  // Spike súbito!
        { duration: '1m', target: 200 },   // Mantém o pico
        { duration: '15s', target: 10 },   // Retorna ao normal
        { duration: '30s', target: 10 },
      ],
      startTime: '13m',
      tags: { scenario: 'spike' },
      gracefulStop: '30s',
    },
    
    // Cenário 5: Operações de Leitura Intensiva
    read_heavy: {
      executor: 'constant-vus',
      vus: 30,
      duration: '2m',
      startTime: '16m',
      tags: { scenario: 'read_heavy' },
      exec: 'readOnlyScenario',
      gracefulStop: '30s',
    },
  },
  
  thresholds: {
    'http_req_duration': ['p(95)<3000', 'p(99)<5000'],
    'http_req_failed': ['rate<0.10'],
    'errors': ['rate<0.15'],
    'user_creation_duration': ['p(95)<2000'],
    'post_creation_duration': ['p(95)<2000'],
    'feed_load_duration': ['p(95)<3000'],
  },
};

// ═══════════════════════════════════════════════════════════════
// 🌐 CONFIGURAÇÃO BASE
// ═══════════════════════════════════════════════════════════════
const BASE_URL = 'http://localhost:18765';  // CORRIGIDO: porta 18765

// SharedArray para IDs criados (compartilhado entre VUs)
const sharedUserIds = new SharedArray('userIds', function() { return []; });
const sharedPostIds = new SharedArray('postIds', function() { return []; });

// ═══════════════════════════════════════════════════════════════
// 🎯 FUNÇÕES AUXILIARES
// ═══════════════════════════════════════════════════════════════
function randomString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function randomEmail() {
  return `user_${randomString(10)}_${Date.now()}@test.com`;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomElement(array) {
  if (!array || array.length === 0) return null;
  return array[randomInt(0, array.length - 1)];
}

function handleResponse(res, metricTrend, operationName) {
  totalRequests.add(1);
  
  const success = check(res, {
    [`${operationName}: status 2xx`]: (r) => r.status >= 200 && r.status < 300,
    [`${operationName}: response < 5s`]: (r) => r.timings.duration < 5000,
    [`${operationName}: has body`]: (r) => r.body && r.body.length > 0,
  });
  
  if (!success) {
    errorRate.add(1);
    failedRequests.add(1);
    if (__ENV.DEBUG === 'true') {
      console.error(`❌ ${operationName} failed: ${res.status} - ${res.body.substring(0, 200)}`);
    }
  } else {
    successfulRequests.add(1);
    if (metricTrend) {
      metricTrend.add(res.timings.duration);
    }
  }
  
  let data = null;
  try {
    if (res.body && res.body.trim().length > 0) {
      data = res.json();
    }
  } catch (e) {
    if (__ENV.DEBUG === 'true') {
      console.warn(`⚠️  Failed to parse JSON for ${operationName}: ${e.message}`);
    }
  }
  
  return { success, data, status: res.status };
}

// ═══════════════════════════════════════════════════════════════
// 📦 OPERAÇÕES DA API
// ═══════════════════════════════════════════════════════════════

// 👤 CRIAR USUÁRIO
function createUser() {
  const payload = JSON.stringify({
    username: `User ${randomString(8)}`,
    email: randomEmail(),
    password: 'test123'
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_user' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/user-ms/api/users`, payload, params);
  const result = handleResponse(res, userCreationTime, 'Create User');
  
  if (result.success && result.data && result.data.id) {
    return result.data.id;
  }
  return null;
}

// 📋 LISTAR USUÁRIOS
function listUsers() {
  const params = {
    tags: { operation: 'list_users' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/user-ms/api/users`, params);
  const result = handleResponse(res, getOperationTime, 'List Users');
  
  return result.success && result.data ? result.data : [];
}

// 👤 BUSCAR USUÁRIO POR ID
function getUserById(userId) {
  if (!userId) return null;
  
  const params = {
    tags: { operation: 'get_user' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/user-ms/api/users/${userId}`, params);
  const result = handleResponse(res, getOperationTime, 'Get User');
  
  return result.success ? result.data : null;
}

// 📝 CRIAR POST
function createPost(userId) {
  if (!userId) return null;
  
  const payload = JSON.stringify({
    user: { id: userId },
    content: `Post sobre microsserviços e performance - ${randomString(50)}. #TCC #SpringBoot #Performance`
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_post' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/post-ms/api/posts`, payload, params);
  const result = handleResponse(res, postCreationTime, 'Create Post');
  
  if (result.success && result.data && result.data.id) {
    return result.data.id;
  }
  return null;
}

// 📖 LISTAR POSTS (FEED)
function listPosts() {
  const params = {
    tags: { operation: 'feed_load' },
    timeout: '15s',
  };
  
  const res = http.get(`${BASE_URL}/post-ms/api/posts`, params);
  const result = handleResponse(res, feedLoadTime, 'List Posts (Feed)');
  
  return result.success && result.data ? result.data : [];
}

// 📖 LISTAR POSTS DE UM USUÁRIO
function listPostsByUser(userId) {
  if (!userId) return [];
  
  const params = {
    tags: { operation: 'list_user_posts' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/post-ms/api/posts/user/${userId}`, params);
  const result = handleResponse(res, getOperationTime, 'List User Posts');
  
  return result.success && result.data ? result.data : [];
}

// 📝 BUSCAR POST POR ID
function getPostById(postId) {
  if (!postId) return null;
  
  const params = {
    tags: { operation: 'get_post' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/post-ms/api/posts/${postId}`, params);
  const result = handleResponse(res, getOperationTime, 'Get Post');
  
  return result.success ? result.data : null;
}

// 💬 CRIAR COMENTÁRIO
function createComment(postId, userId) {
  if (!postId || !userId) return null;
  
  const payload = JSON.stringify({
    postId: postId,
    userId: userId,
    content: `Comentário interessante sobre performance: ${randomString(40)}`
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_comment' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/comment-ms/api/comments`, payload, params);
  const result = handleResponse(res, commentCreationTime, 'Create Comment');
  
  if (result.success && result.data && result.data.id) {
    return result.data.id;
  }
  return null;
}

// 📋 LISTAR COMENTÁRIOS DE UM POST
function listCommentsByPost(postId) {
  if (!postId) return [];
  
  const params = {
    tags: { operation: 'list_comments' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/comment-ms/api/comments/post/${postId}`, params);
  const result = handleResponse(res, getOperationTime, 'List Comments');
  
  return result.success && result.data ? result.data : [];
}

// 👍 DAR LIKE EM POST
function createLikeOnPost(postId, userId) {
  if (!postId || !userId) return null;
  
  const payload = JSON.stringify({
    postId: postId,
    userId: userId,
    commentId: null
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_like' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/like-ms/api/likes`, payload, params);
  handleResponse(res, likeCreationTime, 'Create Like');
}

// 👍 DAR LIKE EM COMENTÁRIO
function createLikeOnComment(commentId, userId) {
  if (!commentId || !userId) return null;
  
  const payload = JSON.stringify({
    postId: null,
    commentId: commentId,
    userId: userId
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_like_comment' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/like-ms/api/likes`, payload, params);
  handleResponse(res, likeCreationTime, 'Create Like Comment');
}

// 📊 LISTAR LIKES
function listLikes() {
  const params = {
    tags: { operation: 'list_likes' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/like-ms/api/likes`, params);
  handleResponse(res, getOperationTime, 'List Likes');
}

// 📊 LISTAR LIKES DE UM POST
function listLikesByPost(postId) {
  if (!postId) return;
  
  const params = {
    tags: { operation: 'list_post_likes' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/like-ms/api/likes/post/${postId}`, params);
  handleResponse(res, getOperationTime, 'List Post Likes');
}

// 🤝 CRIAR AMIZADE
function createFriendship(userId1, userId2) {
  if (!userId1 || !userId2 || userId1 === userId2) return null;
  
  const payload = JSON.stringify({
    userId1: userId1,
    userId2: userId2,
    status: 'ACCEPTED'
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_friendship' },
    timeout: '10s',
  };
  
  const res = http.post(`${BASE_URL}/friendship-ms/api/friendships`, payload, params);
  handleResponse(res, friendshipCreationTime, 'Create Friendship');
}

// 👥 LISTAR AMIGOS DE UM USUÁRIO
function listFriendsByUser(userId) {
  if (!userId) return;
  
  const params = {
    tags: { operation: 'list_friends' },
    timeout: '10s',
  };
  
  const res = http.get(`${BASE_URL}/friendship-ms/api/friendships/user/${userId}`, params);
  handleResponse(res, getOperationTime, 'List Friends');
}

// ═══════════════════════════════════════════════════════════════
// 🎬 CENÁRIO PRINCIPAL - FLUXO COMPLETO DE USUÁRIO
// ═══════════════════════════════════════════════════════════════
export default function() {
  // Obtém lista atual de usuários para simular interações reais
  let availableUsers = [];
  let availablePosts = [];
  
  // 20% chance de carregar lista de usuários/posts existentes
  if (Math.random() < 0.2) {
    const users = listUsers();
    if (Array.isArray(users) && users.length > 0) {
      availableUsers = users.map(u => u.id).filter(id => id != null);
    }
    sleep(0.5);
    
    const posts = listPosts();
    if (Array.isArray(posts) && posts.length > 0) {
      availablePosts = posts.map(p => p.id).filter(id => id != null);
    }
    sleep(0.5);
  }
  
  // ═══════════════════════════════════════════════════════════
  // 🆕 GRUPO 1: Registro e Perfil de Usuário
  // ═══════════════════════════════════════════════════════════
  group('User Registration and Profile', function() {
    // 70% cria novo usuário, 30% usa existente
    let currentUserId;
    
    if (Math.random() < 0.7 || availableUsers.length === 0) {
      currentUserId = createUser();
      sleep(randomInt(1, 2));
      
      if (currentUserId) {
        availableUsers.push(currentUserId);
        
        // Verifica se o usuário foi criado corretamente
        getUserById(currentUserId);
        sleep(0.5);
      }
    } else {
      currentUserId = randomElement(availableUsers);
      if (currentUserId) {
        getUserById(currentUserId);
        sleep(0.5);
      }
    }
    
    // Lista alguns usuários (simula busca/exploração)
    if (Math.random() < 0.3) {
      listUsers();
      sleep(0.5);
    }
  });
  
  // ═══════════════════════════════════════════════════════════
  // 📝 GRUPO 2: Criação de Conteúdo
  // ═══════════════════════════════════════════════════════════
  group('Content Creation', function() {
    let userId = randomElement(availableUsers);
    
    if (!userId) {
      userId = createUser();
      if (userId) availableUsers.push(userId);
      sleep(1);
    }
    
    if (userId) {
      // Cria 1-3 posts
      const numPosts = randomInt(1, 3);
      for (let i = 0; i < numPosts; i++) {
        const postId = createPost(userId);
        if (postId) {
          availablePosts.push(postId);
          sleep(randomInt(1, 2));
          
          // 40% chance de visualizar o post criado
          if (Math.random() < 0.4) {
            getPostById(postId);
            sleep(0.5);
          }
        }
      }
      
      // Visualiza posts do usuário
      if (Math.random() < 0.5) {
        listPostsByUser(userId);
        sleep(0.5);
      }
    }
  });
  
  // ═══════════════════════════════════════════════════════════
  // 📖 GRUPO 3: Consumo de Feed (operação pesada)
  // ═══════════════════════════════════════════════════════════
  group('Feed Browsing', function() {
    // Carrega o feed principal
    listPosts();
    sleep(randomInt(1, 3));
    
    // Explora alguns posts do feed
    if (availablePosts.length > 0) {
      const postsToView = Math.min(randomInt(2, 5), availablePosts.length);
      for (let i = 0; i < postsToView; i++) {
        const postId = randomElement(availablePosts);
        if (postId) {
          getPostById(postId);
          sleep(randomInt(1, 2));
        }
      }
    }
  });
  
  // ═══════════════════════════════════════════════════════════
  // 💬 GRUPO 4: Interações Sociais
  // ═══════════════════════════════════════════════════════════
  group('Social Interactions', function() {
    let userId = randomElement(availableUsers);
    let postId = randomElement(availablePosts);
    
    if (!userId) {
      userId = createUser();
      if (userId) availableUsers.push(userId);
      sleep(1);
    }
    
    if (userId && postId) {
      // 80% da chance criar like no post
      if (Math.random() < 0.8) {
        createLikeOnPost(postId, userId);
        sleep(randomInt(1, 2));
      }
      
      // 60% chance de comentar
      if (Math.random() < 0.6) {
        const commentId = createComment(postId, userId);
        sleep(randomInt(1, 2));
        
        // 50% chance de dar like no próprio comentário (outro usuário)
        if (commentId && Math.random() < 0.5) {
          const otherUserId = randomElement(availableUsers);
          if (otherUserId && otherUserId !== userId) {
            createLikeOnComment(commentId, otherUserId);
            sleep(1);
          }
        }
      }
      
      // Visualiza comentários do post
      if (Math.random() < 0.7) {
        listCommentsByPost(postId);
        sleep(0.5);
      }
      
      // Visualiza likes do post
      if (Math.random() < 0.5) {
        listLikesByPost(postId);
        sleep(0.5);
      }
    }
  });
  
  // ═══════════════════════════════════════════════════════════
  // 🤝 GRUPO 5: Rede de Amizades
  // ═══════════════════════════════════════════════════════════
  group('Social Network', function() {
    if (availableUsers.length >= 2 && Math.random() < 0.5) {
      const user1 = randomElement(availableUsers);
      const user2 = randomElement(availableUsers.filter(id => id !== user1));
      
      if (user1 && user2) {
        createFriendship(user1, user2);
        sleep(randomInt(1, 2));
        
        // Visualiza amigos
        listFriendsByUser(user1);
        sleep(0.5);
      }
    }
  });
  
  // Pausa realista entre iterações
  sleep(randomInt(2, 5));
}

// ═══════════════════════════════════════════════════════════════
// 📖 CENÁRIO ALTERNATIVO - OPERAÇÕES DE LEITURA INTENSIVA
// ═══════════════════════════════════════════════════════════════
export function readOnlyScenario() {
  group('Read-Heavy Operations', function() {
    // Lista usuários
    const users = listUsers();
    sleep(0.3);
    
    // Lista posts (feed)
    const posts = listPosts();
    sleep(0.5);
    
    // Busca detalhes de posts aleatórios
    if (Array.isArray(posts) && posts.length > 0) {
      const postIds = posts.map(p => p.id).filter(id => id != null);
      const postsToRead = Math.min(randomInt(3, 8), postIds.length);
      
      for (let i = 0; i < postsToRead; i++) {
        const postId = randomElement(postIds);
        if (postId) {
          getPostById(postId);
          sleep(0.3);
          
          listCommentsByPost(postId);
          sleep(0.2);
          
          listLikesByPost(postId);
          sleep(0.2);
        }
      }
    }
    
    // Busca perfis de usuários
    if (Array.isArray(users) && users.length > 0) {
      const userIds = users.map(u => u.id).filter(id => id != null);
      const usersToRead = Math.min(randomInt(2, 5), userIds.length);
      
      for (let i = 0; i < usersToRead; i++) {
        const userId = randomElement(userIds);
        if (userId) {
          getUserById(userId);
          sleep(0.2);
          
          listPostsByUser(userId);
          sleep(0.3);
          
          listFriendsByUser(userId);
          sleep(0.2);
        }
      }
    }
  });
  
  sleep(randomInt(1, 3));
}

// ═══════════════════════════════════════════════════════════════
// 📊 SETUP E TEARDOWN
// ═══════════════════════════════════════════════════════════════
export function setup() {
  console.log('');
  console.log('╔════════════════════════════════════════════════════════╗');
  console.log('║  🚀 TCC - TESTE DE CARGA - MICROSSERVIÇOS            ║');
  console.log('╚════════════════════════════════════════════════════════╝');
  console.log('');
  console.log(`📍 URL Base: ${BASE_URL}`);
  console.log('⏱️  Criando dados iniciais para setup...');
  console.log('');
  
  const setupData = {
    initialUsers: [],
    initialPosts: [],
    startTime: new Date().toISOString(),
  };
  
  // Cria usuários iniciais
  console.log('👤 Criando usuários iniciais...');
  for (let i = 0; i < 10; i++) {
    const payload = JSON.stringify({
      username: `Setup User ${i}`,
      email: `setup${i}_${Date.now()}@test.com`,
      password: 'test123'
    });
    
    const res = http.post(`${BASE_URL}/user-ms/api/users`, payload, {
      headers: { 'Content-Type': 'application/json' },
      timeout: '15s',
    });
    
    if (res.status >= 200 && res.status < 300) {
      try {
        const user = res.json();
        if (user && user.id) {
          setupData.initialUsers.push(user.id);
        }
      } catch (e) {
        console.warn(`⚠️  Failed to parse user response: ${e.message}`);
      }
    } else {
      console.warn(`⚠️  Failed to create user ${i}: ${res.status}`);
    }
    sleep(0.5);
  }
  
  console.log(`✅ ${setupData.initialUsers.length} usuários criados`);
  
  // Cria posts iniciais
  if (setupData.initialUsers.length > 0) {
    console.log('📝 Criando posts iniciais...');
    for (let i = 0; i < 20; i++) {
      const userId = setupData.initialUsers[i % setupData.initialUsers.length];
      const payload = JSON.stringify({
        user: { id: userId },
        content: `Initial post ${i} for load testing. This is sample content with hashtags #TCC #Microservices #Performance`
      });
      
      const res = http.post(`${BASE_URL}/post-ms/api/posts`, payload, {
        headers: { 'Content-Type': 'application/json' },
        timeout: '15s',
      });
      
      if (res.status >= 200 && res.status < 300) {
        try {
          const post = res.json();
          if (post && post.id) {
            setupData.initialPosts.push(post.id);
          }
        } catch (e) {
          console.warn(`⚠️  Failed to parse post response: ${e.message}`);
        }
      }
      sleep(0.3);
    }
    
    console.log(`✅ ${setupData.initialPosts.length} posts criados`);
  }
  
  console.log('');
  console.log('✅ Setup concluído! Iniciando testes de carga...');
  console.log('');
  
  return setupData;
}

export function teardown(data) {
  console.log('');
  console.log('═══════════════════════════════════════════════════════');
  console.log('📊 RESUMO FINAL DOS TESTES');
  console.log('═══════════════════════════════════════════════════════');
  console.log(`⏱️  Duração total: ${data.startTime} até ${new Date().toISOString()}`);
  console.log(`👤 Usuários criados no setup: ${data.initialUsers.length}`);
  console.log(`📝 Posts criados no setup: ${data.initialPosts.length}`);
  console.log('');
  console.log('📈 Métricas detalhadas estão disponíveis no relatório JSON');
  console.log('═══════════════════════════════════════════════════════');
  console.log('');
  console.log('✅ Testes concluídos com sucesso!');
  console.log('🎓 Boa sorte com seu TCC!');
  console.log('');
}