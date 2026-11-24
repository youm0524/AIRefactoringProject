import test from 'node:test'
import assert from 'node:assert/strict'
import {deleteCoverLetterWithConfirmation} from '../src/features/coverLetters/deleteCoverLetterWithConfirmation.js'

const letter = {id: 17, jobId: 3, company: '기업', jobTitle: '개발자 채용'}

test('삭제 확인 취소 시 서버 요청을 보내지 않는다', async t => {
  t.mock.method(globalThis, 'fetch', () => assert.fail('취소한 삭제 요청'))
  const outcome = await deleteCoverLetterWithConfirmation(letter, 'token', {confirm: message => {
    assert.match(message, /기업 · 개발자 채용/)
    assert.match(message, /문항과 답변/)
    return false
  }})
  assert.equal(outcome, 'cancelled')
})

test('확인한 초안 ID로 삭제하고 204를 성공으로 처리한다', async t => {
  const controller = new AbortController()
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/cover-letters/17')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers.Authorization, 'Bearer token')
    assert.equal(options.signal, controller.signal)
    return new Response(null, {status: 204})
  })
  assert.equal(await deleteCoverLetterWithConfirmation(letter, 'token', {confirm: () => true, signal: controller.signal}), 'deleted')
})

test('이미 삭제된 대상의 404는 목록 갱신 대상으로 구분한다', async t => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({code: 'COVER_LETTER_NOT_FOUND', message: '대상 없음'}, {status: 404}))
  assert.equal(await deleteCoverLetterWithConfirmation(letter, 'token', {confirm: () => true}), 'missing')
})

test('인증·서버 실패는 성공으로 바꾸지 않고 상태와 메시지를 유지한다', async t => {
  for (const status of [401, 500]) {
    t.mock.method(globalThis, 'fetch', async () => Response.json({code: 'ERROR', message: '삭제 실패'}, {status}))
    await assert.rejects(deleteCoverLetterWithConfirmation(letter, 'token', {confirm: () => true}),
      error => error.status === status && error.message === '삭제 실패')
  }
})

test('네트워크 실패와 취소를 그대로 전달하고 원본 항목을 변경하지 않는다', async t => {
  const original = {...letter}
  for (const error of [new TypeError('Failed to fetch'), new DOMException('Aborted', 'AbortError')]) {
    t.mock.method(globalThis, 'fetch', async () => {throw error})
    await assert.rejects(deleteCoverLetterWithConfirmation(letter, 'token', {confirm: () => true}), actual => actual === error)
    assert.deepEqual(letter, original)
  }
})
