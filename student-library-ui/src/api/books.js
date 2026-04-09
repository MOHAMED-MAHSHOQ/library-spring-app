import { api } from './client';

export async function getBooks(params) {
  const { data } = await api.get('/books', { params });
  return data;
}

export async function searchBooks(query, page = 0, size = 20) {
  const { data } = await api.get('/books/search', { params: { query, page, size } });
  return data;
}

export async function getUnassignedBooks() {
  const { data } = await api.get('/books/unassigned');
  return data;
}

export async function createBook(payload) {
  const { data } = await api.post('/books', payload);
  return data;
}

export async function updateBook(id, payload) {
  const { data } = await api.put(`/books/${id}`, payload);
  return data;
}

export async function deleteBook(id) {
  await api.delete(`/books/${id}`);
}
