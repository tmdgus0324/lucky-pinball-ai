import { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { GamePage } from './pages/GamePage';
import { AdminPage } from './pages/AdminPage';

const StudyReactPage = lazy(() => import('./pages/StudyReactPage').then((m) => ({ default: m.StudyReactPage })));
const StudyReactChapterPage = lazy(() =>
  import('./pages/StudyReactChapterPage').then((m) => ({ default: m.StudyReactChapterPage })),
);

const StudySpringPage = lazy(() => import('./pages/StudySpringPage').then((m) => ({ default: m.StudySpringPage })));
const StudySpringChapterPage = lazy(() =>
  import('./pages/StudySpringChapterPage').then((m) => ({ default: m.StudySpringChapterPage })),
);

const loading = <p className="status-text">불러오는 중...</p>;

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<GamePage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/study/react" element={<Suspense fallback={loading}><StudyReactPage /></Suspense>} />
          <Route
            path="/study/react/:chapterId"
            element={<Suspense fallback={loading}><StudyReactChapterPage /></Suspense>}
          />
          <Route path="/study/spring" element={<Suspense fallback={loading}><StudySpringPage /></Suspense>} />
          <Route
            path="/study/spring/:chapterId"
            element={<Suspense fallback={loading}><StudySpringChapterPage /></Suspense>}
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
