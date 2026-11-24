import { useEffect, useState } from 'react'
import { getCommunityMessages, getCommunityRooms, leaveCommunity } from '../shared/api/careerpickApi.js'

const formatTime = value => value ? new Date(value).toLocaleString('ko-KR', {
  month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit'
}) : ''

export default function CommunityPage({token, selectedRoomId, onSelectRoom, onSessionExpired, onNotice}) {
  const [rooms, setRooms] = useState([])
  const [messages, setMessages] = useState([])
  const [roomsLoading, setRoomsLoading] = useState(true)
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [error, setError] = useState('')
  const selectedRoom = rooms.find(room => room.id === selectedRoomId)

  const handleError = (requestError, message) => {
    if (requestError.status === 401) onSessionExpired()
    else setError(message)
  }

  useEffect(() => {
    let active = true
    setRoomsLoading(true)
    setError('')
    getCommunityRooms(token)
      .then(data => {
        if (!active) return
        setRooms(data)
        if (selectedRoomId && !data.some(room => room.id === selectedRoomId)) onSelectRoom(null)
      })
      .catch(requestError => {
        if (active) handleError(requestError, '참여 중인 커뮤니티를 불러오지 못했습니다.')
      })
      .finally(() => {if (active) setRoomsLoading(false)})
    return () => {active = false}
  }, [token])

  useEffect(() => {
    if (!selectedRoomId) {
      setMessages([])
      return
    }
    let active = true
    setMessages([])
    setMessagesLoading(true)
    setError('')
    getCommunityMessages(selectedRoomId, token)
      .then(data => {if (active) setMessages(data)})
      .catch(requestError => {
        if (active) handleError(requestError, '메시지를 불러오지 못했습니다.')
      })
      .finally(() => {if (active) setMessagesLoading(false)})
    return () => {active = false}
  }, [selectedRoomId, token])

  const leave = async () => {
    if (!selectedRoom || !window.confirm(`${selectedRoom.companyName} 커뮤니티에서 나갈까요?`)) return
    setError('')
    try {
      await leaveCommunity(selectedRoom.id, token)
      setRooms(current => current.filter(room => room.id !== selectedRoom.id))
      setMessages([])
      onSelectRoom(null)
      onNotice('커뮤니티에서 나갔습니다')
    } catch (requestError) {
      handleError(requestError, '커뮤니티에서 나가지 못했습니다.')
    }
  }

  return <main className="community-page">
    <header className="community-page-head">
      <span className="section-kicker">COMMUNITY</span>
      <h1>지원자 커뮤니티</h1>
      <p>참여 중인 기업의 채용 정보와 경험을 함께 나눠보세요.</p>
    </header>
    {error && <div className="community-error" role="alert">
      {error}
    </div>}
    <section className="community-layout">
      <aside className="room-list" aria-label="참여 중인 커뮤니티">
        <h2>
          내 커뮤니티
          <span>
            {rooms.length}
          </span>
        </h2>
        {roomsLoading && <p className="community-state">목록을 불러오는 중입니다…</p>}
        {!roomsLoading && rooms.length === 0 && <div className="community-state">
          <b>참여 중인 방이 없습니다.</b>
          <p>채용공고에서 지원자 커뮤니티에 참여해 보세요.</p>
        </div>}
        {!roomsLoading && rooms.map(room => <button className={room.id === selectedRoomId ? 'active' : ''} key={room.id} onClick={() => onSelectRoom(room.id)}>
          <span className="room-avatar">
            {room.companyName.slice(0, 1)}
          </span>
          <span>
            <b>
              {room.companyName}
            </b>
            <small>
              {room.lastMessage?.text || '아직 메시지가 없습니다.'}
            </small>
          </span>
          <time>
            {formatTime(room.lastMessage?.createdAt)}
          </time>
        </button>)}
      </aside>
      <section className="community-chat" aria-live="polite">
        {!selectedRoomId && <div className="community-placeholder">
          <span>◌</span>
          <h2>커뮤니티를 선택해 주세요</h2>
          <p>왼쪽 목록에서 참여 중인 기업을 선택하면 대화를 볼 수 있습니다.</p>
        </div>}
        {selectedRoomId && <>
          <header>
            <div>
              <span className="room-avatar">
                {selectedRoom?.companyName.slice(0, 1)}
              </span>
              <h2>
                {selectedRoom?.companyName || '커뮤니티'}
              </h2>
            </div>
            {selectedRoom && <button className="leave-button" onClick={leave}>나가기</button>}
          </header>
          <div className="community-messages">
            {messagesLoading && <p className="community-state">메시지를 불러오는 중입니다…</p>}
            {!messagesLoading && messages.length === 0 && <div className="community-state">
              <b>아직 메시지가 없습니다.</b>
              <p>실시간 대화 기능은 다음 단계에서 추가됩니다.</p>
            </div>}
            {!messagesLoading && messages.map(message => <article className={`community-message ${message.mine ? 'mine' : ''}`} key={message.id}>
              <b>
                {message.author}
              </b>
              <p>
                {message.text}
              </p>
              <time>
                {formatTime(message.createdAt)}
              </time>
            </article>)}
          </div>
          <div className="community-readonly">현재는 기존 메시지만 볼 수 있습니다. 메시지 전송은 준비 중입니다.</div>
        </>}
      </section>
    </section>
  </main>
}
