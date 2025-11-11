import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

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
const totalRequests = new Counter('total_requests');

// ═══════════════════════════════════════════════════════════════
// ⚙️ CONFIGURAÇÃO DE CARGA
// ═══════════════════════════════════════════════════════════════
export const options = {
  scenarios: {
    // Cenário 1: Carga Constante (Baseline)
    constant_load: {
      executor: 'constant-vus',
      vus: 10,
      duration: '2m',
      startTime: '0s',
      tags: { scenario: 'constant' },
    },
    
    // Cenário 2: Rampa Progressiva (Stress Test)
    ramp_up_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },   // Aquecimento
        { duration: '2m', target: 50 },   // Carga média
        { duration: '2m', target: 100 },  // Pico
        { duration: '1m', target: 150 },  // Estresse
        { duration: '1m', target: 0 },    // Cooldown
      ],
      startTime: '2m',
      tags: { scenario: 'ramp_up' },
    },
    
    // Cenário 3: Spike Test (Picos repentinos)
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '10s', target: 200 },  // Spike!
        { duration: '30s', target: 200 },
        { duration: '10s', target: 10 },
        { duration: '30s', target: 10 },
      ],
      startTime: '9m',
      tags: { scenario: 'spike' },
    },
  },
  
  thresholds: {
    'http_req_duration': ['p(95)<2000', 'p(99)<3000'],
    'http_req_failed': ['rate<0.05'],
    'errors': ['rate<0.1'],
  },
};

// ═══════════════════════════════════════════════════════════════
// 🌐 CONFIGURAÇÃO BASE
// ═══════════════════════════════════════════════════════════════
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8765';

// Arrays para armazenar IDs criados
let userIds = [];
let postIds = [];
let commentIds = [];

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
  return `user_${randomString(8)}@test.com`;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function handleResponse(res, metricTrend, operationName) {
  totalRequests.add(1);
  
  const success = check(res, {
    [`${operationName}: status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
    [`${operationName}: response time < 3s`]: (r) => r.timings.duration < 3000,
  });
  
  if (!success) {
    errorRate.add(1);
    console.error(`❌ ${operationName} failed: ${res.status} - ${res.body}`);
  } else {
    if (metricTrend) {
      metricTrend.add(res.timings.duration);
    }
  }
  
  return { success, data: res.json() };
}

// ═══════════════════════════════════════════════════════════════
// 📦 OPERAÇÕES DA API
// ═══════════════════════════════════════════════════════════════

// 👤 CRIAR USUÁRIO
function createUser() {
  const payload = JSON.stringify({
    name: `User ${randomString(6)}`,
    email: randomEmail(),
    bio: `Bio for user ${randomString(10)}`
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_user' },
  };
  
  const res = http.post(`${BASE_URL}/user-ms/api/users`, payload, params);
  const result = handleResponse(res, userCreationTime, 'Create User');
  
  if (result.success && result.data && result.data.id) {
    userIds.push(result.data.id);
    return result.data.id;
  }
  return null;
}

// 📋 LISTAR USUÁRIOS
function listUsers() {
  const params = {
    tags: { operation: 'list_users' },
  };
  
  const res = http.get(`${BASE_URL}/user-ms/api/users`, params);
  handleResponse(res, null, 'List Users');
}

// 👤 BUSCAR USUÁRIO POR ID
function getUserById(userId) {
  if (!userId) return null;
  
  const params = {
    tags: { operation: 'get_user' },
  };
  
  const res = http.get(`${BASE_URL}/user-ms/api/users/${userId}`, params);
  const result = handleResponse(res, null, 'Get User');
  
  return result.success ? result.data : null;
}

// 📝 CRIAR POST
function createPost(userId) {
  if (!userId) return null;
  
  const payload = JSON.stringify({
    user: { id: userId },
    content: `Post content ${randomString(50)}`
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_post' },
  };
  
  const res = http.post(`${BASE_URL}/post-ms/api/posts`, payload, params);
  const result = handleResponse(res, postCreationTime, 'Create Post');
  
  if (result.success && result.data && result.data.id) {
    postIds.push(result.data.id);
    return result.data.id;
  }
  return null;
}

// 📖 LISTAR POSTS (FEED)
function listPosts() {
  const params = {
    tags: { operation: 'list_posts' },
  };
  
  const res = http.get(`${BASE_URL}/post-ms/api/posts`, params);
  handleResponse(res, feedLoadTime, 'List Posts');
}

// 📖 LISTAR POSTS DE UM USUÁRIO
function listPostsByUser(userId) {
  if (!userId) return;
  
  const params = {
    tags: { operation: 'list_user_posts' },
  };
  
  const res = http.get(`${BASE_URL}/post-ms/api/posts/user/${userId}`, params);
  handleResponse(res, null, 'List User Posts');
}

// 💬 CRIAR COMENTÁRIO
function createComment(postId, userId) {
  if (!postId || !userId) return null;
  
  const payload = JSON.stringify({
    postId: postId,
    userId: userId,
    content: `Comment content ${randomString(30)}`
  });
  
  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { operation: 'create_comment' },
  };
  
  const res = http.post(`${BASE_URL}/comment-ms/api/comments`, payload, params);
  const result = handleResponse(res, commentCreationTime, 'Create Comment');
  
  if (result.success && result.data && result.data.id) {
    commentIds.push(result.data.id);
    return result.data.id;
  }
  return null;
}

// 📋 LISTAR COMENTÁRIOS DE UM POST
function listCommentsByPost(postId) {
  if (!postId) return;
  
  const params = {
    tags: { operation: 'list_comments' },
  };
  
  const res = http.get(`${BASE_URL}/comment-ms/api/comments/post/${postId}`, params);
  handleResponse(res, null, 'List Comments');
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
  };
  
  const res = http.post(`${BASE_URL}/like-ms/api/likes`, payload, params);
  handleResponse(res, likeCreationTime, 'Create Like on Comment');
}

// 📊 LISTAR LIKES
function listLikes() {
  const params = {
    tags: { operation: 'list_likes' },
  };
  
  const res = http.get(`${BASE_URL}/like-ms/api/likes`, params);
  handleResponse(res, null, 'List Likes');
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
  };
  
  const res = http.post(`${BASE_URL}/friendship-ms/api/friendships`, payload, params);
  handleResponse(res, friendshipCreationTime, 'Create Friendship');
}

// 👥 LISTAR AMIGOS DE UM USUÁRIO
function listFriendsByUser(userId) {
  if (!userId) return;
  
  const params = {
    tags: { operation: 'list_friends' },
  };
  
  const res = http.get(`${BASE_URL}/friendship-ms/api/friendships/user/${userId}`, params);
  handleResponse(res, null, 'List Friends');
}

// ═══════════════════════════════════════════════════════════════
// 🎬 CENÁRIO PRINCIPAL
// ═══════════════════════════════════════════════════════════════
export default function() {
  // Simula um usuário completo navegando na rede social
  
  group('User Registration and Profile', function() {
    const userId = createUser();
    sleep(1);
    
    if (userId) {
      listUsers();
      sleep(0.5);
      
      getUserById(userId);
      sleep(0.5);
    }
  });
  
  group('Content Creation', function() {
    let userId = userIds.length > 0 
      ? userIds[randomInt(0, userIds.length - 1)] 
      : createUser();
    
    if (userId) {
      const postId1 = createPost(userId);
      sleep(1);
      
      const postId2 = createPost(userId);
      sleep(1);
      
      listPosts();
      sleep(0.5);
      
      if (postId1) {
        listPostsByUser(userId);
        sleep(0.5);
      }
    }
  });
  
  group('Social Interactions', function() {
    let userId = userIds.length > 0 
      ? userIds[randomInt(0, userIds.length - 1)] 
      : createUser();
    
    let postId = postIds.length > 0 
      ? postIds[randomInt(0, postIds.length - 1)] 
      : null;
    
    if (userId && postId) {
      // Like no post
      createLikeOnPost(postId, userId);
      sleep(0.5);
      
      // Comentar no post
      const commentId = createComment(postId, userId);
      sleep(0.5);
      
      // Like no comentário
      if (commentId) {
        createLikeOnComment(commentId, userId);
        sleep(0.5);
      }
      
      // Lista comentários do post
      listCommentsByPost(postId);
      sleep(0.3);
      
      // Lista todos os likes
      listLikes();
      sleep(0.3);
    }
  });
  
  group('Social Network', function() {
    if (userIds.length >= 2) {
      const user1 = userIds[randomInt(0, userIds.length - 1)];
      const user2 = userIds[randomInt(0, userIds.length - 1)];
      
      if (user1 !== user2) {
        createFriendship(user1, user2);
        sleep(0.5);
        
        listFriendsByUser(user1);
        sleep(0.3);
      }
    }
  });
  
  sleep(randomInt(1, 3));
}

// ═══════════════════════════════════════════════════════════════
// 📊 SETUP E TEARDOWN
// ═══════════════════════════════════════════════════════════════
export function setup() {
  console.log('🚀 Iniciando testes de carga na arquitetura de Microsserviços');
  console.log(`📍 URL Base: ${BASE_URL}`);
  console.log('⏱️  Criando dados iniciais...');
  
  const initialUsers = [];
  for (let i = 0; i < 5; i++) {
    const payload = JSON.stringify({
      name: `Initial User ${i}`,
      email: `initial${i}@test.com`,
      bio: `Bio for initial user ${i}`
    });
    
    const res = http.post(`${BASE_URL}/user-ms/api/users`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (res.status >= 200 && res.status < 300) {
      const user = res.json();
      if (user && user.id) {
        initialUsers.push(user.id);
        userIds.push(user.id);
      }
    }
  }
  
  console.log(`✅ ${initialUsers.length} usuários iniciais criados`);
  return { initialUsers };
}

export function teardown(data) {
  console.log('');
  console.log('═══════════════════════════════════════════');
  console.log('📊 RESUMO DOS TESTES');
  console.log('═══════════════════════════════════════════');
  console.log(`✅ Total de requisições: ${totalRequests}`);
  console.log(`👤 Usuários criados: ${userIds.length}`);
  console.log(`📝 Posts criados: ${postIds.length}`);
  console.log(`💬 Comentários criados: ${commentIds.length}`);
  console.log('═══════════════════════════════════════════');
}