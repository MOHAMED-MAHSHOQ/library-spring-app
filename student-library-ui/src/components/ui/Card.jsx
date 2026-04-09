function Card({ children, className = '', elevated = false, compact = false, ...props }) {
  const classes = [
    'panel',
    elevated ? 'panel-elevated' : '',
    compact ? 'panel-compact' : '',
    className
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <section className={classes} {...props}>
      {children}
    </section>
  );
}

export default Card;
