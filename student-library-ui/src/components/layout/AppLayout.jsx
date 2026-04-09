import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

function AppLayout() {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">
        <div className="content-shell">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

export default AppLayout;
