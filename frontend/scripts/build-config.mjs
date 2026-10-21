import { copyFile, mkdir } from 'node:fs/promises'

export const buildOptions = {
  entryPoints: ['src/main.jsx'],
  bundle: true,
  outdir: 'dist/assets',
  entryNames: 'app',
  assetNames: '[name]',
  format: 'esm',
  sourcemap: true,
  minify: true,
  jsx: 'automatic',
  loader: { '.jsx': 'jsx' }
}

export async function copyIndex() {
  await mkdir('dist', { recursive: true })
  await copyFile('index.html', 'dist/index.html')
}
