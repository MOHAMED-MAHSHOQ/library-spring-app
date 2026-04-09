import { Search } from 'lucide-react';

function TopBar({ title, subtitle, actions, search, setSearch, searchPlaceholder = 'Search...' }) {
  return (
    <header className="panel topbar">
      <div className="topbar-copy">
        <h1>{title}</h1>
        {subtitle ? <p className="muted">{subtitle}</p> : null}
      </div>

      <div className="topbar-controls">
        {typeof search === 'string' ? (
          <div className="search-box">
            <Search size={15} className="search-icon" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder={searchPlaceholder}
              aria-label={searchPlaceholder}
            />
          </div>
        ) : null}
        {actions}
      </div>
    </header>
  );
}

export default TopBar;
