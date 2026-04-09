export function getErrorMessage(error) {
  const data = error?.response?.data;
  if (!data) return error?.message || 'Something went wrong.';

  if (typeof data.message === 'string') return data.message;

  if (data.messages && typeof data.messages === 'object') {
    return Object.values(data.messages).join(' | ');
  }

  return 'Request failed. Please try again.';
}

