import test from 'node:test'
import assert from 'node:assert/strict'
import { mergeCommunityMessage, updateRoomWithMessage } from '../src/features/community/communityMessages.js'

test('merges duplicate messages by server id in time order', () => {
  const message = {id: 2, roomId: 7, text: '수정된 결과', createdAt: '2026-11-17T02:00:00Z'}
  const result = mergeCommunityMessage([
    {id: 2, roomId: 7, text: '기존 결과', createdAt: '2026-11-17T02:00:00Z'},
    {id: 1, roomId: 7, text: '이전', createdAt: '2026-11-17T01:00:00Z'}
  ], message)

  assert.deepEqual(result.map(item => item.id), [1, 2])
  assert.equal(result[1].text, '수정된 결과')
})

test('moves a room with a new message to the front', () => {
  const rooms = [
    {id: 1, lastMessage: {id: 1, createdAt: '2026-11-17T01:00:00Z'}},
    {id: 2, lastMessage: null}
  ]
  const message = {id: 3, roomId: 2, text: '새 메시지', createdAt: '2026-11-17T03:00:00Z'}

  assert.deepEqual(updateRoomWithMessage(rooms, message).map(room => room.id), [2, 1])
})
