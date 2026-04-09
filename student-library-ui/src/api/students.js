import { api } from './client';

export async function getStudents(params) {
  const { data } = await api.get('/students', { params });
  return data;
}

export async function searchStudents(query, page = 0, size = 20) {
  const { data } = await api.get('/students/search', { params: { query, page, size } });
  return data;
}

export async function createStudent(payload) {
  const { data } = await api.post('/students', payload);
  return data;
}

export async function updateStudent(id, payload) {
  const { data } = await api.put(`/students/${id}`, payload);
  return data;
}

export async function deleteStudent(id) {
  await api.delete(`/students/${id}`);
}

export async function assignBook(studentId, bookId) {
  const { data } = await api.post(`/students/${studentId}/books/${bookId}`);
  return data;
}

export async function removeBook(studentId, bookId) {
  const { data } = await api.delete(`/students/${studentId}/books/${bookId}`);
  return data;
}
