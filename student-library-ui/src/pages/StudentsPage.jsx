import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  assignBook,
  createStudent,
  deleteStudent,
  getStudents,
  removeBook,
  searchStudents,
  updateStudent
} from '../api/students';
import { getUnassignedBooks } from '../api/books';
import { getErrorMessage } from '../utils/errors';
import { useAuth } from '../contexts/AuthContext';
import TopBar from '../components/layout/TopBar';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import Loader from '../components/ui/Loader';
import Modal from '../components/ui/Modal';
import Input from '../components/ui/Input';
import Badge from '../components/ui/Badge';
import Pagination from '../components/ui/Pagination';

const emptyForm = { name: '', email: '', phone: '', department: '' };

function StudentsPage() {
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [sortBy] = useState('id');
  const [sortDir] = useState('asc');

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [studentModalOpen, setStudentModalOpen] = useState(false);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [editingStudent, setEditingStudent] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [selectedBookId, setSelectedBookId] = useState('');
  const [bookSearch, setBookSearch] = useState('');
  const [form, setForm] = useState(emptyForm);

  const queryKey = ['students', search, page, size, sortBy, sortDir];
  const studentsQuery = useQuery({
    queryKey,
    queryFn: () => {
      if (search.trim()) return searchStudents(search.trim(), page, size);
      return getStudents({ page, size, sortBy, sortDir });
    }
  });

  const unassignedQuery = useQuery({
    queryKey: ['unassigned-books'],
    queryFn: getUnassignedBooks
  });

  const students = useMemo(() => studentsQuery.data?.content || [], [studentsQuery.data]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['students'] });
    queryClient.invalidateQueries({ queryKey: ['books'] });
    queryClient.invalidateQueries({ queryKey: ['unassigned-books'] });
    queryClient.invalidateQueries({ queryKey: ['students-kpi'] });
    queryClient.invalidateQueries({ queryKey: ['books-kpi'] });
    queryClient.invalidateQueries({ queryKey: ['unassigned-kpi'] });
  };

  const createMutation = useMutation({
    mutationFn: createStudent,
    onSuccess: () => {
      setSuccess('Student created successfully.');
      setStudentModalOpen(false);
      setForm(emptyForm);
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateStudent(id, payload),
    onSuccess: () => {
      setSuccess('Student updated successfully.');
      setStudentModalOpen(false);
      setEditingStudent(null);
      setForm(emptyForm);
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const deleteMutation = useMutation({
    mutationFn: deleteStudent,
    onSuccess: () => {
      setSuccess('Student deleted.');
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const assignMutation = useMutation({
    mutationFn: ({ studentId, bookId }) => assignBook(studentId, bookId),
    onSuccess: () => {
      setSuccess('Book assigned successfully.');
      setAssignModalOpen(false);
      setSelectedBookId('');
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const removeMutation = useMutation({
    mutationFn: ({ studentId, bookId }) => removeBook(studentId, bookId),
    onSuccess: () => {
      setSuccess('Book removed from student.');
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err))
  });

  const openCreate = () => {
    setError('');
    setSuccess('');
    setEditingStudent(null);
    setForm(emptyForm);
    setStudentModalOpen(true);
  };

  const openEdit = (student) => {
    setError('');
    setSuccess('');
    setEditingStudent(student);
    setForm({
      name: student.name,
      email: student.email,
      phone: student.phone,
      department: student.department
    });
    setStudentModalOpen(true);
  };

  const openAssign = (student) => {
    setError('');
    setSuccess('');
    setSelectedStudent(student);
    setSelectedBookId('');
    setBookSearch('');
    setAssignModalOpen(true);
  };

  const onSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (editingStudent) {
      updateMutation.mutate({ id: editingStudent.id, payload: form });
      return;
    }
    createMutation.mutate(form);
  };

  return (
    <div className="page-stack">
      <TopBar
        title="Students"
        subtitle="Manage readers, borrowing and book assignments"
        search={search}
        setSearch={(value) => {
          setSearch(value);
          setPage(0);
        }}
        searchPlaceholder="Search students by name, email, phone..."
        actions={isAdmin ? <Button onClick={openCreate}>+ New Student</Button> : null}
      />

      {error ? <div className="notice error notice-spaced">{error}</div> : null}
      {success ? <div className="notice success notice-spaced">{success}</div> : null}

      {studentsQuery.isLoading ? <Loader text="Fetching students..." /> : null}
      {!studentsQuery.isLoading && students.length === 0 ? (
        <EmptyState title="No students found" description="Try another search keyword or create your first student." />
      ) : null}

      {students.length > 0 ? (
        <Card className="table-card" elevated>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Department</th>
                  <th>Books</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr key={student.id}>
                    <td>{student.id}</td>
                    <td>{student.name}</td>
                    <td>{student.email}</td>
                    <td>{student.phone}</td>
                    <td>{student.department}</td>
                    <td>
                      {student.books?.length ? (
                        <div className="chip-wrap">
                          {student.books.map((book) => (
                            <span key={book.id} className="badge">
                              {book.title}
                              {isAdmin ? (
                                <button
                                  className="chip-dismiss"
                                  onClick={() => removeMutation.mutate({ studentId: student.id, bookId: book.id })}
                                  title="Remove book"
                                >
                                  x
                                </button>
                              ) : null}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <Badge tone="warn">No books</Badge>
                      )}
                    </td>
                    <td>
                      <div className="action-inline">
                        {isAdmin ? (
                          <>
                            <Button variant="secondary" size="sm" onClick={() => openAssign(student)}>
                              Assign
                            </Button>
                            <Button variant="secondary" size="sm" onClick={() => openEdit(student)}>
                              Edit
                            </Button>
                            <Button
                              variant="danger"
                              size="sm"
                              onClick={() => deleteMutation.mutate(student.id)}
                              disabled={student.books?.length > 0 || deleteMutation.isPending}
                              title={student.books?.length > 0 ? "Cannot delete student with assigned books" : ""}
                            >
                              Delete
                            </Button>
                          </>
                        ) : (
                          <Badge>Read Only</Badge>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={studentsQuery.data?.page || 0}
            totalPages={studentsQuery.data?.totalPages || 1}
            onPageChange={setPage}
          />
        </Card>
      ) : null}

      <Modal open={studentModalOpen} onClose={() => setStudentModalOpen(false)} title={editingStudent ? 'Edit Student' : 'Create Student'}>
        <form className="form-grid" onSubmit={onSubmit}>
          <Input
            label="Name"
            value={form.name}
            onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
            required
          />
          <Input
            label="Email"
            type="email"
            value={form.email}
            onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            required
          />
          <Input
            label="Phone"
            value={form.phone}
            onChange={(event) => setForm((prev) => ({ ...prev, phone: event.target.value }))}
            required
          />
          <Input
            label="Department"
            value={form.department}
            onChange={(event) => setForm((prev) => ({ ...prev, department: event.target.value }))}
            required
          />
          <div className="actions-row">
            <Button type="button" variant="ghost" onClick={() => setStudentModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={createMutation.isPending || updateMutation.isPending}>
              {editingStudent ? 'Update Student' : 'Create Student'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={assignModalOpen} onClose={() => setAssignModalOpen(false)} title={`Assign Book: ${selectedStudent?.name || ''}`}>
        <div className="form-grid">
          <Input
            id="book-search"
            label="Search Book"
            placeholder="Type book title or author..."
            value={bookSearch}
            onChange={(e) => setBookSearch(e.target.value)}
          />
          <div className="field">
            <label htmlFor="book-picker">Select Book</label>
            <select
              id="book-picker"
              value={selectedBookId}
              onChange={(event) => setSelectedBookId(event.target.value)}
              size={6}
              style={{ padding: '8px' }}
            >
              <option value="">-- Choose a book --</option>
              {(unassignedQuery.data || [])
                .filter(b => b.title.toLowerCase().includes(bookSearch.toLowerCase()) || b.author.toLowerCase().includes(bookSearch.toLowerCase()))
                .map((book) => (
                <option key={book.id} value={book.id} style={{ padding: '4px 8px' }}>
                  {book.title} - {book.author}
                </option>
              ))}
            </select>
          </div>
          <div className="actions-row">
            <Button type="button" variant="ghost" onClick={() => setAssignModalOpen(false)}>
              Cancel
            </Button>
            <Button
              loading={assignMutation.isPending}
              disabled={!selectedBookId || assignMutation.isPending}
              onClick={() =>
                selectedStudent &&
                selectedBookId &&
                assignMutation.mutate({ studentId: selectedStudent.id, bookId: Number(selectedBookId) })
              }
            >
              Assign Book
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default StudentsPage;

