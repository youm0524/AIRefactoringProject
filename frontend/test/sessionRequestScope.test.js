import test from 'node:test'
import assert from 'node:assert/strict'
import {createSessionRequestScope} from '../src/features/auth/sessionRequestScope.js'
import {getMe, login} from '../src/shared/api/careerpickApi.js'

test('새 인증 요청은 이전 요청을 취소하고 이전 응답의 적용을 막는다', () => {
  const scope = createSessionRequestScope()
  const first = scope.start()
  assert.equal(first.isCurrent(), true)
  const second = scope.start()
  assert.equal(first.signal.aborted, true)
  assert.equal(first.isCurrent(), false)
  assert.equal(second.isCurrent(), true)
})

test('로그아웃·모달 닫기 후 응답은 무효이고 다시 로그인한 요청에는 영향이 없다', () => {
  const scope = createSessionRequestScope()
  const oldRequest = scope.start()
  scope.cancel()
  assert.equal(oldRequest.isCurrent(), false)
  assert.equal(oldRequest.signal.aborted, true)
  scope.cancel()
  const newRequest = scope.start()
  assert.equal(newRequest.isCurrent(), true)
  assert.equal(oldRequest.isCurrent(), false)
})

test('이전 사용자 응답이 늦게 도착해도 현재 사용자로 덮어쓰지 않는다', async () => {
  const scope = createSessionRequestScope()
  let resolveOld
  let user = null
  const old = scope.start()
  const pending = new Promise(resolve => {resolveOld = resolve})
    .then(data => {if (old.isCurrent()) user = data})
  const current = scope.start()
  if (current.isCurrent()) user = {id: 2}
  resolveOld({id: 1})
  await pending
  assert.deepEqual(user, {id: 2})
})

test('이전 세션의 실패가 새 세션을 로그아웃시키지 않는다', async () => {
  const scope = createSessionRequestScope()
  let rejectOld
  let signedOut = false
  const old = scope.start()
  const pending = new Promise((resolve, reject) => {rejectOld = reject})
    .catch(() => {if (old.isCurrent()) signedOut = true})
  scope.start()
  rejectOld(new Error('이전 세션 만료'))
  await pending
  assert.equal(signedOut, false)
})

test('로그인·내 정보 API에 취소 신호를 전달하고 서버 장애를 401과 구분한다', async t => {
  const controller = new AbortController()
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(options.signal, controller.signal)
    if (url.endsWith('/auth/login')) return Response.json({token: 'new', user: {id: 2}})
    assert.equal(options.headers.Authorization, 'Bearer new')
    return Response.json({code: 'INTERNAL_ERROR', message: '잠시 후 다시 시도해 주세요.'}, {status: 500})
  })
  assert.equal((await login('demo@test.kr', 'password', {signal: controller.signal})).user.id, 2)
  await assert.rejects(getMe('new', {signal: controller.signal}), error => error.status === 500)
})
