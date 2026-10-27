import { useState } from 'react'

const STORAGE_PREFIX = 'careerpick-cover-letter-'

function readDrafts() {
  const drafts = []
  for (let index = 0; index < localStorage.length; index += 1) {
    const key = localStorage.key(index)
    if (!key?.startsWith(STORAGE_PREFIX)) continue
    try {
      const draft = JSON.parse(localStorage.getItem(key))
      if (typeof draft?.motivation !== 'string' || typeof draft?.experience !== 'string') continue
      drafts.push({id: key.slice(STORAGE_PREFIX.length), ...draft})
    } catch {
      // Invalid entries cannot be opened by the existing editor.
    }
  }
  return drafts.sort((left, right) => left.id.localeCompare(right.id, 'ko', {numeric: true}))
}

export default function CoverLetterListPage({jobs, onOpen}) {
  const [drafts] = useState(readDrafts)

  return <main className="cover-page cover-list-page">
    <div className="cover-list-heading">
      <div className="cover-head">
        <h1>자기소개서 목록</h1>
        <p>이 브라우저에 저장한 초안을 이어서 작성해 보세요.</p>
      </div>
      <button className="apply-button cover-new" onClick={() => onOpen(null)}>＋ 일반 초안 작성</button>
    </div>
    <div className="cover-list-toolbar"><p className="cover-list-count">저장한 초안 <b>{drafts.length}</b></p><span>초안을 선택해 이어서 작성하세요</span></div>
    {drafts.length ? <ul className="cover-list">
      {drafts.map(draft => {
        const job = draft.id === 'general' ? null : jobs.find(item => String(item.id) === draft.id)
        const targetJob = job || (draft.id === 'general' ? null : {id: draft.id, company: '저장한 공고', title: `공고 #${draft.id}`})
        const title = targetJob ? targetJob.title : '일반 자기소개서'
        const preview = (draft.motivation.trim() || draft.experience.trim()).replace(/\s+/g, ' ')
        return <li key={draft.id}>
          <button className="cover-list-item" onClick={() => onOpen(targetJob)}>
            <span className="cover-item-top"><span className="cover-company">{targetJob ? targetJob.company : '자유 작성'}</span><span className="cover-draft-badge">초안</span></span>
            <h2>{title}</h2>
            <p className="cover-list-preview">{preview || '저장한 내용이 없습니다. 작성을 시작해 보세요.'}</p>
            <span className="cover-list-meta"><span>문항 2개 · 총 {draft.motivation.length + draft.experience.length}자</span><strong>이어서 작성 →</strong></span>
          </button>
        </li>
      })}
    </ul> : <section className="empty-state cover-empty">
      <h2>저장한 자기소개서가 없어요</h2>
      <p>일반 초안을 작성하거나 채용공고에서 자기소개서를 작성한 뒤 저장해 보세요.</p>
    </section>}
  </main>
}
