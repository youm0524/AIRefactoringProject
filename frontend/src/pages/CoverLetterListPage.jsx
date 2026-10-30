import { useEffect, useState } from 'react'
import { getCoverLetters } from '../shared/api/careerpickApi.js'
import { getLastCoverLetterPage } from '../features/coverLetters/pagination.js'

const modifiedAtFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'short'
})

export function CoverLetterListContent({view, onOpen, onRequireLogin, onFindJobs, onRetry, onPageChange}) {
  if (view.status === 'checking' || view.status === 'loading') {
    return <p role="status">
      {view.status === 'checking' ? '로그인 정보를 확인하고 있습니다.' : '자기소개서를 불러오고 있습니다.'}
    </p>
  }
  if (view.status === 'signed-out') {
    return <div className="cover-list-state">
      <p>로그인하면 저장한 자기소개서를 확인할 수 있습니다.</p>
      <button className="login-button" onClick={onRequireLogin}>로그인</button>
    </div>
  }
  if (view.status === 'error') {
    const expired = view.error.status === 401
    return <div className="cover-list-state">
      <p role="alert">
        {expired ? '로그인이 만료되었습니다. 다시 로그인해 주세요.' : view.error.message}
      </p>
      <button className="login-button" onClick={expired ? onRequireLogin : onRetry}>
        {expired ? '다시 로그인' : '다시 시도'}
      </button>
    </div>
  }
  if (view.data.items.length === 0) {
    return <div className="cover-list-state">
      <p>저장한 자기소개서가 없습니다.</p>
      <button className="login-button" onClick={onFindJobs}>채용공고 찾기</button>
    </div>
  }
  const {page, size, totalElements} = view.data
  const lastPage = getLastCoverLetterPage(totalElements, size)
  return <>
    <p className="cover-list-count">
      {`전체 ${view.data.totalElements}개`}
    </p>
    <ul className="cover-letter-list" aria-label="저장한 자기소개서">
      {view.data.items.map(letter => <li key={letter.id} className="cover-letter-row">
        <div className="cover-letter-summary">
          <p className="cover-letter-company">
            {letter.company}
          </p>
          <h2>
            {letter.jobTitle}
          </h2>
          <p className="cover-letter-modified">
            {'마지막 수정 '}
            <time dateTime={letter.updatedAt}>
              {modifiedAtFormat.format(new Date(letter.updatedAt))}
            </time>
          </p>
        </div>
        <button className="login-button" onClick={() => onOpen(letter.id)} aria-label={`${letter.company} ${letter.jobTitle} 이어서 작성`}>
          이어서 작성
        </button>
      </li>)}
    </ul>
    <nav className="cover-list-pagination" aria-label="자기소개서 목록 페이지">
      <button className="login-button" disabled={page === 0} onClick={() => onPageChange(page - 1)}>이전</button>
      <span aria-live="polite">
        {`${page + 1} / ${lastPage + 1} 페이지`}
      </span>
      <button className="login-button" disabled={page >= lastPage} onClick={() => onPageChange(page + 1)}>다음</button>
    </nav>
  </>
}

export default function CoverLetterListPage({token, user, onOpen, onRequireLogin, onFindJobs}) {
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [selection, setSelection] = useState(null)
  const userId = user?.id
  const page = selection?.userId === userId && selection?.token === token ? selection.page : 0

  useEffect(() => {
    if (!token || !userId) return
    const controller = new AbortController()
    getCoverLetters(token, page, 20, {signal: controller.signal})
      .then(data => {
        if (controller.signal.aborted) return
        const lastPage = getLastCoverLetterPage(data.totalElements, data.size)
        if (data.items.length === 0 && page > lastPage) {
          setResult(null)
          setSelection({token, userId, page: lastPage})
          return
        }
        setResult({token, userId, attempt, page, status: 'ready', data})
      })
      .catch(error => {
        if (!controller.signal.aborted) setResult({token, userId, attempt, page, status: 'error', error})
      })
    return () => controller.abort()
  }, [token, userId, attempt, page])

  let view
  if (!token) view = {status: 'signed-out'}
  else if (!userId) view = {status: 'checking'}
  else if (result?.token !== token || result?.userId !== userId || result?.attempt !== attempt || result?.page !== page) view = {status: 'loading'}
  else view = result

  return (
    <main className="cover-page">
      <div className="cover-head">
        <span className="section-kicker">COVER LETTER</span>
        <h1>내 자기소개서</h1>
      </div>
      <section className="cover-list-panel" aria-label="내 자기소개서 목록" aria-busy={view.status === 'loading'}>
        <CoverLetterListContent
          view={view}
          onOpen={onOpen}
          onRequireLogin={onRequireLogin}
          onFindJobs={onFindJobs}
          onRetry={() => setAttempt(current => current + 1)}
          onPageChange={nextPage => {
            if (view.status !== 'ready' || nextPage < 0 || nextPage > getLastCoverLetterPage(view.data.totalElements, view.data.size)) return
            setSelection({token, userId, page: nextPage})
          }}
        />
      </section>
    </main>
  )
}
