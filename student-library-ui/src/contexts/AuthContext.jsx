import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { login as loginApi } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('sl_token'));
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('sl_user');
    return stored ? JSON.parse(stored) : null;
  });

  useEffect(() => {
    const onUnauthorized = () => logout();
    window.addEventListener('sl-unauthorized', onUnauthorized);
    return () => window.removeEventListener('sl-unauthorized', onUnauthorized);
  }, []);

  const login = async (credentials) => {
    const result = await loginApi(credentials);
    const userData = {
      email: result.email,
      fullName: result.fullName,
      role: result.role,
      expiresIn: result.expiresIn
    };

    localStorage.setItem('sl_token', result.token);
    localStorage.setItem('sl_user', JSON.stringify(userData));
    setToken(result.token);
    setUser(userData);
    return result;
  };

  const logout = () => {
    localStorage.removeItem('sl_token');
    localStorage.removeItem('sl_user');
    setToken(null);
    setUser(null);
  };

  const value = useMemo(
    () => ({ token, user, login, logout, isAdmin: user?.role === 'ADMIN' }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
