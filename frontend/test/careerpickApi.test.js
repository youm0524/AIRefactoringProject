import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getCoverLetters, deleteCoverLetter, getJobs, getMessages, sendMessage, login, getMe
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
