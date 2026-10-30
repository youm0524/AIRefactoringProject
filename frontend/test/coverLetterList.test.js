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
      export const render = view => renderToStaticMarkup(React.createElement(CoverLetterListContent, {view}));
      export const selectFirst = view => {
        let selected;
        const tree = CoverLetterListContent({view, onOpen: id => {selected = id}});
        const list = tree.props.children.find(child => child.type === 'ul');
        list.props.children[0].props.children.find(child => child.type === 'button').props.onClick();
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
const {render, selectFirst} = createRequire(import.meta.url)(filename)
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
    totalElements: 21,
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
