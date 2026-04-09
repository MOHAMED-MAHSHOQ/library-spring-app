import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { importBooksCsv, importStudentsCsv } from '../api/imports';
import { getErrorMessage } from '../utils/errors';
import TopBar from '../components/layout/TopBar';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';

function Result({ data }) {
  if (!data) return null;
  return (
    <div className="import-result">
      <p className="muted">
        Rows: {data.totalRows} | Imported: {data.imported} | Skipped: {data.skipped} | Failed: {data.failed}
      </p>
      {Array.isArray(data.errors) && data.errors.length > 0 ? (
        <ul>
          {data.errors.slice(0, 8).map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function ImportsPage() {
  const [studentFile, setStudentFile] = useState(null);
  const [bookFile, setBookFile] = useState(null);
  const [error, setError] = useState('');

  const studentsMutation = useMutation({
    mutationFn: importStudentsCsv,
    onError: (err) => setError(getErrorMessage(err))
  });

  const booksMutation = useMutation({
    mutationFn: importBooksCsv,
    onError: (err) => setError(getErrorMessage(err))
  });

  return (
    <div className="page-stack">
      <TopBar title="CSV Imports" subtitle="Bulk import students and books" actions={null} />
      {error ? <div className="notice error notice-spaced">{error}</div> : null}

      <div className="grid cols-2">
        <Card className="import-card" elevated>
          <h3>Import Students</h3>
          <p className="muted">CSV columns should match student name, email, phone and department fields.</p>
          <input type="file" accept=".csv" onChange={(event) => setStudentFile(event.target.files?.[0] || null)} />
          <div className="actions-row">
            <Button
              onClick={() => studentFile && studentsMutation.mutate(studentFile)}
              loading={studentsMutation.isPending}
              disabled={!studentFile || studentsMutation.isPending}
            >
              {studentsMutation.isPending ? 'Uploading...' : 'Upload Students CSV'}
            </Button>
          </div>
          <Result data={studentsMutation.data} />
        </Card>

        <Card className="import-card" elevated>
          <h3>Import Books</h3>
          <p className="muted">CSV columns should match title, author, genre and ISBN values.</p>
          <input type="file" accept=".csv" onChange={(event) => setBookFile(event.target.files?.[0] || null)} />
          <div className="actions-row">
            <Button
              onClick={() => bookFile && booksMutation.mutate(bookFile)}
              loading={booksMutation.isPending}
              disabled={!bookFile || booksMutation.isPending}
            >
              {booksMutation.isPending ? 'Uploading...' : 'Upload Books CSV'}
            </Button>
          </div>
          <Result data={booksMutation.data} />
        </Card>
      </div>
    </div>
  );
}

export default ImportsPage;
