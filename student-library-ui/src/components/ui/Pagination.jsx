import Button from './Button';

function Pagination({ page, totalPages, onPageChange }) {
  const safeTotalPages = Math.max(totalPages || 1, 1);

  return (
    <div className="pagination-row">
      <p className="muted pagination-label">
        Page {page + 1} of {safeTotalPages}
      </p>
      <div className="pagination-actions">
        <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onPageChange(0)}>
          <span aria-hidden="true">&laquo;</span> First
        </Button>
        <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          <span aria-hidden="true">&lsaquo;</span> Prev
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= safeTotalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next <span aria-hidden="true">&rsaquo;</span>
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= safeTotalPages}
          onClick={() => onPageChange(safeTotalPages - 1)}
        >
          Last <span aria-hidden="true">&raquo;</span>
        </Button>
      </div>
    </div>
  );
}

export default Pagination;
