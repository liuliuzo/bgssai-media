import assert from 'node:assert/strict';
import test from 'node:test';
import {
  applyShellDocumentFlags,
  detectShellMode,
  detectShellModeFromLocation,
  isTruthyFlag,
  persistShellMode,
  SHELL_DOCUMENT_CLASS,
  STANDALONE_SHELL_MODE,
} from './detectShell.ts';

test('truthy flags accept 1/true/yes', () => {
  assert.equal(isTruthyFlag('1'), true);
  assert.equal(isTruthyFlag('true'), true);
  assert.equal(isTruthyFlag('YES'), true);
  assert.equal(isTruthyFlag('0'), false);
  assert.equal(isTruthyFlag(''), false);
  assert.equal(isTruthyFlag(null), false);
});

test('bgssai_shell=1 enters shell mode', () => {
  const mode = detectShellMode({ search: '?bgssai_shell=1' });
  assert.deepEqual(mode, { inShell: true, bgssaiShell: true, chatPane: false });
});

test('chat_pane=1 enters shell mode', () => {
  const mode = detectShellMode({ search: 'chat_pane=1' });
  assert.deepEqual(mode, { inShell: true, bgssaiShell: false, chatPane: true });
});

test('desktop standalone stays unchanged without flags', () => {
  const mode = detectShellMode({ search: '', hash: '' });
  assert.deepEqual(mode, STANDALONE_SHELL_MODE);
});

test('session storage keeps shell after SPA navigation', () => {
  const mode = detectShellModeFromLocation({ search: '' }, {
    getItem: (key: string) => (key === 'bgssai_shell' ? '1' : null),
  });
  assert.equal(mode.inShell, true);
  assert.equal(mode.bgssaiShell, true);
});

test('hash query is also accepted', () => {
  const mode = detectShellMode({ hash: '#/shorts?chat_pane=1' });
  assert.equal(mode.inShell, true);
  assert.equal(mode.chatPane, true);
});

test('explicit 0 exits shell even if storage was on', () => {
  const mode = detectShellMode({
    search: '?bgssai_shell=0',
    storedShell: '1',
    storedChatPane: '1',
  });
  assert.deepEqual(mode, STANDALONE_SHELL_MODE);
});

test('persist and document flags write no secrets', () => {
  const store: Record<string, string> = {};
  persistShellMode({ inShell: true, bgssaiShell: true, chatPane: true }, {
    setItem: (k, v) => {
      store[k] = v;
    },
    removeItem: (k) => {
      delete store[k];
    },
  });
  assert.deepEqual(store, { bgssai_shell: '1', chat_pane: '1' });

  const attrs: Record<string, string> = {};
  const classes = new Set<string>();
  applyShellDocumentFlags({ inShell: true, bgssaiShell: true, chatPane: true }, {
    classList: {
      add: (c) => {
        classes.add(c);
      },
      remove: (c) => {
        classes.delete(c);
      },
    },
    setAttribute: (k, v) => {
      attrs[k] = v;
    },
    removeAttribute: (k) => {
      delete attrs[k];
    },
  });
  assert.equal(classes.has(SHELL_DOCUMENT_CLASS), true);
  assert.equal(attrs['data-bgssai-shell'], '1');
  assert.equal(attrs['data-chat-pane'], '1');
});
