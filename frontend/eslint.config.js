import stylistic from '@stylistic/eslint-plugin';

export default [
  {
    files: ['**/*.jsx'],
    languageOptions: {
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: { '@stylistic': stylistic },
    rules: {
      '@stylistic/jsx-one-expression-per-line': ['error', { allow: 'literal' }],
      '@stylistic/indent': ['error', 2],
      '@stylistic/jsx-indent-props': ['error', 2],
      '@stylistic/jsx-closing-tag-location': 'error',
    },
  },
];
