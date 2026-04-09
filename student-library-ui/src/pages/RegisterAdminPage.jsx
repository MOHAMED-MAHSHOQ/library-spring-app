import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { registerAdmin } from '../api/auth';
import { getErrorMessage } from '../utils/errors';
import TopBar from '../components/layout/TopBar';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';

const initialForm = {
  fullName: '',
  email: '',
  password: ''
};

function RegisterAdminPage() {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const mutation = useMutation({
    mutationFn: registerAdmin,
    onSuccess: (result) => {
      setError('');
      setSuccess(`Admin account created for ${result.email}.`);
      setForm(initialForm);
    },
    onError: (err) => {
      setSuccess('');
      setError(getErrorMessage(err));
    }
  });

  const onSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    mutation.mutate(form);
  };

  return (
    <div className="page-stack">
      <TopBar
        title="Register Admin"
        subtitle="Create privileged admin users through secure backend endpoint"
        actions={null}
      />

      {error ? <div className="notice error notice-spaced">{error}</div> : null}
      {success ? <div className="notice success notice-spaced">{success}</div> : null}

      <Card className="admin-register-card" elevated>
        <h3>Create New Admin</h3>
        <p className="muted">
          {/*This action calls <code>/api/v1/auth/register-admin</code> and requires an authenticated admin token.*/}
        </p>

        <form className="form-grid" onSubmit={onSubmit}>
          <Input
            id="admin-fullName"
            label="Full Name"
            value={form.fullName}
            onChange={(event) => setForm((prev) => ({ ...prev, fullName: event.target.value }))}
            required
          />
          <Input
            id="admin-email"
            label="Email"
            type="email"
            value={form.email}
            onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            required
          />
          <Input
            id="admin-password"
            label="Password"
            type="password"
            minLength={8}
            description="Minimum 8 characters"
            value={form.password}
            onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            required
          />
          <div className="actions-row">
            <Button type="submit" loading={mutation.isPending}>
              {mutation.isPending ? 'Creating admin...' : 'Create Admin'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

export default RegisterAdminPage;
