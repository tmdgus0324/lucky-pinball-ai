import { useMemo, useState } from 'react';

/**
 * 관리자 목록(참가자·게임·오류 로그)이 같이 쓰는 클라이언트 페이징.
 * 목록이 줄어들어 현재 페이지가 범위를 벗어나면(예: 데이터가 다시 로드됨) 마지막 페이지로 맞춘다.
 */
export function usePagination<T>(items: T[] | null, pageSize: number) {
  const [requestedPage, setPage] = useState(1);

  const totalPages = items ? Math.max(1, Math.ceil(items.length / pageSize)) : 1;
  const page = Math.min(requestedPage, totalPages);

  const pageItems = useMemo(() => {
    if (!items) return [];
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
  }, [items, page, pageSize]);

  return { page, totalPages, pageItems, setPage };
}
