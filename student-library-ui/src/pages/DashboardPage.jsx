import { useQuery } from '@tanstack/react-query';
import { getStudents } from '../api/students';
import { getBooks, getUnassignedBooks } from '../api/books';
import TopBar from '../components/layout/TopBar';
import StatCard from '../components/ui/StatCard';
import Card from '../components/ui/Card';

function DashboardPage() {
  const studentsQuery = useQuery({
    queryKey: ['students-kpi'],
    queryFn: () => getStudents({ page: 0, size: 1 })
  });

  const booksQuery = useQuery({
    queryKey: ['books-kpi'],
    queryFn: () => getBooks({ page: 0, size: 1 })
  });

  const unassignedQuery = useQuery({
    queryKey: ['unassigned-kpi'],
    queryFn: getUnassignedBooks
  });

  const totalStudents = studentsQuery.data?.totalElements ?? 0;
  const totalBooks = booksQuery.data?.totalElements ?? 0;
  const unassignedBooks = unassignedQuery.data?.length ?? 0;
  const assignedBooks = Math.max(totalBooks - unassignedBooks, 0);

  return (
    <div className="page-stack">
      <TopBar title="Dashboard" subtitle="Live overview from your backend services" actions={null} />

      <div className="grid cols-4">
        <StatCard title="Total Students" value={totalStudents} hint="Registered readers" />
        <StatCard title="Total Books" value={totalBooks} hint="Library inventory" />
        <StatCard title="Assigned" value={assignedBooks} hint="Currently borrowed" />
        <StatCard title="On Shelf" value={unassignedBooks} hint="Available now" />
      </div>

      <div className="grid cols-2">
        <Card className="insight-card" elevated>
          <h3>Usage Health</h3>
          <p className="muted">Assignment ratio</p>
          <h2>{totalBooks ? `${Math.round((assignedBooks / totalBooks) * 100)}%` : '0%'}</h2>
        </Card>

        <Card className="insight-card" elevated>
          <h3>System Status</h3>
          <p className="muted status-copy">
            {studentsQuery.isLoading || booksQuery.isLoading || unassignedQuery.isLoading
              ? 'Syncing data across services...'
              : 'All services connected and responding'}
          </p>
        </Card>
      </div>
    </div>
  );
}

export default DashboardPage;
