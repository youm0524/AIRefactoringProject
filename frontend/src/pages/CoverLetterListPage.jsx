import { useEffect, useRef, useState } from 'react'
import { getCoverLetters } from '../shared/api/careerpickApi.js'
import { getLastCoverLetterPage } from '../features/coverLetters/pagination.js'
import { deleteCoverLetterWithConfirmation } from '../features/coverLetters/deleteCoverLetterWithConfirmation.js'

const modifiedAtFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'short'
})

export function CoverLetterListContent({view, onOpen, onRequireLogin, onFindJobs, onRetry, onPageChange, onDelete, deletingId = null}) {
  const isDeleting = deletingId !== null
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
        <div className="cover-letter-actions">
          <button className="login-button" disabled={isDeleting} onClick={() => onOpen(letter.id)} aria-label={`${letter.company} ${letter.jobTitle} 이어서 작성`}>
            이어서 작성
          </button>
          <button className="cover-letter-delete" disabled={isDeleting} onClick={() => onDelete(letter)} aria-label={`${letter.company} ${letter.jobTitle} 삭제`}>
            {deletingId === letter.id ? '삭제 중…' : '삭제'}
          </button>
        </div>
      </li>)}
    </ul>
    <nav className="cover-list-pagination" aria-label="자기소개서 목록 페이지">
      <button className="login-button" disabled={isDeleting || page === 0} onClick={() => onPageChange(page - 1)}>이전</button>
      <span aria-live="polite">
        {`${page + 1} / ${lastPage + 1} 페이지`}
      </span>
      <button className="login-button" disabled={isDeleting || page >= lastPage} onClick={() => onPageChange(page + 1)}>다음</button>
    </nav>
  </>
}

export default function CoverLetterListPage({token, user, onOpen, onRequireLogin, onFindJobs, sessionError, onRetrySession, onSessionExpired}) {
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [selection, setSelection] = useState(null)
  const [deletion, setDeletion] = useState(null)
  const activeDelete = useRef(null)
  const userId = user?.id
  const page = selection?.userId === userId && selection?.token === token ? selection.page : 0

  useEffect(() => () => {
    activeDelete.current?.controller.abort()
    activeDelete.current = null
  }, [token, userId])

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
        if (controller.signal.aborted) return
        if (error.status === 401) onSessionExpired?.(token)
        else setResult({token, userId, attempt, page, status: 'error', error})
      })
    return () => controller.abort()
  }, [token, userId, attempt, page, onSessionExpired])

  let view
  if (!token) view = {status: 'signed-out'}
  else if (!userId && sessionError) view = {status: 'error', error: sessionError}
  else if (!userId) view = {status: 'checking'}
  else if (result?.token !== token || result?.userId !== userId || result?.attempt !== attempt || result?.page !== page) view = {status: 'loading'}
  else view = result

  const currentDeletion = deletion?.token === token && deletion?.userId === userId ? deletion : null
  const removeLetter = async letter => {
    if (activeDelete.current || view.status !== 'ready') return
    const operation = {controller: new AbortController()}
    activeDelete.current = operation
    setDeletion({token, userId, letter, status: 'pending'})
    try {
      const outcome = await deleteCoverLetterWithConfirmation(letter, token, {signal: operation.controller.signal})
      if (operation.controller.signal.aborted) return
      if (outcome === 'cancelled') {
        setDeletion(null)
        return
      }
      setDeletion({token, userId, status: 'success', message: outcome === 'deleted'
        ? '자기소개서를 삭제했습니다.' : '이미 삭제되었거나 접근할 수 없는 자기소개서입니다. 목록을 갱신합니다.'})
      setAttempt(current => current + 1)
    } catch (error) {
      if (operation.controller.signal.aborted) return
      if (error.status === 401) onSessionExpired?.(token)
      else setDeletion({token, userId, letter, status: 'error', error})
    } finally {
      if (activeDelete.current === operation) activeDelete.current = null
    }
  }

  return (
    <main className="cover-page">
      <div className="cover-head">
        <span className="section-kicker">COVER LETTER</span>
        <h1>내 자기소개서</h1>
      </div>
      <section className="cover-list-panel" aria-label="내 자기소개서 목록" aria-busy={view.status === 'loading'}>
        {currentDeletion?.status === 'success' && <p role="status">
          {currentDeletion.message}
        </p>}
        {currentDeletion?.status === 'error' && <div className="cover-list-delete-error">
          <p role="alert">
            {currentDeletion.error.status === 401 ? '로그인이 만료되었습니다. 다시 로그인해 주세요.' : `삭제하지 못했습니다. ${currentDeletion.error.message}`}
          </p>
          <button className="login-button" onClick={currentDeletion.error.status === 401 ? onRequireLogin : () => removeLetter(currentDeletion.letter)}>
            {currentDeletion.error.status === 401 ? '다시 로그인' : '삭제 다시 시도'}
          </button>
        </div>}
        <CoverLetterListContent
          view={view}
          onOpen={onOpen}
          onRequireLogin={onRequireLogin}
          onFindJobs={onFindJobs}
          onRetry={!userId && sessionError ? onRetrySession : () => setAttempt(current => current + 1)}
          onDelete={removeLetter}
          deletingId={currentDeletion?.status === 'pending' ? currentDeletion.letter.id : null}
          onPageChange={nextPage => {
            if (activeDelete.current || view.status !== 'ready' || nextPage < 0 || nextPage > getLastCoverLetterPage(view.data.totalElements, view.data.size)) return
            setDeletion(null)
            setSelection({token, userId, page: nextPage})
          }}
        />
      </section>
    </main>
  )
}
