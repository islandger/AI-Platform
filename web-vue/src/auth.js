export function saveToken(token) { localStorage.setItem('auth_token', token); }
export function getToken() { return localStorage.getItem('auth_token'); }
export function clearToken() { localStorage.removeItem('auth_token'); }

export async function login(username, password) {
  const res = await fetch('/api/auth/login', { 
    method: 'POST', 
    headers: { 'Content-Type': 'application/json' }, 
    body: JSON.stringify({ username, password }) 
  });
  return res.json();
}

export async function register(username, password, display_name) {
  const res = await fetch('/api/auth/register', { 
    method: 'POST', 
    headers: { 'Content-Type': 'application/json' }, 
    body: JSON.stringify({ username, password, display_name }) 
  });
  return res.json();
}

export async function authFetch(path, opts = {}) {
  const token = getToken();
  if (!token) {
    return { ok: false, status: 401, json: () => Promise.resolve({ error: 'No token provided' }) };
  }
  const headers = opts.headers || {};
  headers['Authorization'] = 'Bearer ' + token;
  opts.headers = headers;
  return fetch(path, opts);
}

export async function logout() { clearToken(); }

export async function fetchCurrentUser() {
  const token = getToken(); if (!token) return null;
  try {
    const res = await fetch('/api/auth/me', { 
      method: 'GET',
      headers: { 'Authorization': 'Bearer ' + token }
    });
    if (res.ok) return await res.json();
    return null;
  } catch (e) { 
    console.error('Failed to fetch current user:', e);
    return null;
  }
}