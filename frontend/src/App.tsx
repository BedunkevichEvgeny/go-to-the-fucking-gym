import { Link, Route, Routes } from 'react-router-dom';
import './App.css';
import { FreeSessionPage } from './pages/FreeSessionPage';
import { ProgramSessionPage } from './pages/ProgramSessionPage';
import { ProgressionChartPage } from './pages/ProgressionChartPage';
import { SessionHistoryPage } from './pages/SessionHistoryPage';

function HomePage() {
  return (
    <section className="home-grid">
      <section className="card stack-sm">
        <p className="eyebrow">Workout Tracker</p>
        <h1>Train, log, review, improve</h1>
        <p>
          This MVP lets you log your next program session, record a free session,
          review session history, and inspect progression trends per exercise.
        </p>
      </section>
      <nav className="card stack-sm">
        <Link className="button primary" to="/program-session">
          Log next program session
        </Link>
        <Link className="button secondary" to="/free-session">
          Start free session
        </Link>
        <Link className="button ghost" to="/history">
          Browse workout history
        </Link>
      </nav>
    </section>
  );
}

function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/program-session" element={<ProgramSessionPage />} />
        <Route path="/free-session" element={<FreeSessionPage />} />
        <Route path="/history" element={<SessionHistoryPage />} />
        <Route path="/history/:sessionId" element={<SessionHistoryPage />} />
        <Route path="/progression/:exerciseName" element={<ProgressionChartPage />} />
      </Routes>
    </div>
  );
}

export default App;
