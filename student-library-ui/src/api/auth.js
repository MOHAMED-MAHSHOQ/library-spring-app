import { api } from './client';

export async function login(payload) {
  const { data } = await api.post('/auth/login', payload);
  return data;
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload);
  return data;
}

export async function registerAdmin(payload) {
  const { data } = await api.post('/auth/register-admin', payload);
  return data;
}
