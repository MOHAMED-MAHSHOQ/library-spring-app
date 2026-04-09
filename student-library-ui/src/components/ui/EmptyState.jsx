function EmptyState({ title, description }) {
  return (
    <div className="panel empty-state">
      <div className="empty-state-icon" aria-hidden="true">
        *
      </div>
      <h3>{title}</h3>
      <p className="muted">{description}</p>
    </div>
  );
}

export default EmptyState;
