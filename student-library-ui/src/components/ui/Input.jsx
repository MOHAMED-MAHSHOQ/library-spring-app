function Input({ label, id, error, description, className = '', ...props }) {
  return (
    <div className={`field ${className}`.trim()}>
      {label ? <label htmlFor={id}>{label}</label> : null}
      <input id={id} {...props} />
      {description ? <small className="field-description">{description}</small> : null}
      {error ? <small className="field-error">{error}</small> : null}
    </div>
  );
}

export default Input;
