import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getCoverLetters, createCoverLetter, getCoverLetter, updateCoverLetter,
  deleteCoverLetter, getJobs, getMessages, sendMessage, login, getMe
} from '../src/shared/api/careerpickApi.js'

test('자기소개서 목록 요청은 인증·페이지 조건과 취소 신호를 전달한다', async t => {
  const page = {items: [{id: 10, company: '기업'}], page: 1, size: 5, totalElements: 6}
  const controller = new AbortController()
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters?page=1&size=5')
    assert.equal(options.headers.Authorization, 'Bearer token')
    assert.equal(options.signal, controller.signal)
    return Response.json(page)
  })
  assert.deepEqual(await getCoverLetters('token', 1, 5, {signal: controller.signal}), page)
})

test('빈 목록과 기본 페이지 조건을 처리한다', async t => {
  const page = {items: [], page: 0, size: 20, totalElements: 0}
  t.mock.method(globalThis, 'fetch', async url => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters?page=0&size=20')
    return Response.json(page)
  })
  assert.deepEqual(await getCoverLetters('token'), page)
})

test('삭제는 인증 헤더를 보내고 204 응답을 JSON으로 읽지 않는다', async t => {
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters/10')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers.Authorization, 'Bearer token')
    assert.equal(options.body, undefined)
    return {ok: true, status: 204, json: () => assert.fail('204는 JSON 파싱하지 않아야 한다')}
  })
  assert.equal(await deleteCoverLetter(10, 'token'), undefined)
})

test('서버 오류의 상태·코드·메시지를 보존한다', async t => {
  const cases = [
    [400, 'INVALID_REQUEST', '입력값을 확인해 주세요.'],
    [401, 'AUTHENTICATION_REQUIRED', '로그인이 필요합니다.'],
    [404, 'COVER_LETTER_NOT_FOUND', '자기소개서를 찾을 수 없습니다.']
  ]
  for (const [status, code, message] of cases) {
    t.mock.method(globalThis, 'fetch', async () => Response.json({code, message}, {status}))
    await assert.rejects(deleteCoverLetter(10, 'token'), error =>
      error.status === status && error.code === code && error.message === message)
  }
})

test('비JSON·빈 오류 응답도 HTTP 상태를 유지한다', async t => {
  for (const body of ['<html>Bad Gateway</html>', '']) {
    t.mock.method(globalThis, 'fetch', async () => new Response(body, {status: 502}))
    await assert.rejects(getCoverLetters('token'), error =>
      error.status === 502 && error.code === null && error.message === 'API 오류: 502')
  }
})

test('네트워크 실패와 요청 취소를 성공 응답으로 바꾸지 않는다', async t => {
  for (const error of [new TypeError('Failed to fetch'), new DOMException('Aborted', 'AbortError')]) {
    t.mock.method(globalThis, 'fetch', async () => {throw error})
    await assert.rejects(getCoverLetters('token'), actual => actual === error)
  }
})

test('기존 공고·인증·채팅 API의 성공 응답 처리를 유지한다', async t => {
  const responses = [[], [], {id: 1}, {token: 'token', user: {id: 1}}, {id: 1, name: '사용자'}]
  let index = 0
  t.mock.method(globalThis, 'fetch', async () => Response.json(responses[index++]))
  assert.deepEqual(await getJobs(), responses[0])
  assert.deepEqual(await getMessages(1), responses[1])
  assert.deepEqual(await sendMessage(1, '안녕하세요', 'token'), responses[2])
  assert.deepEqual(await login('demo@careerpick.kr', 'password'), responses[3])
  assert.deepEqual(await getMe('token'), responses[4])
})

test('공고 ID로 생성 요청을 보내고 신규 201·기존 200의 상세를 반환한다', async t => {
  const controller = new AbortController()
  const detail = {id: 17, jobId: 3, items: [{id: 101, answer: '기존 답변'}]}
  for (const status of [201, 200]) {
    t.mock.method(globalThis, 'fetch', async (url, options) => {
      assert.equal(url, 'http://localhost:8080/api/cover-letters')
      assert.equal(options.method, 'POST')
      assert.equal(options.headers.Authorization, 'Bearer token')
      assert.equal(options.headers['Content-Type'], 'application/json')
      assert.deepEqual(JSON.parse(options.body), {jobId: 3})
      assert.equal(options.signal, controller.signal)
      return Response.json(detail, {status})
    })
    assert.deepEqual(await createCoverLetter(3, 'token', {signal: controller.signal}), detail)
  }
})

test('공고 ID가 아닌 초안 ID로 상세를 조회하고 문항 정보를 그대로 반환한다', async t => {
  const controller = new AbortController()
  const detail = {id: 17, jobId: 3, items: [{id: 101, questionText: '지원 동기', maxLength: 700, answer: ''}]}
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters/17')
    assert.equal(options.method || 'GET', 'GET')
    assert.equal(options.headers.Authorization, 'Bearer token')
    assert.equal(options.body, undefined)
    assert.equal(options.signal, controller.signal)
    return Response.json(detail)
  })
  assert.deepEqual(await getCoverLetter(17, 'token', {signal: controller.signal}), detail)
})

test('초안 경로와 문항 ID를 구분해 전체 답변을 저장하고 공백·빈 답변을 보존한다', async t => {
  const controller = new AbortController()
  const items = [{id: 101, answer: '  지원 동기\n'}, {id: 102, answer: ''}]
  const before = structuredClone(items)
  const saved = {id: 17, jobId: 3, updatedAt: '2026-10-31T01:00:00Z', items}
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters/17')
    assert.equal(options.method, 'PUT')
    assert.equal(options.headers.Authorization, 'Bearer token')
    assert.equal(options.headers['Content-Type'], 'application/json')
    assert.equal(options.signal, controller.signal)
    assert.deepEqual(JSON.parse(options.body), {items})
    return Response.json(saved)
  })
  assert.deepEqual(await updateCoverLetter(17, items, 'token', {signal: controller.signal}), saved)
  assert.deepEqual(items, before)
})

test('새 API도 오류 상태·코드·메시지와 네트워크 실패·취소를 보존한다', async t => {
  const requests = [
    () => createCoverLetter(3, 'token'),
    () => getCoverLetter(17, 'token'),
    () => updateCoverLetter(17, [{id: 101, answer: ''}], 'token')
  ]
  for (const request of requests) {
    for (const status of [400, 401, 404, 500]) {
      t.mock.method(globalThis, 'fetch', async () => Response.json({code: 'TEST_ERROR', message: '테스트 오류'}, {status}))
      await assert.rejects(request(), error => error.status === status && error.code === 'TEST_ERROR' && error.message === '테스트 오류')
    }
    t.mock.method(globalThis, 'fetch', async () => new Response('<html>Gateway error</html>', {status: 502}))
    await assert.rejects(request(), error => error.status === 502 && error.message === 'API 오류: 502')
    for (const error of [new TypeError('Failed to fetch'), new DOMException('Aborted', 'AbortError')]) {
      t.mock.method(globalThis, 'fetch', async () => {throw error})
      await assert.rejects(request(), actual => actual === error)
    }
  }
})
