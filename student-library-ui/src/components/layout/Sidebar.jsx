import { BookCopy, FileUp, LayoutDashboard, LogOut, ShieldCheck, Users } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../ui/Button';

function Sidebar() {
  const { user, logout, isAdmin } = useAuth();
  const links = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/students', label: 'Students', icon: Users },
    { to: '/books', label: 'Books', icon: BookCopy },
    { to: '/imports', label: 'CSV Imports', icon: FileUp },
    ...(isAdmin ? [{ to: '/admin/register', label: 'Register Admin', icon: ShieldCheck }] : [])
  ];

  return (
    <aside className="sidebar">
      <div className="brand-block">
        <div className="brand-orb" aria-hidden="true" />
        <div>
          <h2 className="brand-title">Student Library</h2>
          <p className="muted brand-subtitle">Premium Admin Console</p>
        </div>
      </div>

      <nav className="nav-group" aria-label="Primary navigation">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`.trim()}
          >
            <link.icon size={17} className="nav-icon" />
            <span className="hide-on-compact">{link.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="account-chip hide-on-compact">
          <strong>{user?.fullName || 'Library Admin'}</strong>
          <span className="muted">{user?.email || ''}</span>
        </div>
        <Button variant="ghost" size="sm" onClick={logout} className="logout-btn">
          <LogOut size={15} />
          <span className="hide-on-compact">Logout</span>
        </Button>
      </div>
    </aside>
  );
}

export default Sidebar;

