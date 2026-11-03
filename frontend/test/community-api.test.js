import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import {
  getCommunityMessages,
  getCommunityRooms,
  joinCommunity,
  leaveCommunity
} from '../src/shared/api/careerpickApi.js'

const originalFetch = global.fetch
afterEach(() => {global.fetch = originalFetch})

test('community API uses room endpoints and bearer authentication', async () => {
  const calls = []
  global.fetch = async (url, options = {}) => {
    calls.push({url, options})
    return {ok: true, status: options.method === 'DELETE' ? 204 : 200, json: async () => []}
  }

  await joinCommunity(3, 'token')
  await getCommunityRooms('token')
  await getCommunityMessages(7, 'token')
  assert.equal(await leaveCommunity(7, 'token'), null)

  assert.deepEqual(calls.map(call => [call.url, call.options.method || 'GET']), [
    ['http://localhost:8080/api/jobs/3/community-membership', 'PUT'],
    ['http://localhost:8080/api/community-rooms', 'GET'],
    ['http://localhost:8080/api/community-rooms/7/messages', 'GET'],
    ['http://localhost:8080/api/community-rooms/7/membership', 'DELETE']
  ])
  assert.ok(calls.every(call => call.options.headers.Authorization === 'Bearer token'))
})

test('community API exposes the response status on failure', async () => {
  global.fetch = async () => ({ok: false, status: 403})

  await assert.rejects(() => getCommunityMessages(7, 'token'), error => error.status === 403)
})
