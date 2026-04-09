function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  className = '',
  children,
  disabled,
  ...props
}) {
  const classes = ['btn', variant, `size-${size}`, className].filter(Boolean).join(' ');

  return (
    <button className={classes} disabled={disabled || loading} {...props}>
      {loading ? <span className="btn-spinner" aria-hidden="true" /> : null}
      <span>{children}</span>
    </button>
  );
}

export default Button;
