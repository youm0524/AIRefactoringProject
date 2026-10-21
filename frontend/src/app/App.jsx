import { useEffect, useState } from 'react'
import { getJobs, getMe, login } from '../shared/api/careerpickApi.js'
import { filterSampleJobs, sampleJobs } from '../features/jobs/sampleJobs.js'
import LoginModal from '../features/auth/LoginModal.jsx'
import JobChat from '../features/chat/JobChat.jsx'
import JobDetail from '../features/jobs/JobDetail.jsx'
import CoverLetterPage from '../pages/CoverLetterPage.jsx'
import JobsPage from '../pages/JobsPage.jsx'

export default function App() {
  const [page, setPage] = useState('jobs'), [targetJob, setTargetJob] = useState(null), [selectedJob, setSelectedJob] = useState(null), [chatJob, setChatJob] = useState(null)
  const [query, setQuery] = useState(''), [search, setSearch] = useState(''), [category, setCategory] = useState('전체'), [jobs, setJobs] = useState(sampleJobs)
  const [notice, setNotice] = useState(''), [apiOnline, setApiOnline] = useState(true)
  const [token, setToken] = useState(() => localStorage.getItem('careerpick-token') || ''), [user, setUser] = useState(null), [loginOpen, setLoginOpen] = useState(false)

  useEffect(() => {
    getJobs(search, category)
      .then(data => { setJobs(data); setApiOnline(true) })
      .catch(() => { setJobs(filterSampleJobs(sampleJobs, search, category)); setApiOnline(false) })
  }, [search, category])

  useEffect(() => {
    if (token) getMe(token).then(setUser).catch(() => {
      localStorage.removeItem('careerpick-token')
      setToken('')
    })
  }, [token])

  const showNotice = text => {
    setNotice(text)
    window.setTimeout(() => setNotice(''), 2200)
  }
  const signIn = async (email, password) => {
    const result = await login(email, password)
    localStorage.setItem('careerpick-token', result.token)
    setToken(result.token)
    setUser(result.user)
    showNotice(`${result.user.name}님, 반가워요!`)
  }
  const signOut = () => {
    localStorage.removeItem('careerpick-token')
    setToken('')
    setUser(null)
    showNotice('로그아웃했습니다')
  }
  const openCover = job => {
    setTargetJob(job)
    setSelectedJob(null)
    setPage('cover')
    window.scrollTo({top:0, behavior:'smooth'})
  }
  const openCommunity = job => {
    setSelectedJob(null)
    setChatJob(job)
  }

  return <>
    <header className="topbar"><button className="brand brand-button" onClick={() => setPage('jobs')}><span>career</span><b>pick</b><i>●</i></button><nav><button className={page === 'jobs' ? 'active' : ''} onClick={() => setPage('jobs')}>채용공고</button><button className={page === 'cover' ? 'active' : ''} onClick={() => {setTargetJob(null); setPage('cover')}}>자기소개서</button><button onClick={() => {setPage('jobs'); setTimeout(() => document.querySelector('.community')?.scrollIntoView({behavior:'smooth'}), 0)}}>커뮤니티</button></nav><div className="header-actions"><button className="login-button" onClick={user ? signOut : () => setLoginOpen(true)}>{user ? `${user.name} · 로그아웃` : '로그인'}</button></div></header>
    {!apiOnline && <div className="api-banner">백엔드 연결 전 · 일부 기능은 데모 모드로 표시됩니다</div>}
    {page === 'jobs'
      ? <JobsPage jobs={jobs} query={query} search={search} category={category} onQuery={setQuery} onSearch={setSearch} onCategory={setCategory} onOpen={setSelectedJob}/>
      : <CoverLetterPage key={targetJob?.id || 'general'} job={targetJob} onBack={() => setPage('jobs')} onNotice={showNotice}/>}
    <footer><a className="brand small" href="#top"><span>career</span><b>pick</b><i>●</i></a><p>공고 탐색부터 자기소개서, 지원자 커뮤니티까지.</p><div><button onClick={() => setPage('jobs')}>채용공고</button><button onClick={() => {setTargetJob(null); setPage('cover')}}>자기소개서</button></div><small>© 2026 Careerpick. MVP demonstration.</small></footer>
    <JobDetail job={selectedJob} onClose={() => setSelectedJob(null)} onWrite={openCover} onCommunity={openCommunity}/>
    <JobChat job={chatJob} user={user} token={token} onClose={() => setChatJob(null)} onRequireLogin={() => setLoginOpen(true)} onConnectionChange={setApiOnline}/>
    <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} onLogin={signIn}/>
    <div className={`toast ${notice ? 'show' : ''}`} role="status">{notice}</div>
  </>
}
