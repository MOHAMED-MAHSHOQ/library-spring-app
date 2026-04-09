import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { register as registerApi } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage } from '../utils/errors';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';

function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [mode, setMode] = useState('login');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [registerForm, setRegisterForm] = useState({
    fullName: '',
    email: '',
    password: ''
  });

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: () => navigate('/dashboard'),
    onError: (err) => setError(getErrorMessage(err))
  });

  const registerMutation = useMutation({
    mutationFn: registerApi,
    onSuccess: () => {
      setSuccess('Registration completed. You can now sign in.');
      setMode('login');
      setRegisterForm({ fullName: '', email: '', password: '' });
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const onLoginSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    loginMutation.mutate(loginForm);
  };

  const onRegisterSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    registerMutation.mutate(registerForm);
  };

  return (
    <div className="login-wrap">
      <div className="login-grid" style={{ gridTemplateColumns: '1fr', maxWidth: '440px' }}>
        <Card className="login-card" elevated>
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <h1 style={{ marginTop: 0, marginBottom: '8px', fontSize: '1.75rem' }}>Student Library System</h1>
            <p className="muted" style={{ margin: 0 }}>Enter your credentials to access the admin portal.</p>
          </div>

          <div className="segment-control" role="tablist" aria-label="Authentication options">
            <Button
              variant={mode === 'login' ? 'primary' : 'ghost'}
              size="sm"
              style={{ flex: 1 }}
              onClick={() => setMode('login')}
              aria-pressed={mode === 'login'}
            >
              Sign In
            </Button>
            <Button
              variant={mode === 'register' ? 'primary' : 'ghost'}
              size="sm"
              style={{ flex: 1 }}
              onClick={() => setMode('register')}
              aria-pressed={mode === 'register'}
            >
              Register
            </Button>
          </div>

          {error ? <div className="notice error notice-spaced">{error}</div> : null}
          {success ? <div className="notice success notice-spaced">{success}</div> : null}

          {mode === 'login' ? (
            <form className="form-grid" onSubmit={onLoginSubmit}>
              <div className="field">
                <label htmlFor="login-email">Email</label>
                <input
                  id="login-email"
                  type="email"
                  placeholder="admin@library.com"
                  value={loginForm.email}
                  onChange={(event) => setLoginForm((prev) => ({ ...prev, email: event.target.value }))}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="login-password">Password</label>
                <input
                  id="login-password"
                  type="password"
                  value={loginForm.password}
                  onChange={(event) => setLoginForm((prev) => ({ ...prev, password: event.target.value }))}
                  required
                />
              </div>
              <Button type="submit" style={{ marginTop: '12px' }} loading={loginMutation.isPending}>
                {loginMutation.isPending ? 'Authenticating...' : 'Sign In'}
              </Button>
            </form>
          ) : (
            <form className="form-grid" onSubmit={onRegisterSubmit}>
              <div className="field">
                <label htmlFor="register-name">Full Name</label>
                <input
                  id="register-name"
                  value={registerForm.fullName}
                  onChange={(event) => setRegisterForm((prev) => ({ ...prev, fullName: event.target.value }))}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="register-email">Email</label>
                <input
                  id="register-email"
                  type="email"
                  value={registerForm.email}
                  onChange={(event) => setRegisterForm((prev) => ({ ...prev, email: event.target.value }))}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="register-password">Password</label>
                <input
                  id="register-password"
                  type="password"
                  minLength={8}
                  value={registerForm.password}
                  onChange={(event) => setRegisterForm((prev) => ({ ...prev, password: event.target.value }))}
                  required
                />
                <span className="field-description">Minimum 8 characters</span>
              </div>
              <Button type="submit" style={{ marginTop: '12px' }} loading={registerMutation.isPending}>
                {registerMutation.isPending ? 'Processing...' : 'Create Account'}
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}

export default LoginPage;
