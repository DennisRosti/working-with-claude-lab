const fs = require('fs');
const path = require('path');
const { loadApp, requireApp, APP_PATH } = require('./setup/loadApp');

const CSS_PATH = path.resolve(__dirname, '../../main/resources/static/style.css');
const STORAGE_KEY = 'ops-dashboard.theme';
const COLOUR_LITERAL = /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(/;

function html() {
  return document.documentElement;
}

/** Custom property names declared in the rule whose selector contains `selector`. */
function variablesIn(css, selector) {
  const names = new Set();
  const re = /([^{}]+)\{([^}]*)\}/g;
  let match;
  while ((match = re.exec(css)) !== null) {
    if (match[1].includes(selector)) {
      (match[2].match(/--[\w-]+(?=\s*:)/g) || []).forEach((n) => names.add(n));
    }
  }
  return names;
}

describe('theme toggle (TODO-231)', () => {
  describe('AC-1: a toggle button in the header', () => {
    test('#theme-toggle sits inside the header', async () => {
      const { document } = await loadApp();
      const button = document.getElementById('theme-toggle');
      expect(button).not.toBeNull();
      expect(document.getElementById('app-header').contains(button)).toBe(true);
    });

    test('clicking switches the theme, clicking again switches it back', async () => {
      const { document } = await loadApp();
      const button = document.getElementById('theme-toggle');
      expect(html().getAttribute('data-theme')).toBe('dark');
      button.click();
      expect(html().getAttribute('data-theme')).toBe('light');
      button.click();
      expect(html().getAttribute('data-theme')).toBe('dark');
    });

    test('the label names the theme you get on click', async () => {
      const { document } = await loadApp();
      const button = document.getElementById('theme-toggle');
      expect(button.textContent).toBe('Light theme');
      button.click();
      expect(button.textContent).toBe('Dark theme');
    });
  });

  describe('AC-2: data-theme on <html> and CSS variables, charts included', () => {
    test('app.js contains no colour literals', () => {
      expect(fs.readFileSync(APP_PATH, 'utf8')).not.toMatch(COLOUR_LITERAL);
    });

    test('chart bars and labels carry no inline colour, so CSS decides', async () => {
      const { document } = await loadApp();
      const marks = document.querySelectorAll('#chart-on-time *, #chart-tickets *');
      expect(marks.length).toBeGreaterThan(0);
      marks.forEach((el) => {
        expect(el.hasAttribute('fill')).toBe(false);
        expect(el.hasAttribute('style')).toBe(false);
      });
    });

    test('style.css has colour literals only in custom property declarations', () => {
      const offending = fs.readFileSync(CSS_PATH, 'utf8')
        .split('\n')
        .filter((line) => COLOUR_LITERAL.test(line) && !/^\s*--[\w-]+\s*:/.test(line));
      expect(offending).toEqual([]);
    });

    test('the light and dark themes define the same variables, chart colours included', () => {
      const css = fs.readFileSync(CSS_PATH, 'utf8');
      const light = variablesIn(css, '[data-theme="light"]');
      const dark = variablesIn(css, '[data-theme="dark"]');
      expect(light.size).toBeGreaterThan(0);
      expect([...dark].sort()).toEqual([...light].sort());
      ['--chart-bar', '--chart-bar-warn', '--chart-label', '--chart-value'].forEach((name) => {
        expect(dark.has(name)).toBe(true);
      });
    });
  });

  describe('AC-3: the choice survives a refresh', () => {
    test('a click stores the theme in localStorage', async () => {
      const { document } = await loadApp();
      document.getElementById('theme-toggle').click();
      expect(window.localStorage.getItem(STORAGE_KEY)).toBe('light');
    });

    test('a stored theme is restored on load', async () => {
      const { document } = await loadApp({ storedTheme: 'light' });
      expect(html().getAttribute('data-theme')).toBe('light');
      expect(document.getElementById('theme-toggle').textContent).toBe('Dark theme');
    });

    test('an unknown stored value is ignored and never reaches the page', async () => {
      await loadApp({ storedTheme: '"><img src=x onerror=alert(1)>' });
      expect(html().getAttribute('data-theme')).toBe('dark');
    });

    test('the page still works when localStorage throws', async () => {
      const get = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('denied'); });
      const set = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('denied'); });
      try {
        const { document } = await loadApp();
        expect(html().getAttribute('data-theme')).toBe('dark');
        document.getElementById('theme-toggle').click();
        expect(html().getAttribute('data-theme')).toBe('light');
      } finally {
        get.mockRestore();
        set.mockRestore();
      }
    });
  });

  describe('AC-4: dark by default, whatever the OS says', () => {
    test('with nothing stored the theme is dark, even when the OS prefers light', async () => {
      window.matchMedia = jest.fn(() => ({ matches: true, addListener() {}, removeListener() {} }));
      try {
        await loadApp();
        expect(html().getAttribute('data-theme')).toBe('dark');
      } finally {
        delete window.matchMedia;
      }
    });

    test('resolveTheme keeps light and dark and falls back to dark for anything else', () => {
      const { resolveTheme } = requireApp();
      expect(resolveTheme('light')).toBe('light');
      expect(resolveTheme('dark')).toBe('dark');
      [null, undefined, '', 'Dark', 'system', 'light ', '<script>'].forEach((value) => {
        expect(resolveTheme(value)).toBe('dark');
      });
    });
  });
});
