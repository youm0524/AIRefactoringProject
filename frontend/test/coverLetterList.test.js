import test, {after} from 'node:test'
import assert from 'node:assert/strict'
import {build} from 'esbuild'
import {mkdtemp, writeFile, rm} from 'node:fs/promises'
import {tmpdir} from 'node:os'
import {join} from 'node:path'
import {createRequire} from 'node:module'

// 기존 esbuild로 JSX를 읽어 화면 상태를 검증한다. 새 테스트 의존성은 추가하지 않는다.
const output = await build({
  stdin: {
    contents: `
      import React from 'react';
      import {renderToStaticMarkup} from 'react-dom/server';
      import {CoverLetterListContent} from './src/pages/CoverLetterListPage.jsx';
      import CoverLetterStartPage from './src/pages/CoverLetterStartPage.jsx';
      import CoverLetterPage, {CoverLetterDetailContent} from './src/pages/CoverLetterPage.jsx';
      export const renderDetail = view => renderToStaticMarkup(React.createElement(CoverLetterDetailContent, {view}));
      export const renderDetailPage = props => renderToStaticMarkup(React.createElement(CoverLetterPage, props));
      export const renderStart = props => renderToStaticMarkup(React.createElement(CoverLetterStartPage, props));
      export const render = (view, options = {}) => renderToStaticMarkup(React.createElement(CoverLetterListContent, {view, ...options}));
      export const pageAction = (view, label) => {
        let selected;
        const tree = CoverLetterListContent({view, onPageChange: page => {selected = page}});
        const nav = tree.props.children.find(child => child.type === 'nav');
        const button = nav.props.children.find(child => child.type === 'button' && child.props.children === label);
        if (!button.props.disabled) button.props.onClick();
        return {disabled: button.props.disabled, selected};
      };
      export const deleteFirst = view => {
        let selected;
        const tree = CoverLetterListContent({view, onDelete: letter => {selected = letter}});
        const list = tree.props.children.find(child => child.type === 'ul');
        list.props.children[0].props.children.find(child => child.props.className === 'cover-letter-actions').props.children[1].props.onClick();
        return selected;
      };
      export const selectFirst = view => {
        let selected;
        const tree = CoverLetterListContent({view, onOpen: id => {selected = id}});
        const list = tree.props.children.find(child => child.type === 'ul');
        list.props.children[0].props.children.find(child => child.props.className === 'cover-letter-actions').props.children[0].props.onClick();
        return selected;
      };
    `,
    resolveDir: process.cwd()
  },
  bundle: true, platform: 'node', format: 'cjs', jsx: 'automatic', write: false
})
const directory = await mkdtemp(join(tmpdir(), 'careerpick-list-'))
const filename = join(directory, 'view.cjs')
await writeFile(filename, output.outputFiles[0].text)
const {render, selectFirst, pageAction, deleteFirst, renderStart, renderDetail, renderDetailPage} = createRequire(import.meta.url)(filename)
after(() => rm(directory, {recursive: true, force: true}))

test('로그인 전·로그인 확인 중·조회 중 상태를 구분한다', () => {
  assert.match(render({status: 'signed-out'}), /로그인하면 저장한 자기소개서/)
  assert.match(render({status: 'checking'}), /로그인 정보를 확인/)
  assert.match(render({status: 'loading'}), /자기소개서를 불러오고/)
})

test('조회 실패와 세션 만료를 빈 목록으로 표시하지 않는다', () => {
  const failed = render({status: 'error', error: {status: 500, message: '잠시 후 다시 시도해 주세요.'}})
  assert.match(failed, /role="alert"/)
  assert.match(failed, /다시 시도/)
  assert.doesNotMatch(failed, /저장한 자기소개서가 없습니다/)
  assert.match(render({status: 'error', error: {status: 401}}), /다시 로그인/)
})

test('실제 빈 응답에서 공고 찾기 안내를 표시한다', () => {
  const html = render({status: 'ready', data: {items: [], totalElements: 0}})
  assert.match(html, /저장한 자기소개서가 없습니다/)
  assert.match(html, /채용공고 찾기/)
})

test('목록의 기업·공고·한국 시간과 총 개수를 표시하고 초안 ID로 선택한다', () => {
  const view = {status: 'ready', data: {
    totalElements: 21, page: 0, size: 20,
    items: [{id: 17, jobId: 3, company: '기업 <A>', jobTitle: '백엔드 개발자', updatedAt: '2026-10-30T00:00:00Z'}]
  }}
  const html = render(view)
  assert.match(html, /전체 21개/)
  assert.match(html, /기업 &lt;A&gt;/)
  assert.match(html, /백엔드 개발자/)
  assert.match(html, /dateTime="2026-10-30T00:00:00Z"/i)
  assert.match(html, /9:00/)
  assert.match(html, /이어서 작성/)
  assert.equal(selectFirst(view), 17)
})


test('첫·중간·마지막 페이지의 이동과 경계를 표시한다', () => {
  const viewAt = page => ({status: 'ready', data: {page, size: 20, totalElements: 41,
    items: [{id: 1, company: '기업', jobTitle: '공고', updatedAt: '2026-10-30T00:00:00Z'}]}})
  assert.match(render(viewAt(0)), /1 \/ 3 페이지/)
  assert.deepEqual(pageAction(viewAt(0), '이전'), {disabled: true, selected: undefined})
  assert.deepEqual(pageAction(viewAt(0), '다음'), {disabled: false, selected: 1})
  assert.deepEqual(pageAction(viewAt(1), '이전'), {disabled: false, selected: 0})
  assert.deepEqual(pageAction(viewAt(1), '다음'), {disabled: false, selected: 2})
  assert.deepEqual(pageAction(viewAt(2), '다음'), {disabled: true, selected: undefined})
  assert.match(render(viewAt(2)), /3 \/ 3 페이지/)
})


test('삭제 버튼은 선택한 초안을 전달하고 삭제 중에는 항목·페이지 버튼을 잠근다', () => {
  const letter = {id: 17, jobId: 3, company: '기업', jobTitle: '공고', updatedAt: '2026-10-30T00:00:00Z'}
  const view = {status: 'ready', data: {page: 0, size: 20, totalElements: 21, items: [letter]}}
  assert.deepEqual(deleteFirst(view), letter)
  const html = render(view, {deletingId: 17})
  assert.match(html, /삭제 중…/)
  assert.equal((html.match(/disabled=""/g) || []).length, 4)
  assert.match(html, /기업/)
  assert.match(html, /공고/)
})


test('공고 작성 진입은 선택한 공고와 로그인 안내를 표시하고 로컬 편집기를 열지 않는다', () => {
  const html = renderStart({job: {id: 3, company: '기업', title: '개발자 채용'}, token: '', user: null})
  assert.match(html, /기업 · 개발자 채용/)
  assert.match(html, /로그인하면 선택한 공고/)
  assert.doesNotMatch(html, /textarea|초안 저장/)
})

test('인증 확인 중과 서버 초안 생성 중 상태를 구분한다', () => {
  const props = {job: {id: 3, company: '기업', title: '공고'}, token: 'token'}
  assert.match(renderStart({...props, user: null}), /로그인 정보를 확인/)
  assert.match(renderStart({...props, user: {id: 1}}), /자기소개서를 준비/)
})

test('작성 시작 전 인증 확인의 서버 장애는 재시도로 안내한다', () => {
  const html = renderStart({job: {id: 3, company: '기업', title: '공고'}, token: 'token', user: null,
    sessionError: {status: 500, message: '서버 연결 실패'}})
  assert.match(html, /role="alert"/)
  assert.match(html, /서버 연결 실패/)
  assert.match(html, /다시 시도/)
  assert.doesNotMatch(html, /textarea|초안 저장/)
})


test('상세의 로그인 전·로그인 확인 중·로딩 상태에서는 답변을 표시하지 않는다', () => {
  assert.match(renderDetail({status: 'signed-out'}), /로그인하면 자기소개서/)
  assert.match(renderDetail({status: 'checking'}), /로그인 정보를 확인/)
  assert.match(renderDetail({status: 'loading'}), /자기소개서를 불러오고/)
  assert.doesNotMatch(renderDetail({status: 'loading'}), /textarea/)
  assert.match(renderDetailPage({coverLetterId: 17, token: 'token', user: {id: 1}}), /자기소개서를 불러오고/)
})

test('상세 대상 없음과 서버 오류를 구분하고 서버 오류에 재시도를 제공한다', () => {
  const missing = renderDetail({status: 'not-found'})
  assert.match(missing, /role="alert"/)
  assert.match(missing, /목록에서 다시 선택/)
  assert.doesNotMatch(missing, /textarea|다시 시도/)
  const failed = renderDetail({status: 'error', error: {message: '서버 연결 실패'}})
  assert.match(failed, /서버 연결 실패/)
  assert.match(failed, /다시 시도/)
  assert.doesNotMatch(failed, /textarea/)
})

test('서버의 질문·답변·글자 제한을 응답 순서로 읽기 전용 표시한다', () => {
  const html = renderDetail({status: 'ready', detail: {company: '기업 <A>', jobTitle: '개발자 채용', updatedAt: '2026-10-31T00:00:00Z',
    items: [
      {id: 500, questionText: '서버에서 제공한 첫 질문', maxLength: 120, answer: '😀'},
      {id: 400, questionText: '서버에서 제공한 다음 질문', maxLength: 700, answer: '  저장한 답변\n'},
      {id: 600, questionText: '빈 답변 질문', maxLength: 200, answer: ''}
    ]}})
  assert.match(html, /기업 &lt;A&gt; · 개발자 채용/)
  assert.match(html, /문항 3개/)
  assert.ok(html.indexOf('첫 질문') < html.indexOf('다음 질문'))
  assert.match(html, /2 \/ 120/)
  assert.match(html, /0 \/ 200/)
  assert.match(html, /maxLength="120"/i)
  assert.match(html, /id="cover-item-500"/)
  assert.match(html, /  저장한 답변\n/)
  assert.equal((html.match(/readOnly=""/gi) || []).length, 3)
  assert.doesNotMatch(html, /초안 저장/)
})

test('상세의 마지막 저장 시각을 한국 시간으로 표시하고 목록 복귀를 제공한다', () => {
  const html = renderDetail({status: 'ready', detail: {company: '기업', jobTitle: '공고', updatedAt: '2026-10-31T00:00:00Z', items: []}})
  assert.match(html, /dateTime="2026-10-31T00:00:00Z"/i)
  assert.match(html, /9:00/)
  assert.match(renderDetailPage({coverLetterId: 17, token: 'token', user: {id: 1}}), /목록으로/)
})
