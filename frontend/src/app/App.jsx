import { useCallback, useEffect, useRef, useState } from 'react'
import { getJobs, getMe, login } from '../shared/api/careerpickApi.js'
import { filterSampleJobs, sampleJobs } from '../features/jobs/sampleJobs.js'
import LoginModal from '../features/auth/LoginModal.jsx'
import { createSessionRequestScope } from '../features/auth/sessionRequestScope.js'
import JobChat from '../features/chat/JobChat.jsx'
import JobDetail from '../features/jobs/JobDetail.jsx'
import CoverLetterStartPage from '../pages/CoverLetterStartPage.jsx'
import CoverLetterPage from '../pages/CoverLetterPage.jsx'
import CoverLetterListPage from '../pages/CoverLetterListPage.jsx'
import JobsPage from '../pages/JobsPage.jsx'

export default function App() {
  const [page, setPage] = useState('jobs'), [targetJob, setTargetJob] = useState(null), [selectedJob, setSelectedJob] = useState(null), [chatJob, setChatJob] = useState(null)
  const [query, setQuery] = useState(''), [search, setSearch] = useState(''), [category, setCategory] = useState('전체'), [jobs, setJobs] = useState(sampleJobs)
  const [notice, setNotice] = useState(''), [apiOnline, setApiOnline] = useState(true)
  const [selectedCoverLetterId, setSelectedCoverLetterId] = useState(null)
  const [token, setToken] = useState(() => localStorage.getItem('careerpick-token') || ''), [user, setUser] = useState(null), [loginOpen, setLoginOpen] = useState(false)

  useEffect(() => {
    getJobs(search, category)
      .then(data => { setJobs(data); setApiOnline(true) })
      .catch(() => { setJobs(filterSampleJobs(sampleJobs, search, category)); setApiOnline(false) })
  }, [search, category])

  const coverStartJob = useRef(null)
  const tokenRef = useRef(token)
  const verification = useRef(createSessionRequestScope())
  const loginRequest = useRef(createSessionRequestScope())
  const [sessionError, setSessionError] = useState(null)
  const [sessionAttempt, setSessionAttempt] = useState(0)

  const clearSession = useCallback(() => {
    coverStartJob.current = null
    tokenRef.current = ''
    verification.current.cancel()
    localStorage.removeItem('careerpick-token')
    setToken('')
    setUser(null)
    setSessionError(null)
    setSelectedCoverLetterId(null)
    setTargetJob(null)
    setPage(current => current.startsWith('cover') ? 'cover-list' : current)
  }, [])

  const expireSession = useCallback(failedToken => {
    if (!failedToken || tokenRef.current !== failedToken) return
    const resumeJob = coverStartJob.current
    clearSession()
    if (resumeJob) {
      coverStartJob.current = resumeJob
      setTargetJob(resumeJob)
      setPage('cover-start')
    }
    setLoginOpen(true)
  }, [clearSession])

  useEffect(() => {
    if (!token) return
    const request = verification.current.start()
    getMe(token, {signal: request.signal})
      .then(data => {
        if (!request.isCurrent() || tokenRef.current !== token) return
        setUser(data)
        setSessionError(null)
      })
      .catch(error => {
        if (!request.isCurrent() || tokenRef.current !== token) return
        if (error.status === 401) expireSession(token)
        else setSessionError({token, error})
      })
    return () => verification.current.cancel()
  }, [token, sessionAttempt, expireSession])

  useEffect(() => () => loginRequest.current.cancel(), [])

  const showNotice = text => {
    setNotice(text)
    window.setTimeout(() => setNotice(''), 2200)
  }
  const signIn = async (email, password) => {
    const request = loginRequest.current.start()
    const result = await login(email, password, {signal: request.signal})
    if (!request.isCurrent()) throw new DOMException('취소된 로그인 요청입니다.', 'AbortError')
    verification.current.cancel()
    tokenRef.current = result.token
    localStorage.setItem('careerpick-token', result.token)
    setToken(result.token)
    setUser(result.user)
    setSessionError(null)
    setSelectedCoverLetterId(null)
    const resumeJob = coverStartJob.current
    setTargetJob(resumeJob)
    setPage(current => resumeJob ? 'cover-start' : current.startsWith('cover') ? 'cover-list' : current)
    showNotice(`${result.user.name}님, 반가워요!`)
  }
  const closeLogin = () => {
    loginRequest.current.cancel()
    setLoginOpen(false)
    if (coverStartJob.current) {
      coverStartJob.current = null
      setTargetJob(null)
      setPage('cover-list')
    }
  }
  const signOut = () => {
    loginRequest.current.cancel()
    clearSession()
    setLoginOpen(false)
    showNotice('로그아웃했습니다')
  }
  const openCoverList = () => {
    coverStartJob.current = null
    setSelectedCoverLetterId(null)
    setTargetJob(null)
    setSelectedJob(null)
    setPage('cover-list')
    window.scrollTo({top:0, behavior:'smooth'})
  }
  const openCoverDetail = coverLetterId => {
    setSelectedCoverLetterId(coverLetterId)
    setPage('cover-detail')
    window.scrollTo({top:0, behavior:'smooth'})
  }
  const openCover = job => {
    if (coverStartJob.current) return
    coverStartJob.current = job
    setSelectedCoverLetterId(null)
    setTargetJob(job)
    setSelectedJob(null)
    setPage('cover-start')
    if (!tokenRef.current) setLoginOpen(true)
    window.scrollTo({top:0, behavior:'smooth'})
  }
  const coverCreated = useCallback((detail, jobId, requestToken) => {
    if (coverStartJob.current?.id !== jobId || tokenRef.current !== requestToken) return
    coverStartJob.current = null
    setTargetJob(null)
    setSelectedCoverLetterId(detail.id)
    setPage('cover-detail')
  }, [])
  const openJobs = () => {
    coverStartJob.current = null
    setTargetJob(null)
    setPage('jobs')
  }
  const openCommunity = job => {
    setSelectedJob(null)
    setChatJob(job)
  }

  return <>
    <header className="topbar">
      <button className="brand brand-button" onClick={openJobs}>
        <span>career</span>
        <b>pick</b>
        <i>●</i>
      </button>
      <nav>
        <button className={page === 'jobs' ? 'active' : ''} onClick={openJobs}>채용공고</button>
        <button className={page.startsWith('cover') ? 'active' : ''} onClick={openCoverList}>자기소개서</button>
        <button onClick={() => {openJobs(); setTimeout(() => document.querySelector('.community')?.scrollIntoView({behavior:'smooth'}), 0)}}>커뮤니티</button>
      </nav>
      <div className="header-actions">
        <button className="login-button" onClick={user ? signOut : () => setLoginOpen(true)}>
          {user ? `${user.name} · 로그아웃` : '로그인'}
        </button>
      </div>
    </header>
    {!apiOnline && <div className="api-banner">백엔드 연결 전 · 일부 기능은 데모 모드로 표시됩니다</div>}
    {page === 'jobs'
      ? <JobsPage jobs={jobs} query={query} search={search} category={category} onQuery={setQuery} onSearch={setSearch} onCategory={setCategory} onOpen={setSelectedJob}/>
      : page === 'cover-list'
        ? (
          <CoverLetterListPage
            key={`${token}:${user?.id || 'pending'}`}
            token={token}
            user={user}
            sessionError={sessionError?.token === token ? sessionError.error : null}
            onRetrySession={() => {setSessionError(null); setSessionAttempt(current => current + 1)}}
            onSessionExpired={expireSession}
            onOpen={openCoverDetail}
            onRequireLogin={() => setLoginOpen(true)}
            onFindJobs={openJobs}
          />
        )
        : page === 'cover-detail'
          ? (
            <CoverLetterPage
              key={`${selectedCoverLetterId}:${token}:${user?.id || 'pending'}`}
              coverLetterId={selectedCoverLetterId}
              token={token}
              user={user}
              sessionError={sessionError?.token === token ? sessionError.error : null}
              onRetrySession={() => {setSessionError(null); setSessionAttempt(current => current + 1)}}
              onSessionExpired={expireSession}
              onRequireLogin={() => setLoginOpen(true)}
              onBack={openCoverList}
            />
          )
          : targetJob && (
            <CoverLetterStartPage
              key={`${targetJob.id}:${token}:${user?.id || 'pending'}`}
              job={targetJob}
              token={token}
              user={user}
              sessionError={sessionError?.token === token ? sessionError.error : null}
              onRetrySession={() => {setSessionError(null); setSessionAttempt(current => current + 1)}}
              onRequireLogin={() => setLoginOpen(true)}
              onSessionExpired={expireSession}
              onCreated={coverCreated}
              onBack={openCoverList}
            />
          )}
    <footer>
      <a className="brand small" href="#top">
        <span>career</span>
        <b>pick</b>
        <i>●</i>
      </a>
      <p>공고 탐색부터 자기소개서, 지원자 커뮤니티까지.</p>
      <div>
        <button onClick={openJobs}>채용공고</button>
        <button onClick={openCoverList}>자기소개서</button>
      </div>
      <small>© 2026 Careerpick. MVP demonstration.</small>
    </footer>
    <JobDetail job={selectedJob} onClose={() => setSelectedJob(null)} onWrite={openCover} onCommunity={openCommunity}/>
    <JobChat job={chatJob} user={user} token={token} onClose={() => setChatJob(null)} onRequireLogin={() => setLoginOpen(true)} onConnectionChange={setApiOnline}/>
    <LoginModal key={loginOpen ? 'open' : 'closed'} open={loginOpen} onClose={closeLogin} onLogin={signIn} onSuccess={() => {loginRequest.current.cancel(); setLoginOpen(false)}}/>
    <div className={`toast ${notice ? 'show' : ''}`} role="status">
      {notice}
    </div>
  </>
}
