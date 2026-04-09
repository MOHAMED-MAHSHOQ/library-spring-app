import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createBook, deleteBook, getBooks, searchBooks, updateBook } from '../api/books';
import { getErrorMessage } from '../utils/errors';
import { useAuth } from '../contexts/AuthContext';
import TopBar from '../components/layout/TopBar';
import Card from '../components/ui/Card';
import Modal from '../components/ui/Modal';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';
import Loader from '../components/ui/Loader';
import Pagination from '../components/ui/Pagination';

const emptyForm = {
  title: '',
  author: '',
  genre: '',
  isbn: ''
};

function BooksPage() {
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [sortBy] = useState('id');
  const [sortDir] = useState('asc');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [form, setForm] = useState(emptyForm);
  const [editingBook, setEditingBook] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  const booksQuery = useQuery({
    queryKey: ['books', search, page, size, sortBy, sortDir],
    queryFn: () => {
      if (search.trim()) return searchBooks(search.trim(), page, size);
      return getBooks({ page, size, sortBy, sortDir });
    }
  });

  const books = useMemo(() => booksQuery.data?.content || [], [booksQuery.data]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['books'] });
    queryClient.invalidateQueries({ queryKey: ['students'] });
    queryClient.invalidateQueries({ queryKey: ['unassigned-books'] });
    queryClient.invalidateQueries({ queryKey: ['books-kpi'] });
    queryClient.invalidateQueries({ queryKey: ['unassigned-kpi'] });
  };

  const createMutation = useMutation({
    mutationFn: createBook,
    onSuccess: () => {
      setSuccess('Book created successfully.');
      setModalOpen(false);
      setForm(emptyForm);
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateBook(id, payload),
    onSuccess: () => {
      setSuccess('Book updated successfully.');
      setModalOpen(false);
      setEditingBook(null);
      setForm(emptyForm);
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const deleteMutation = useMutation({
    mutationFn: deleteBook,
    onSuccess: () => {
      setSuccess('Book deleted.');
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const openCreate = () => {
    setError('');
    setSuccess('');
    setEditingBook(null);
    setForm(emptyForm);
    setModalOpen(true);
  };

  const openEdit = (book) => {
    setError('');
    setSuccess('');
    setEditingBook(book);
    setForm({
      title: book.title,
      author: book.author,
      genre: book.genre,
      isbn: book.isbn
    });
    setModalOpen(true);
  };

  const onSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (editingBook) {
      updateMutation.mutate({ id: editingBook.id, payload: form });
      return;
    }
    createMutation.mutate(form);
  };

  return (
    <div className="page-stack">
      <TopBar
        title="Books"
        subtitle="Manage catalog and assignment status"
        search={search}
        setSearch={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search books by title, author, genre or ISBN..."
        actions={isAdmin ? <Button onClick={openCreate}>+ New Book</Button> : null}
      />

      {error ? <div className="notice error notice-spaced">{error}</div> : null}
      {success ? <div className="notice success notice-spaced">{success}</div> : null}

      {booksQuery.isLoading ? <Loader text="Fetching books..." /> : null}
      {!booksQuery.isLoading && books.length === 0 ? (
        <EmptyState title="No books found" description="Try another search query or add your first book." />
      ) : null}

      {books.length > 0 ? (
        <Card className="table-card" elevated>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Author</th>
                  <th>Genre</th>
                  <th>ISBN</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {books.map((book) => (
                  <tr key={book.id}>
                    <td>{book.id}</td>
                    <td>{book.title}</td>
                    <td>{book.author}</td>
                    <td>{book.genre}</td>
                    <td>{book.isbn}</td>
                    <td>
                      {book.studentId ? (
                        <Badge tone="success">Assigned to {book.studentName || `#${book.studentId}`}</Badge>
                      ) : (
                        <Badge tone="warn">On shelf</Badge>
                      )}
                    </td>
                    <td>
                      {isAdmin ? (
                        <div className="action-inline">
                          <Button variant="secondary" size="sm" onClick={() => openEdit(book)}>
                            Edit
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => deleteMutation.mutate(book.id)}
                            disabled={!!book.studentId || deleteMutation.isPending}
                            title={book.studentId ? "Cannot delete an assigned book" : ""}
                          >
                            Delete
                          </Button>
                        </div>
                      ) : (
                        <Badge>Read Only</Badge>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={booksQuery.data?.page || 0} totalPages={booksQuery.data?.totalPages || 1} onPageChange={setPage} />
        </Card>
      ) : null}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingBook ? 'Edit Book' : 'Create Book'}>
        <form className="form-grid" onSubmit={onSubmit}>
          <Input
            label="Title"
            value={form.title}
            onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
            required
          />
          <Input
            label="Author"
            value={form.author}
            onChange={(event) => setForm((prev) => ({ ...prev, author: event.target.value }))}
            required
          />
          <Input
            label="Genre"
            value={form.genre}
            onChange={(event) => setForm((prev) => ({ ...prev, genre: event.target.value }))}
            required
          />
          <Input
            label="ISBN"
            placeholder="ISBN-001"
            value={form.isbn}
            onChange={(event) => setForm((prev) => ({ ...prev, isbn: event.target.value }))}
            required
          />
          <div className="actions-row">
            <Button type="button" variant="ghost" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={createMutation.isPending || updateMutation.isPending}>
              {editingBook ? 'Update Book' : 'Create Book'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default BooksPage;

