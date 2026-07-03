/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
import { LitElement, html, css, type PropertyValues } from 'lit';

/**
 * A lightweight, dependency-free YAML editor with syntax highlighting.
 *
 * A native <textarea> is layered, transparent, on top of a <pre> that mirrors the same
 * text tokenised into coloured spans. The textarea keeps caret, selection, undo and IME
 * behaviour; the <pre> underneath supplies the colours. Both layers share identical font
 * metrics and padding so they stay pixel-aligned, and the highlight layer follows the
 * textarea's scroll offset.
 *
 * Colours are drawn from the app's Aura theme tokens, so they adapt to light/dark mode.
 * Exposed properties `value` (two-way, fires `value-changed`) and `readonly` are driven
 * from the Vaadin Java wrapper (org.karnak.frontend.profile.component.editprofile.YamlCodeEditor).
 */
class KarnakYamlEditor extends LitElement {
  value = '';

  readonly = false;

  static get properties() {
    return {
      value: { type: String },
      readonly: { type: Boolean, reflect: true },
    };
  }

  static styles = css`
    :host {
      display: block;
      width: 100%;
      height: 100%;
      min-height: 0;

      /* Syntax palette — override any of these from the outside to re-theme.
       * Defaults are built from Aura tokens so they flip with the app theme. */
      --yaml-key-color: var(--aura-accent-text-color, #2160c4);
      --yaml-string-color: var(--aura-green, #2a8c4a);
      --yaml-number-color: var(--aura-orange, #b46a00);
      --yaml-bool-color: var(--aura-red-text, #c0392b);
      --yaml-anchor-color: var(--aura-primary-color, #6a4bcc);
      --yaml-comment-color: var(--vaadin-text-color-secondary, #6b7280);
      --yaml-punct-color: var(--vaadin-text-color-secondary, #6b7280);
      --yaml-text-color: var(--vaadin-text-color, #1a1a1a);
    }

    .editor {
      position: relative;
      width: 100%;
      height: 100%;
      min-height: 0;
      overflow: hidden;
      border: 1px solid
        var(--vaadin-input-field-border-color, color-mix(in srgb, var(--vaadin-text-color, #000) 25%, transparent));
      border-radius: var(--vaadin-radius-m, 6px);
      background: var(--vaadin-input-field-background, var(--aura-surface-color, #fff));
    }

    :host([readonly]) .editor {
      opacity: 0.85;
    }

    /* The two stacked layers must be geometrically identical. */
    .layer {
      margin: 0;
      border: 0;
      padding: 10px 12px;
      box-sizing: border-box;
      position: absolute;
      inset: 0;
      font-family: var(
        --vaadin-code-font-family,
        ui-monospace,
        'SF Mono',
        'SFMono-Regular',
        'Menlo',
        'Consolas',
        'Liberation Mono',
        monospace
      );
      font-size: 13px;
      line-height: 1.5;
      letter-spacing: normal;
      tab-size: 2;
      white-space: pre;
      word-break: normal;
      overflow-wrap: normal;
    }

    pre.highlight {
      overflow: hidden;
      z-index: 0;
      color: var(--yaml-text-color);
      pointer-events: none;
    }

    pre.highlight code {
      font: inherit;
    }

    textarea {
      overflow: auto;
      z-index: 1;
      resize: none;
      background: transparent;
      color: transparent;
      caret-color: var(--vaadin-text-color, #1a1a1a);
      -webkit-text-fill-color: transparent;
      outline: none;
    }

    textarea::selection {
      background: color-mix(in srgb, var(--aura-primary-color, #6a4bcc) 28%, transparent);
    }

    /* Token colours */
    .tok-key {
      color: var(--yaml-key-color);
    }
    .tok-string {
      color: var(--yaml-string-color);
    }
    .tok-number {
      color: var(--yaml-number-color);
    }
    .tok-bool {
      color: var(--yaml-bool-color);
      font-weight: 600;
    }
    .tok-anchor {
      color: var(--yaml-anchor-color);
    }
    .tok-comment {
      color: var(--yaml-comment-color);
      font-style: italic;
    }
    .tok-punct {
      color: var(--yaml-punct-color);
    }
    .tok-marker {
      color: var(--yaml-punct-color);
      font-weight: 600;
    }
    .tok-value {
      color: var(--yaml-text-color);
    }
  `;

  render() {
    return html`
      <div class="editor">
        <pre class="layer highlight" aria-hidden="true"><code></code></pre>
        <textarea
          class="layer"
          part="input"
          spellcheck="false"
          autocomplete="off"
          autocapitalize="off"
          wrap="off"
          ?readonly=${this.readonly}
          @input=${this.onInput}
          @scroll=${this.syncScroll}
          @keydown=${this.onKeyDown}
        ></textarea>
      </div>
    `;
  }

  private get textarea(): HTMLTextAreaElement {
    return this.renderRoot.querySelector('textarea') as HTMLTextAreaElement;
  }

  private get code(): HTMLElement {
    return this.renderRoot.querySelector('pre.highlight code') as HTMLElement;
  }

  firstUpdated() {
    this.textarea.value = this.value ?? '';
    this.paint();
  }

  updated(changed: PropertyValues) {
    // Only push an external `value` (e.g. set from the server) into the textarea; skip
    // when the change originated from user typing, to avoid resetting the caret position.
    if (changed.has('value') && this.textarea && this.textarea.value !== (this.value ?? '')) {
      this.textarea.value = this.value ?? '';
      this.paint();
      this.syncScroll();
    }
  }

  private onInput() {
    this.value = this.textarea.value;
    this.paint();
    this.dispatchEvent(new CustomEvent('value-changed', { detail: { value: this.value } }));
  }

  private onKeyDown(event: KeyboardEvent) {
    // Insert two spaces on Tab so indentation stays fluent; Shift+Tab still moves focus out.
    if (event.key === 'Tab' && !event.shiftKey && !this.readonly) {
      event.preventDefault();
      const ta = this.textarea;
      const start = ta.selectionStart;
      const end = ta.selectionEnd;
      ta.setRangeText('  ', start, end, 'end');
      this.onInput();
    }
  }

  private syncScroll() {
    const pre = this.renderRoot.querySelector('pre.highlight') as HTMLElement;
    const ta = this.textarea;
    if (pre && ta) {
      pre.scrollTop = ta.scrollTop;
      pre.scrollLeft = ta.scrollLeft;
    }
  }

  private paint() {
    this.code.innerHTML = highlightYaml(this.textarea.value ?? '');
  }
}

/* ===================== YAML tokeniser ===================== */

function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function span(cls: string, text: string): string {
  return `<span class="tok-${cls}">${escapeHtml(text)}</span>`;
}

function highlightYaml(source: string): string {
  return source.split('\n').map(highlightLine).join('\n');
}

function highlightLine(line: string): string {
  const indent = (line.match(/^\s*/) as RegExpMatchArray)[0];
  const rest = line.slice(indent.length);

  if (rest === '') {
    return escapeHtml(line);
  }
  // Whole-line comment.
  if (rest.startsWith('#')) {
    return escapeHtml(indent) + span('comment', rest);
  }
  // Document markers.
  if (rest === '---' || rest === '...') {
    return escapeHtml(indent) + span('marker', rest);
  }

  const { content, comment } = splitTrailingComment(rest);
  let out = escapeHtml(indent);
  let body = content;

  // Sequence markers ("- " possibly repeated for nested inline sequences).
  let dash: RegExpMatchArray | null;
  while ((dash = body.match(/^(-\s+)/))) {
    out += span('punct', dash[1]);
    body = body.slice(dash[1].length);
  }
  if (body === '-') {
    return out + span('punct', '-') + (comment ? span('comment', comment) : '');
  }

  const kv = matchKeyValue(body);
  if (kv) {
    out +=
      span('key', kv.key) +
      escapeHtml(kv.preColon) +
      span('punct', ':') +
      escapeHtml(kv.sep) +
      highlightValue(kv.value);
  } else {
    out += highlightValue(body);
  }

  if (comment) {
    out += span('comment', comment);
  }
  return out;
}

/** Split an unquoted trailing `# comment` (a `#` at start of token or after whitespace). */
function splitTrailingComment(s: string): { content: string; comment: string } {
  let inSingle = false;
  let inDouble = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === "'" && !inDouble) {
      inSingle = !inSingle;
    } else if (c === '"' && !inSingle) {
      inDouble = !inDouble;
    } else if (c === '#' && !inSingle && !inDouble && (i === 0 || /\s/.test(s[i - 1]))) {
      return { content: s.slice(0, i), comment: s.slice(i) };
    }
  }
  return { content: s, comment: '' };
}

/** Split `key: value` where the colon is a real mapping colon (followed by space or EOL). */
function matchKeyValue(body: string): { key: string; preColon: string; sep: string; value: string } | null {
  const m = body.match(/^("[^"]*"|'[^']*'|[^:#]+?)(\s*):(\s[\s\S]*|)$/);
  if (!m) {
    return null;
  }
  const after = m[3]; // '' or leading-whitespace + value
  const sepMatch = after.match(/^(\s*)([\s\S]*)$/) as RegExpMatchArray;
  return { key: m[1], preColon: m[2], sep: sepMatch[1], value: sepMatch[2] };
}

function highlightValue(value: string): string {
  if (value === '') {
    return '';
  }
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length > 1) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length > 1)
  ) {
    return span('string', value);
  }
  if (/^(true|false|yes|no|on|off|null|~)$/i.test(trimmed)) {
    return span('bool', value);
  }
  if (/^[-+]?(\d+\.?\d*|\.\d+)([eE][-+]?\d+)?$/.test(trimmed)) {
    return span('number', value);
  }
  if (/^[&*!][^\s]+$/.test(trimmed)) {
    return span('anchor', value);
  }
  return span('value', value);
}

customElements.define('karnak-yaml-editor', KarnakYamlEditor);