interface PaginationProps {
  page: number; // 1-based
  totalPages: number;
  onPageChange: (page: number) => void;
}

/** 05번 백로그 A3(관리자 목록 페이징) — 페이지 번호 버튼 + 이전/다음. */
export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <div className="pagination">
      <button type="button" disabled={page <= 1} onClick={() => onPageChange(1)}>
        처음
      </button>
      <button type="button" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
        이전
      </button>
      {pages.map((p) => (
        <button
          key={p}
          type="button"
          className={p === page ? 'active' : ''}
          onClick={() => onPageChange(p)}
        >
          {p}
        </button>
      ))}
      <button type="button" disabled={page >= totalPages} onClick={() => onPageChange(page + 1)}>
        다음
      </button>
      <button type="button" disabled={page >= totalPages} onClick={() => onPageChange(totalPages)}>
        끝
      </button>
      <span className="page-info">
        {page} / {totalPages} 페이지
      </span>
    </div>
  );
}
