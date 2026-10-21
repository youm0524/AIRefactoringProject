import { useEffect, useState } from 'react'
import { getMessages, sendMessage } from '../../shared/api/careerpickApi.js'

export default function JobChat({job, user, token, onClose, onRequireLogin, onConnectionChange}) {
  const [messages, setMessages] = useState([])
  const [text, setText] = useState('')
  useEffect(() => {if (job) getMessages(job.id).then(data => {setMessages(data); onConnectionChange(true)}).catch(() => {setMessages([{id:1,author:'익명지원자',text:`${job.company} 지원하시는 분들 계신가요?`}]); onConnectionChange(false)})}, [job?.id])
  if (!job) return null

  const submit = async event => {
    event.preventDefault()
    if (!user) {onRequireLogin(); return}
    const message = text.trim(); if (!message) return
    try {setMessages(await sendMessage(job.id, message, token)); setText(''); onConnectionChange(true)} catch {onConnectionChange(false)}
  }

  return <aside className="chat-panel open" aria-label={`${job.company} 공고 커뮤니티`}><header><div><span className="avatar" style={{background:job.color}}>{job.logo}</span><p><b>{job.company} 지원자 커뮤니티</b><small><i/> {job.title}</small></p></div><button onClick={onClose} aria-label="채팅 닫기">×</button></header><div className="chat-notice">이 공고에 지원하는 취준생들과 정보를 나눠보세요.</div><div className="messages">{messages.map(item => <div className={`message ${item.author === user?.name ? 'mine' : ''}`} key={item.id}><b>{item.author}</b><p>{item.text}</p></div>)}</div><form className="chat-form" onSubmit={submit}><label className="sr-only" htmlFor="chatInput">메시지</label><input id="chatInput" value={text} onChange={event => setText(event.target.value)} maxLength="120" placeholder={user ? '메시지를 입력하세요' : '로그인 후 대화에 참여할 수 있어요'}/><button aria-label="전송">↑</button></form></aside>
}
