import { build } from 'esbuild'
import { buildOptions, copyIndex } from './build-config.mjs'

await copyIndex()
await build(buildOptions)
console.log('React build complete: dist/')
