import js from '@eslint/js';
import globals from 'globals';

/* Flat ESLint config for the Threadboard PWA.
   app.js / sw.js are classic browser/service-worker scripts;
   tools/*.mjs are Node ES modules. */
export default [
  js.configs.recommended,
  {
    files: ['app.js', 'sw.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'script',
      globals: { ...globals.browser, ...globals.serviceworker },
    },
    rules: {
      'no-unused-vars': ['error', { args: 'none', caughtErrors: 'none' }],
    },
  },
  {
    files: ['tools/**/*.mjs'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.node },
    },
    rules: {
      'no-unused-vars': ['error', { args: 'none', caughtErrors: 'none' }],
    },
  },
];
