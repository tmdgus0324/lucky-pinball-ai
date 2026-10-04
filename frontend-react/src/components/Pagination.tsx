interface PaginationProps {
  page: number; // 1-based
  totalPages: number;
  onPageChange: (page: number) => void;
}

/** 05번 백로그 A3(관리자 목록 페이징) — 페이지 번호 버튼 + 이전/다음. */
export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  // 게임·오류 로그가 쌓이면 페이지가 수십 개가 되므로, 처음·끝 번호와 현재 페이지 앞뒤 2개만 보여준다.
  const shown = new Set([1, totalPages]);
  for (let p = page - 2; p <= page + 2; p++) {
    if (p >= 1 && p <= totalPages) shown.add(p);
  }
  const pages = [...shown].sort((a, b) => a - b);

  return (
    <div className="pagination">
      <button type="button" disabled={page <= 1} onClick={() => onPageChange(1)}>
        처음
      </button>
      <button type="button" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
        이전
      </button>
      {pages.map((p, index) => (
        <span key={p} className="page-group">
          {index > 0 && p - pages[index - 1] > 1 && <span className="page-gap">…</span>}
          <button type="button" className={p === page ? 'active' : ''} onClick={() => onPageChange(p)}>
            {p}
          </button>
        </span>
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
