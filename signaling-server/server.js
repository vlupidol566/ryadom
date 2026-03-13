/**
 * WebRTC Signaling Server для приложения «Роман Александров».
 * Запуск: на удалённом компьютере — node server.js
 * Телефоны подключаются по ws://IP_СЕРВЕРА:9090 или wss:// если настроен HTTPS.
 */

const WebSocket = require('ws');

const PORT = process.env.PORT || 9090;
const server = new WebSocket.Server({ port: PORT });

// Несколько ведущих и один текущий активный ведущий для WebRTC
const leaders = new Set();
let activeLeader = null;  // ведущий, с которым сейчас установлена WebRTC-сессия
let follower = null; // ведомый

function send(to, message) {
  if (to && to.readyState === WebSocket.OPEN) {
    const text = typeof message === 'string' ? message : JSON.stringify(message);
    to.send(text);
  }
}

function broadcastSignaling(from, message) {
  // Для offer/answer/ice оставляем связь один-ко-одному:
  // follower <-> activeLeader
  if (from === follower) {
    if (activeLeader) send(activeLeader, message);
  } else if (leaders.has(from)) {
    if (follower) send(follower, message);
  }
}

server.on('connection', (ws, req) => {
  const addr = req.socket.remoteAddress;
  console.log('Подключение:', addr);

  ws.on('message', (data) => {
    let msg;
    try {
      msg = typeof data === 'string' ? JSON.parse(data) : JSON.parse(data.toString());
    } catch (e) {
      console.log('Не JSON:', data?.toString?.()?.slice(0, 80));
      return;
    }

    if (msg.type === 'role') {
      if (msg.role === 'leader') {
        leaders.add(ws);
        if (!activeLeader) activeLeader = ws;
        console.log('Ведущий подключен:', addr, 'всего ведущих:', leaders.size);
        send(ws, { type: 'role_ok', role: 'leader' });
      } else if (msg.role === 'follower') {
        if (follower) follower.close();
        follower = ws;
        console.log('Ведомый назначен:', addr);
        send(follower, { type: 'role_ok', role: 'follower' });
      }
      return;
    }

    if (msg.type === 'help_request') {
      // Рассылаем всем ведущим уведомление о запросе помощи
      for (const leader of leaders) {
        send(leader, {
          type: 'help_request',
          fromPhone: msg.fromPhone,
          note: msg.note
        });
      }
      return;
    }

    // Пересылаем offer, answer, ice между активным ведущим и ведомым
    broadcastSignaling(ws, msg);
  });

  ws.on('close', () => {
    if (leaders.has(ws)) {
      leaders.delete(ws);
      if (activeLeader === ws) {
        activeLeader = leaders.values().next().value || null;
      }
    }
    if (ws === follower) follower = null;
    console.log('Отключение:', addr);
  });

  ws.on('error', (err) => {
    console.log('Ошибка:', err.message);
  });
});

server.on('listening', () => {
  console.log('Сервер сигналинга слушает порт', PORT);
  console.log('Подключение: ws://<IP_ЭТОГО_КОМПЬЮТЕРА>:' + PORT);
});
