import { api } from './client';

export async function importStudentsCsv(file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post('/import/students', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data;
}

export async function importBooksCsv(file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post('/import/books', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data;
}
