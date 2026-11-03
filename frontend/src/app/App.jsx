import { useEffect, useRef, useState } from 'react'
import { getJobs, getMe, joinCommunity, login } from '../shared/api/careerpickApi.js'
import { filterSampleJobs, sampleJobs } from '../features/jobs/sampleJobs.js'
import LoginModal from '../features/auth/LoginModal.jsx'
import JobDetail from '../features/jobs/JobDetail.jsx'
import CommunityPage from '../pages/CommunityPage.jsx'
import CoverLetterPage from '../pages/CoverLetterPage.jsx'
import JobsPage from '../pages/JobsPage.jsx'

const OPEN_COMMUNITY = 'OPEN_COMMUNITY'
const JOIN_JOB_COMMUNITY = 'JOIN_JOB_COMMUNITY'

export default function App() {
  const [page, setPage] = useState('jobs'), [targetJob, setTargetJob] = useState(null), [selectedJob, setSelectedJob] = useState(null)
  const [query, setQuery] = useState(''), [search, setSearch] = useState(''), [category, setCategory] = useState('전체'), [jobs, setJobs] = useState(sampleJobs)
  const [notice, setNotice] = useState(''), [apiOnline, setApiOnline] = useState(true)
  const [token, setToken] = useState(() => localStorage.getItem('careerpick-token') || ''), [user, setUser] = useState(null), [loginOpen, setLoginOpen] = useState(false)
  const [selectedRoomId, setSelectedRoomId] = useState(null)
  const loginIntent = useRef(null)

  const showNotice = text => {
    setNotice(text)
    window.setTimeout(() => setNotice(''), 2200)
  }

  useEffect(() => {
    getJobs(search, category)
      .then(data => { setJobs(data); setApiOnline(true) })
      .catch(() => { setJobs(filterSampleJobs(sampleJobs, search, category)); setApiOnline(false) })
  }, [search, category])

  useEffect(() => {
    if (token) getMe(token).then(setUser).catch(() => {
      localStorage.removeItem('careerpick-token')
      setToken('')
      setUser(null)
    })
  }, [token])

  const openLogin = intent => {
    loginIntent.current = intent
    setLoginOpen(true)
  }
  const closeLogin = () => {
    setLoginOpen(false)
    loginIntent.current = null
  }
  const joinAndOpen = async (jobId, sessionToken) => {
    const result = await joinCommunity(jobId, sessionToken)
    setSelectedRoomId(result.room.id)
    setPage('community')
    window.scrollTo({top: 0, behavior: 'smooth'})
  }
  const signIn = async (email, password) => {
    const result = await login(email, password)
    localStorage.setItem('careerpick-token', result.token)
    setToken(result.token)
    setUser(result.user)
    showNotice(`${result.user.name}님, 반가워요!`)
    const intent = loginIntent.current
    if (intent?.type === OPEN_COMMUNITY) {
      setSelectedRoomId(null)
      setPage('community')
    }
    if (intent?.type === JOIN_JOB_COMMUNITY) {
      try {
        await joinAndOpen(intent.jobId, result.token)
      } catch {
        showNotice('커뮤니티에 참여하지 못했습니다. 다시 시도해 주세요.')
      }
    }
    loginIntent.current = null
  }
  const signOut = () => {
    localStorage.removeItem('careerpick-token')
    setToken('')
    setUser(null)
    setSelectedRoomId(null)
    if (page === 'community') setPage('jobs')
    showNotice('로그아웃했습니다')
  }
  const sessionExpired = () => {
    localStorage.removeItem('careerpick-token')
    setToken('')
    setUser(null)
    setSelectedRoomId(null)
    setPage('jobs')
    openLogin({type: OPEN_COMMUNITY})
    showNotice('로그인이 만료되었습니다. 다시 로그인해 주세요.')
  }
  const openCover = job => {
    setTargetJob(job)
    setSelectedJob(null)
    setPage('cover')
    window.scrollTo({top:0, behavior:'smooth'})
  }
  const openCommunity = async job => {
    setSelectedJob(null)
    if (!token) {
      openLogin({type: JOIN_JOB_COMMUNITY, jobId: job.id})
      return
    }
    try {
      await joinAndOpen(job.id, token)
    } catch (requestError) {
      if (requestError.status === 401) sessionExpired()
      else showNotice('커뮤니티에 참여하지 못했습니다. 다시 시도해 주세요.')
    }
  }
  const openCommunityHome = () => {
    if (!token) {
      openLogin({type: OPEN_COMMUNITY})
      return
    }
    setSelectedRoomId(null)
    setPage('community')
    window.scrollTo({top: 0, behavior: 'smooth'})
  }

  return <>
    <header className="topbar">
      <button className="brand brand-button" onClick={() => setPage('jobs')}>
        <span>career</span>
        <b>pick</b>
        <i>●</i>
      </button>
      <nav>
        <button className={page === 'jobs' ? 'active' : ''} onClick={() => setPage('jobs')}>채용공고</button>
        <button className={page === 'cover' ? 'active' : ''} onClick={() => {setTargetJob(null); setPage('cover')}}>자기소개서</button>
        <button className={page === 'community' ? 'active' : ''} onClick={openCommunityHome}>커뮤니티</button>
      </nav>
      <div className="header-actions">
        <button className="login-button" disabled={Boolean(token && !user)} onClick={token ? signOut : () => openLogin(null)}>
          {user ? `${user.name} · 로그아웃` : token ? '로그인 확인 중…' : '로그인'}
        </button>
      </div>
    </header>
    {!apiOnline && <div className="api-banner">백엔드 연결 전 · 일부 기능은 데모 모드로 표시됩니다</div>}
    {page === 'jobs' && (
      <JobsPage jobs={jobs} query={query} search={search} category={category} onQuery={setQuery} onSearch={setSearch} onCategory={setCategory} onOpen={setSelectedJob}/>
    )}
    {page === 'cover' && (
      <CoverLetterPage key={targetJob?.id || 'general'} job={targetJob} onBack={() => setPage('jobs')} onNotice={showNotice}/>
    )}
    {page === 'community' && (
      <CommunityPage token={token} selectedRoomId={selectedRoomId} onSelectRoom={setSelectedRoomId} onSessionExpired={sessionExpired} onNotice={showNotice}/>
    )}
    <footer>
      <a className="brand small" href="#top">
        <span>career</span>
        <b>pick</b>
        <i>●</i>
      </a>
      <p>공고 탐색부터 자기소개서, 지원자 커뮤니티까지.</p>
      <div>
        <button onClick={() => setPage('jobs')}>채용공고</button>
        <button onClick={() => {setTargetJob(null); setPage('cover')}}>자기소개서</button>
        <button onClick={openCommunityHome}>커뮤니티</button>
      </div>
      <small>© 2026 Careerpick. MVP demonstration.</small>
    </footer>
    <JobDetail job={selectedJob} onClose={() => setSelectedJob(null)} onWrite={openCover} onCommunity={openCommunity}/>
    <LoginModal open={loginOpen} onClose={closeLogin} onLogin={signIn}/>
    <div className={`toast ${notice ? 'show' : ''}`} role="status">
      {notice}
    </div>
  </>
}
