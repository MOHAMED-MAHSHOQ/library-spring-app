function Loader({ text = 'Loading...' }) {
  return (
    <div className="panel loader-box" role="status" aria-live="polite">
      <div className="loader-dot" aria-hidden="true" />
      <p className="muted">{text}</p>
    </div>
  );
}

export default Loader;
