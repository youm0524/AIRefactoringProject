import { createServer } from 'node:http'
import { readFile, stat } from 'node:fs/promises'
import { extname, resolve, sep } from 'node:path'
import { context } from 'esbuild'
import { buildOptions, copyIndex } from './build-config.mjs'

await copyIndex()
const build = await context({...buildOptions, minify: false})
// shortcut: 브라우저 자동 새로고침은 없음, HMR이 필요해지면 개발 서버를 교체한다.
await build.watch()

const root = resolve('dist')
const types = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.map':'application/json'}
const server = createServer(async (request, response) => {
  try {
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname)
    const file = resolve(root, `.${pathname === '/' ? '/index.html' : pathname}`)
    if (file !== root && !file.startsWith(`${root}${sep}`)) throw new Error('invalid path')
    if (!(await stat(file)).isFile()) throw new Error('not found')
    response.writeHead(200, {'Content-Type': types[extname(file)] || 'application/octet-stream'})
    response.end(await readFile(file))
  } catch {
    response.writeHead(404).end('Not found')
  }
})

server.listen(5173, () => console.log('Careerpick: http://localhost:5173'))
const close = async () => { server.close(); await build.dispose() }
process.once('SIGINT', close)
process.once('SIGTERM', close)
