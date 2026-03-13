/**
 * Простой backend с проверкой API_TOKEN.
 * Токен берётся из backend/.env или из переменной окружения API_TOKEN.
 * Запуск: npm start
 */

require('dotenv').config();
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3001;
const API_TOKEN = process.env.API_TOKEN;
const USERS_FILE = path.join(__dirname, 'users.json');

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', chunk => {
      data += chunk;
      if (data.length > 1e6) {
        // simple protection from very large bodies
        req.connection.destroy();
        reject(new Error('Body too large'));
      }
    });
    req.on('end', () => {
      if (!data) return resolve({});
      try {
        const json = JSON.parse(data);
        resolve(json);
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', reject);
  });
}

function loadUsers() {
  try {
    const raw = fs.readFileSync(USERS_FILE, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    return [];
  }
}

function saveUsers(users) {
  fs.writeFileSync(USERS_FILE, JSON.stringify(users, null, 2), 'utf8');
}

const server = http.createServer(async (req, res) => {
  const auth = req.headers.authorization;
  const token = auth && auth.startsWith('Bearer ') ? auth.slice(7) : null;

  if (API_TOKEN && token !== API_TOKEN) {
    res.writeHead(401, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Invalid or missing API token' }));
    return;
  }

  // CORS для удобства разработки
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.url === '/health' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      ok: true,
      message: 'Backend running',
      tokenSet: !!API_TOKEN
    }));
    return;
  }

  if (req.url.startsWith('/users') && req.method === 'GET') {
    const urlObj = new URL(req.url, `http://localhost:${PORT}`);
    const q = (urlObj.searchParams.get('search') || '').trim().toLowerCase();
    let users = loadUsers();
    if (q) {
      users = users.filter(u =>
        (u.name && u.name.toLowerCase().includes(q)) ||
        (u.phone && u.phone.toLowerCase().includes(q))
      );
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ users }));
    return;
  }

  if (req.url === '/help-requests' && req.method === 'POST') {
    try {
      const body = await readJsonBody(req);
      const fromPhone = (body.fromPhone || '').trim();
      const note = (body.note || '').trim();
      if (!fromPhone) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'fromPhone is required' }));
        return;
      }
      const payload = {
        id: Date.now().toString(),
        fromPhone,
        note,
        createdAt: new Date().toISOString()
      };
      // Пока просто логируем. Можно расширить до хранения / уведомлений.
      console.log('HELP REQUEST:', payload);
      res.writeHead(201, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ request: payload }));
    } catch (e) {
      console.error('Help request error', e);
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid JSON body' }));
    }
    return;
  }

  if (req.url === '/users/register' && req.method === 'POST') {
    try {
      const body = await readJsonBody(req);
      const name = (body.name || '').trim();
      const phone = (body.phone || '').trim();

      if (!name || !phone) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Name and phone are required' }));
        return;
      }

      const users = loadUsers();
      if (users.some(u => u.phone === phone)) {
        res.writeHead(409, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'User with this phone already exists' }));
        return;
      }

      const user = {
        id: Date.now().toString(),
        name,
        phone
      };
      users.push(user);
      saveUsers(users);

      res.writeHead(201, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ user }));
    } catch (e) {
      console.error('Register error', e);
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid JSON body' }));
    }
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, () => {
  console.log(`Backend listening on http://localhost:${PORT}`);
  if (!API_TOKEN) console.warn('Warning: API_TOKEN is not set');
});
