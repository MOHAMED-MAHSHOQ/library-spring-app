function Badge({ children, tone = '' }) {
  const classes = ['badge', tone].filter(Boolean).join(' ');
  return <span className={classes}>{children}</span>;
}

export default Badge;

