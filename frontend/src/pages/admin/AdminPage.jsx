import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { adminApi } from '../../api/endpoints';
import styles from './AdminPage.module.css';

export default function AdminPage() {
  const navigate = useNavigate();
  const { logout } = useAuthStore();
  const [tab, setTab] = useState('stats');

  // Stats
  const [stats, setStats]   = useState(null);
  const [statsErr, setStatsErr] = useState(null);

  // Users
  const [users, setUsers]   = useState([]);
  const [userPage, setUserPage] = useState(0);
  const [userTotal, setUserTotal] = useState(0);

  // Sessions
  const [sessions, setSessions] = useState([]);
  const [sessPage, setSessPage] = useState(0);
  const [sessTotal, setSessTotal] = useState(0);

  // Session detail
  const [selectedSession, setSelectedSession] = useState(null);
  const [responses, setResponses] = useState([]);
  const [score, setScore] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const PAGE_SIZE = 20;

  const handleLogout = () => { logout(); navigate('/'); };

  // Load stats
  useEffect(() => {
    adminApi.stats()
      .then(r => setStats(r.data.data))
      .catch(() => setStatsErr('Failed to load stats'));
  }, []);

  // Load users
  const loadUsers = useCallback((page = 0) => {
    setLoading(true); setError(null);
    adminApi.listUsers(page, PAGE_SIZE)
      .then(r => { setUsers(r.data.data.content); setUserTotal(r.data.data.totalElements); setUserPage(page); })
      .catch(() => setError('Failed to load users'))
      .finally(() => setLoading(false));
  }, []);

  // Load sessions
  const loadSessions = useCallback((page = 0) => {
    setLoading(true); setError(null);
    adminApi.listSessions(page, PAGE_SIZE)
      .then(r => { setSessions(r.data.data.content); setSessTotal(r.data.data.totalElements); setSessPage(page); })
      .catch(() => setError('Failed to load sessions'))
      .finally(() => setLoading(false));
  }, []);

  // Load session detail
  const openSession = useCallback((session) => {
    setSelectedSession(session);
    setDetailLoading(true);
    setResponses([]); setScore(null);
    Promise.all([
      adminApi.listResponses(session.id),
      adminApi.getScore(session.id).catch(() => null),
    ]).then(([rRes, sRes]) => {
      setResponses(rRes.data.data ?? []);
      setScore(sRes?.data?.data ?? null);
    }).finally(() => setDetailLoading(false));
  }, []);

  useEffect(() => { if (tab === 'users')    loadUsers(0);    }, [tab, loadUsers]);
  useEffect(() => { if (tab === 'sessions') loadSessions(0); }, [tab, loadSessions]);

  const totalUserPages = Math.ceil(userTotal / PAGE_SIZE);
  const totalSessPages = Math.ceil(sessTotal / PAGE_SIZE);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <span className={styles.logo}>ConnectPWD <span className={styles.badge}>Admin</span></span>
        <button className={styles.logoutBtn} onClick={handleLogout}>Logout</button>
      </header>

      <nav className={styles.nav}>
        {['stats','users','sessions'].map(t => (
          <button key={t} className={`${styles.navBtn} ${tab === t ? styles.active : ''}`}
            onClick={() => { setSelectedSession(null); setTab(t); }}>
            {t.charAt(0).toUpperCase() + t.slice(1)}
          </button>
        ))}
      </nav>

      <main className={styles.main}>
        {error && <p className={styles.error}>{error}</p>}

        {/* ---- STATS ---- */}
        {tab === 'stats' && (
          <div className={styles.statsGrid}>
            {statsErr && <p className={styles.error}>{statsErr}</p>}
            {stats ? (
              <>
                <StatCard label="Total Users"          value={stats.totalUsers} />
                <StatCard label="Active Sessions"      value={stats.activeSessions} />
                <StatCard label="Completed Sessions"   value={stats.completedSessions} />
                <StatCard label="Reports Generated"    value={stats.totalReports} />
              </>
            ) : !statsErr && <p className={styles.loading}>Loading stats…</p>}
          </div>
        )}

        {/* ---- USERS ---- */}
        {tab === 'users' && (
          <>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead><tr>
                  <th>Name</th><th>Email</th><th>Role</th><th>Language</th>
                  <th>Sessions</th><th>Active</th><th>Created</th>
                </tr></thead>
                <tbody>
                  {loading ? <tr><td colSpan={7} className={styles.loading}>Loading…</td></tr>
                    : users.map(u => (
                    <tr key={u.id}>
                      <td>{u.fullName}</td>
                      <td>{u.email}</td>
                      <td><span className={`${styles.pill} ${styles['pill_'+u.role]}`}>{u.role}</span></td>
                      <td>{u.language}</td>
                      <td>{u.sessionCount}</td>
                      <td>{u.isActive ? '✓' : '✗'}</td>
                      <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={userPage} total={totalUserPages} onChange={loadUsers} />
          </>
        )}

        {/* ---- SESSIONS ---- */}
        {tab === 'sessions' && !selectedSession && (
          <>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead><tr>
                  <th>User</th><th>Module</th><th>Status</th><th>Level</th>
                  <th>Language</th><th>Started</th><th>Detail</th>
                </tr></thead>
                <tbody>
                  {loading ? <tr><td colSpan={7} className={styles.loading}>Loading…</td></tr>
                    : sessions.map(s => (
                    <tr key={s.id}>
                      <td>{s.userFullName ?? s.userId}</td>
                      <td>{s.moduleType ?? '—'}</td>
                      <td><span className={`${styles.pill} ${styles['pill_'+s.status]}`}>{s.status}</span></td>
                      <td>{s.currentLevel}</td>
                      <td>{s.language}</td>
                      <td>{new Date(s.startedAt).toLocaleDateString()}</td>
                      <td><button className={styles.detailBtn} onClick={() => openSession(s)}>View</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={sessPage} total={totalSessPages} onChange={loadSessions} />
          </>
        )}

        {/* ---- SESSION DETAIL ---- */}
        {tab === 'sessions' && selectedSession && (
          <div className={styles.detail}>
            <button className={styles.backBtn} onClick={() => setSelectedSession(null)}>← Back</button>
            <h2 className={styles.detailTitle}>Session: {selectedSession.id}</h2>
            <p className={styles.detailMeta}>
              <b>User:</b> {selectedSession.userEmail} &nbsp;|&nbsp;
              <b>Module:</b> {selectedSession.moduleType ?? '—'} &nbsp;|&nbsp;
              <b>Status:</b> {selectedSession.status}
            </p>

            {score && (
              <div className={styles.scoreBox}>
                <h3>ISAA Score</h3>
                <div className={styles.scoreGrid}>
                  <ScoreItem label="Total"     value={score.totalScore} />
                  <ScoreItem label="Severity"  value={score.severity} />
                  <ScoreItem label="Disability %" value={score.disabilityPct} />
                  <ScoreItem label="Social"    value={score.domain1Social} />
                  <ScoreItem label="Emotional" value={score.domain2Emotional} />
                  <ScoreItem label="Speech"    value={score.domain3Speech} />
                  <ScoreItem label="Behaviour" value={score.domain4Behaviour} />
                  <ScoreItem label="Sensory"   value={score.domain5Sensory} />
                  <ScoreItem label="Cognitive" value={score.domain6Cognitive} />
                </div>
              </div>
            )}

            <h3 className={styles.responsesTitle}>Responses ({responses.length})</h3>
            {detailLoading && <p className={styles.loading}>Loading…</p>}
            {!detailLoading && responses.length === 0 && <p className={styles.empty}>No responses yet.</p>}
            <div className={styles.responseList}>
              {responses.map((r, i) => (
                <div key={r.id ?? i} className={styles.responseCard}>
                  <div className={styles.responseQ}><b>Q{i+1}:</b> {r.questionText}</div>
                  <div className={styles.responseMeta}>
                    <span className={styles.pill}>L{r.level}</span>
                    <span>{r.domain}</span>
                    <span className={styles.answerType}>{r.answerType}</span>
                  </div>
                  <div className={styles.responseA}>
                    {r.answerText && <span><b>Answer:</b> {r.answerText}</span>}
                    {r.scaleValue != null && <span><b>Scale:</b> {r.scaleValue}</span>}
                    {r.transcript && <span><b>Transcript:</b> {r.transcript}</span>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className={styles.statCard}>
      <div className={styles.statValue}>{value ?? '—'}</div>
      <div className={styles.statLabel}>{label}</div>
    </div>
  );
}

function ScoreItem({ label, value }) {
  return <div className={styles.scoreItem}><span className={styles.scoreLabel}>{label}</span><span className={styles.scoreValue}>{value}</span></div>;
}

function Pagination({ page, total, onChange }) {
  if (total <= 1) return null;
  return (
    <div className={styles.pagination}>
      <button disabled={page === 0} onClick={() => onChange(page - 1)}>‹ Prev</button>
      <span>Page {page + 1} / {total}</span>
      <button disabled={page >= total - 1} onClick={() => onChange(page + 1)}>Next ›</button>
    </div>
  );
}
