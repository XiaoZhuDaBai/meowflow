// Generator script: read builtinTemplates.ts and emit matching Java
// into BuiltinTemplateCatalog.java to guarantee FE/BE parity.
//
// v2: full rewrite focused on correctness across all 17 templates.
//
// Usage: node scripts/regen-builtin-catalog.js

const fs = require('fs');
const path = require('path');

const TS_FILE = path.resolve(
  __dirname,
  '../frontend/meowflow-ui/src/mock/builtinTemplates.ts'
);
const JAVA_FILE = path.resolve(
  __dirname,
  '../backend/meowflow/meowflow-template/src/main/java/com/meowflow/template/catalog/BuiltinTemplateCatalog.java'
);

// 行业标识 → 数字（用于 setScene / setCategoryId 时回退）
const INDUSTRY = {
  cs: '"service"',
  hr: '"hr"',
  ops: '"ops"',
  finance: '"finance"',
  general: '"general"',
};

const CATEGORY_ID = {
  cs: '1L',
  hr: '2L',
  ops: '3L',
  finance: '4L',
  general: '5L',
};

const ts = fs.readFileSync(TS_FILE, 'utf8');

// ============================================================================
// 1) Parse each template function body
// ============================================================================

function extractTemplates() {
  const out = [];
  const fnRe = /function\s+(\w+)\(\)\s*:\s*BuiltinTemplate\s*\{/g;
  let m;
  while ((m = fnRe.exec(ts))) {
    const name = m[1];
    const start = m.index + m[0].length;
    let depth = 1;
    let i = start;
    while (i < ts.length && depth > 0) {
      const c = ts[i];
      if (c === '{') depth++;
      else if (c === '}') depth--;
      i++;
    }
    const body = ts.slice(start, i - 1);
    out.push({ name, body });
  }
  return out;
}

// ============================================================================
// 2) Generic JS top-level scanner. Returns tokens (atoms/braces) and supports
//    strings (single/double), template literals, comments.
// ============================================================================

function tokenize(s, options = {}) {
  const toks = [];
  let i = 0;
  while (i < s.length) {
    const c = s[i];
    // Skip line comments
    if (c === '/' && s[i + 1] === '/') {
      while (i < s.length && s[i] !== '\n') i++;
      continue;
    }
    if (c === '/' && s[i + 1] === '*') {
      i += 2;
      while (i < s.length && !(s[i] === '*' && s[i + 1] === '/')) i++;
      i += 2;
      continue;
    }
    // String literals
    if (c === '"' || c === "'") {
      const quote = c;
      let j = i + 1;
      while (j < s.length) {
        if (s[j] === '\\') {
          j += 2;
          continue;
        }
        if (s[j] === quote) {
          j++;
          break;
        }
        j++;
      }
      toks.push({ type: 'string', value: s.slice(i, j), start: i, end: j });
      i = j;
      continue;
    }
    // Template literals (backticks) — rough handling; skip unless needed
    if (c === '`') {
      let j = i + 1;
      while (j < s.length && s[j] !== '`') {
        if (s[j] === '\\') j += 2;
        else j++;
      }
      j++;
      toks.push({ type: 'template', value: s.slice(i, j), start: i, end: j });
      i = j;
      continue;
    }
    // Punctuation
    if ('{[()]}'.includes(c)) {
      toks.push({ type: 'punct', value: c, start: i, end: i + 1 });
      i++;
      continue;
    }
    if (c === ',' || c === ';' || c === ':') {
      toks.push({ type: 'punct', value: c, start: i, end: i + 1 });
      i++;
      continue;
    }
    // Identifier / number
    if (/[a-zA-Z_$]/.test(c)) {
      let j = i;
      while (j < s.length && /[a-zA-Z0-9_$]/.test(s[j])) j++;
      toks.push({ type: 'ident', value: s.slice(i, j), start: i, end: j });
      i = j;
      continue;
    }
    if (/[-0-9.]/.test(c)) {
      let j = i;
      while (j < s.length && /[0-9.eE+\-]/.test(s[j])) j++;
      toks.push({ type: 'number', value: s.slice(i, j), start: i, end: j });
      i = j;
      continue;
    }
    // Other operators / whitespace / etc.
    if (/\s/.test(c)) {
      i++;
      continue;
    }
    // Operators
    let j = i;
    while (j < s.length && !/[\s,;:{}\[\]()"'`]/.test(s[j]) && !/[\/]/i.test(s[j])) j++;
    toks.push({ type: 'other', value: s.slice(i, j), start: i, end: j });
    i = j;
  }
  return toks;
}

// Generic splitter at top level, returning string slices between commas.
function splitTopLevelByComma(s) {
  const toks = tokenize(s);
  const parts = [];
  let cur = '';
  let depth = 0;
  for (const t of toks) {
    if (t.type === 'punct' && t.value === ',' && depth === 0) {
      parts.push(cur);
      cur = '';
      continue;
    }
    if (t.type === 'punct' && /[{\[\(]/.test(t.value)) {
      depth++;
    } else if (t.type === 'punct' && /[}\]\)]/.test(t.value)) {
      depth--;
    }
    cur += s.slice(t.start, t.end);
  }
  if (cur.trim()) parts.push(cur);
  return parts.map((p) => p.trim()).filter(Boolean);
}

// JS string literal → Java string literal (matches '\'' quote and escapes).
function jsStringToJava(literal) {
  // input includes surrounding quotes
  if (literal[0] === '"' || literal[0] === "'") {
    let inner = literal.slice(1, -1);
    // We must allow \n, \t, \"  etc. in Java as well; most escape patterns transfer verbatim.
    // The only thing is that a single quote inside a single-quoted JS string needs
    // escaping. For our templates, all strings contain either CN chars or simple
    // ASCII paths — no embedded quotes — so simple unescaping is enough.
    const escaped = inner
      .replace(/\\"/g, '\\"')
      .replace(/\\'/g, "\\'")
      .replace(/\\\\/g, '\\\\');
    return JSON.stringify(escaped);
  }
  return literal;
}

// Convert a TS expression to a Java expression.
// We support: string, number, boolean, null, undefined, [array], {object}
// Caller wraps with appropriate types.
function tsExprToJava(expr) {
  let s = expr.trim();

  // Array literal → List.of(...)
  if (s.startsWith('[') && s.endsWith(']')) {
    const inner = s.slice(1, -1).trim();
    if (!inner) return 'List.of()';
    const items = splitTopLevelByComma(inner);
    return `List.of(${items.map(tsExprToJava).join(', ')})`;
  }

  // Object literal → Map.ofEntries(Map.entry(...), ...)
  if (s.startsWith('{') && s.endsWith('}')) {
    const inner = s.slice(1, -1).trim();
    if (!inner) return 'Map.of()';
    const items = splitTopLevelByComma(inner);
    const kv = items.map((entry) => {
      // First colon at top level → split key & value
      const colonIdx = findTopLevelColon(entry);
      const key = entry.slice(0, colonIdx).trim();
      const val = entry.slice(colonIdx + 1).trim();
      let keyLiteral;
      if ((key.startsWith('"') && key.endsWith('"')) || (key.startsWith("'") && key.endsWith("'"))) {
        keyLiteral = jsStringToJava(key);
      } else {
        // raw identifier → wrap as string
        keyLiteral = JSON.stringify(key);
      }
      const valJava = tsExprToJava(val);
      return `Map.entry(${keyLiteral}, ${valJava})`;
    });
    return `Map.ofEntries(${kv.join(', ')})`;
  }

  // Undefined → null (Java)
  if (s === 'undefined') return 'null';

  // Strings (any quote)
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    // Already contains the right chars
    return jsStringToJava(s);
  }

  // Numbers
  if (/^-?\d+(\.\d+)?(e[-+]?\d+)?$/i.test(s)) return s;

  // Booleans
  if (s === 'true' || s === 'false') return s;

  // null
  if (s === 'null') return 'null';

  // JS Template literal `${...}` — keep as Java string literal after escape
  if (s.startsWith('`') && s.endsWith('`')) {
    // Best-effort: strip backticks, replace ${...} with quoted strings
    let inner = s.slice(1, -1);
    return JSON.stringify(inner); // not perfect but templates don't appear in node configs
  }

  // Bare identifier: treat as identifier (e.g. variable reference) — pass through.
  return s;
}

function findTopLevelColon(s) {
  const toks = tokenize(s);
  let depth = 0;
  for (const t of toks) {
    if (t.type === 'punct' && t.value === ':' && depth === 0) return t.start;
    if (t.type === 'punct' && /[{\[\(]/.test(t.value)) depth++;
    if (t.type === 'punct' && /[}\]\)]/.test(t.value)) depth--;
  }
  return -1;
}

// Convert a TS `n('id', 'type', 'name', x, y, 'cat', 'desc'?, {config})` to Java.
function convertNodeCall(call) {
  // strip leading `n(` and trailing `)`
  let inner = call.replace(/^\s*n\(/, '').replace(/\)\s*$/, '');
  const parts = splitTopLevelByComma(inner);
  if (parts.length < 6) {
    throw new Error('node call too short: ' + call);
  }
  const [idExpr, typeExpr, nameExpr, xExpr, yExpr, catExpr, ...rest] = parts;
  const id = jsStringToJava(idExpr);
  const type = jsStringToJava(typeExpr);
  const name = jsStringToJava(nameExpr);
  const x = xExpr;
  const y = yExpr;
  const cat = catExpr === 'undefined' ? 'null' : jsStringToJava(catExpr);

  let desc = 'null';
  let cfgExpr = 'Map.of()';
  if (rest.length >= 1) {
    if (rest[0] !== 'undefined') {
      if ((rest[0].startsWith('"') && rest[0].endsWith('"')) || (rest[0].startsWith("'") && rest[0].endsWith("'"))) {
        desc = jsStringToJava(rest[0]);
      } else if (rest[0].startsWith('{') && rest[0].endsWith('}')) {
        cfgExpr = tsExprToJava(rest[0]);
      }
    }
    if (rest.length >= 2 && rest[1] !== 'undefined') {
      cfgExpr = tsExprToJava(rest[1]);
    }
  }

  return `node(${id}, ${type}, ${name}, ${x}, ${y}, ${cat}, ${desc}, ${cfgExpr})`;
}

function convertEdgeCall(call) {
  // leading e(/eCondition(/...
  const m = call.match(/^(e|eCondition|eLoop|eError)\(([\s\S]*)\)$/);
  if (!m) throw new Error('bad edge call: ' + call);
  const fn = m[1];
  let inner = m[2];
  const parts = splitTopLevelByComma(inner);
  const [idExpr, srcExpr, tgtExpr, labelRaw] = parts;
  const id = jsStringToJava(idExpr);
  const src = jsStringToJava(srcExpr);
  const tgt = jsStringToJava(tgtExpr);
  const label = labelRaw && labelRaw !== 'undefined' ? jsStringToJava(labelRaw) : null;

  if (fn === 'e') {
    return label
      ? `edge(${id}, ${src}, ${tgt}, ${label})`
      : `edge(${id}, ${src}, ${tgt})`;
  }
  if (fn === 'eCondition') {
    return `edgeCondition(${id}, ${src}, ${tgt}, ${label || 'null'})`;
  }
  if (fn === 'eLoop') {
    return `edgeLoop(${id}, ${src}, ${tgt}, ${label || 'null'})`;
  }
  if (fn === 'eError') {
    return `edgeError(${id}, ${src}, ${tgt}, ${label || 'null'})`;
  }
  throw new Error('Unknown edge helper: ' + fn);
}

// ============================================================================
// 3) Helpers to extract data from a body
// ============================================================================

function findBalancedBlock(src, openIdx, openCh, closeCh) {
  let depth = 0;
  const toks = tokenize(src);
  // Walk token-wise, but we want character indices
  let ci = openIdx;
  while (ci < src.length) {
    const c = src[ci];
    if (c === openCh) depth++;
    else if (c === closeCh) {
      depth--;
      if (depth === 0) return ci;
    }
    ci++;
  }
  return -1;
}

function extractNodesBody(body) {
  const m = body.match(/const\s+nodes\s*:\s*BuiltinNode\[\]\s*=\s*\[/);
  if (!m) throw new Error('nodes literal not found');
  const start = m.index + m[0].length;
  // Find matching ]
  let depth = 1;
  const toks = tokenize(body);
  for (const t of toks) {
    if (t.start < start) continue;
    if (t.type === 'punct' && t.value === '[') depth++;
    if (t.type === 'punct' && t.value === ']') {
      depth--;
      if (depth === 0) {
        return body.slice(start, t.start).trim();
      }
    }
  }
  throw new Error('nodes closing ] not found');
}

function extractEdgesBody(body) {
  const m = body.match(/const\s+edges\s*:\s*BuiltinEdge\[\]\s*=\s*\[/);
  if (!m) throw new Error('edges literal not found');
  const start = m.index + m[0].length;
  let depth = 1;
  const toks = tokenize(body);
  for (const t of toks) {
    if (t.start < start) continue;
    if (t.type === 'punct' && t.value === '[') depth++;
    if (t.type === 'punct' && t.value === ']') {
      depth--;
      if (depth === 0) {
        return body.slice(start, t.start).trim();
      }
    }
  }
  throw new Error('edges closing ] not found');
}

function extractEntryCall(body) {
  const m = body.match(/return\s+entry\(/);
  if (!m) throw new Error('entry() not found');
  const start = m.index + m[0].length;
  // Walk tokens from start to find balanced close paren
  const toks = tokenize(body);
  let depth = 1;
  for (const t of toks) {
    if (t.start < start) continue;
    if (t.type === 'punct' && t.value === '(') depth++;
    if (t.type === 'punct' && t.value === ')') {
      depth--;
      if (depth === 0) {
        return body.slice(start, t.start).trim();
      }
    }
  }
  throw new Error('entry closing paren not found');
}

// Extract every "n(...)" call that is balanced, anywhere in a string.
function extractAllCalls(s) {
  const calls = [];
  const toks = tokenize(s);
  for (let i = 0; i < toks.length; i++) {
    const t = toks[i];
    if (t.type !== 'ident') continue;
    if (!/^(n|e|eCondition|eLoop|eError)$/.test(t.value)) continue;
    // Look at next token — must be '('
    const next = toks[i + 1];
    if (!next || next.type !== 'punct' || next.value !== '(') continue;
    // Walk to find matching ')'
    let depth = 1;
    let j = i + 2;
    while (j < toks.length && depth > 0) {
      if (toks[j].type === 'punct' && toks[j].value === '(') depth++;
      else if (toks[j].type === 'punct' && toks[j].value === ')') depth--;
      j++;
    }
    if (depth !== 0) continue;
    const call = s.slice(t.start, toks[j - 1].end);
    calls.push({ fn: t.value, call });
    i = j - 1;
  }
  return calls;
}

function parseEntryMeta(entryCallRaw) {
  const parts = splitTopLevelByComma(entryCallRaw);
  // expected 11 args
  if (parts.length !== 11) {
    throw new Error(`entry arg count = ${parts.length}; want 11. raw=${entryCallRaw.slice(0, 200)}`);
  }
  const [idExpr, nameExpr, descExpr, catExpr, catNameExpr, emojiExpr, usageExpr, ratingExpr, tagsExpr] = parts;
  return {
    id: jsStringToJava(idExpr),
    name: jsStringToJava(nameExpr),
    desc: jsStringToJava(descExpr),
    cat: jsStringToJava(catExpr), // Java string literal, e.g. "cs"
    catName: jsStringToJava(catNameExpr),
    emoji: jsStringToJava(emojiExpr),
    usage: usageExpr,
    rating: ratingExpr,
    tags: tsExprToJava(tagsExpr),
  };
}

// ============================================================================
// MAIN
// ============================================================================

const templates = extractTemplates();
console.error(`Extracted ${templates.length} template functions.`);

const javaOrig = fs.readFileSync(JAVA_FILE, 'utf8');

const sceneStartIdx = javaOrig.indexOf('// 场景：智能客服');
if (sceneStartIdx < 0) throw new Error('Java scene marker not found');
// Skip any preceding banner blocks generated by previous runs.
let headEnd = sceneStartIdx;
while (headEnd > 0) {
  const lineEnd = javaOrig[headEnd - 1] === '\n' ? headEnd - 1 : headEnd;
  const lineStart = javaOrig.lastIndexOf('\n', lineEnd - 1) + 1;
  const line = javaOrig.slice(lineStart, lineEnd).trim();
  if (line === '' || line.includes('// ====') || line.includes('// 重新生成')) {
    headEnd = lineStart;
    continue;
  }
  break;
}
const lastBrace = javaOrig.lastIndexOf('}');
const tailAfterScenes = javaOrig.slice(lastBrace);
const headSegment = javaOrig.slice(0, headEnd);

const SCENES = [
  { name: 'registerCustomerServiceScenarios', title: '智能客服', fns: ['csAutoReply', 'csTicketRoute', 'csFeedbackAnalysis'] },
  { name: 'registerHRScenarios', title: 'HR', fns: ['hrResumeScreen', 'hrLeaveApprove', 'hrOnboarding'] },
  { name: 'registerOpsScenarios', title: '运营', fns: ['opsMeetingNotes', 'opsDailyReport', 'opsProductCopy', 'opsDataWeekly'] },
  { name: 'registerFinanceScenarios', title: '财务', fns: ['financeInvoiceOcr', 'financeReimburse', 'financeArReminder'] },
  { name: 'registerGeneralAndDataScenarios', title: '通用 / 数据', fns: ['generalDataSync', 'generalWebhook', 'generalBackup', 'generalDataClean'] },
];

const sceneSrc = [];
sceneSrc.push(`    // =========================================================================`);
sceneSrc.push(`    // 重新生成的模板定义（由 scripts/regen-builtin-catalog.js 从 frontend builtinTemplates.ts 同步生成）`);
sceneSrc.push(`    // =========================================================================`);
sceneSrc.push('');

for (const scene of SCENES) {
  sceneSrc.push(`    // =========================================================================`);
  sceneSrc.push(`    // 场景：${scene.title}`);
  sceneSrc.push(`    // =========================================================================`);
  sceneSrc.push(`    private void ${scene.name}() {`);
  for (const fnName of scene.fns) {
    const tpl = templates.find((t) => t.name === fnName);
    if (!tpl) {
      console.error(`WARN: missing template ${fnName}`);
      continue;
    }
    const nodesBody = extractNodesBody(tpl.body);
    const edgesBody = extractEdgesBody(tpl.body);
    const meta = parseEntryMeta(extractEntryCall(tpl.body));

    const nodeCalls = extractAllCalls(nodesBody).filter((c) => c.fn === 'n').map((c) => c.call);
    const edgeCalls = extractAllCalls(edgesBody).filter((c) => c.fn !== 'n');

    sceneSrc.push(`        add(${meta.id}, ${meta.name},`);
    sceneSrc.push(`                ${meta.desc},`);
    sceneSrc.push(`                ${CATEGORY_ID[meta.cat.replace(/"/g, '')] || '5L'}, ${meta.catName}, ${meta.emoji},`);
    sceneSrc.push(`                ${meta.usage}, ${meta.rating}, ${INDUSTRY[meta.cat.replace(/"/g, '')] || '"general"'},`);
    sceneSrc.push(`                ${meta.tags},`);
    sceneSrc.push(`                List.of(`);
    for (const c of nodeCalls) {
      try {
        sceneSrc.push(`                        ${convertNodeCall(c)},`);
      } catch (e) {
        console.error(`WARN node: ${fnName} :: ${e.message}`);
      }
    }
    sceneSrc.push(`                ),`);
    sceneSrc.push(`                List.of(`);
    for (const c of edgeCalls) {
      try {
        sceneSrc.push(`                        ${convertEdgeCall(c.call)},`);
      } catch (e) {
        console.error(`WARN edge: ${fnName} :: ${e.message}`);
      }
    }
    sceneSrc.push(`                )`);
    sceneSrc.push(`        );`);
    sceneSrc.push('');
  }
  sceneSrc.push(`    }`);
  sceneSrc.push('');
}

const newJava = headSegment + sceneSrc.join('\n') + '\n' + tailAfterScenes;
fs.writeFileSync(JAVA_FILE, newJava, 'utf8');
console.error(`Wrote ${JAVA_FILE} (${newJava.length} bytes). Summary:`);
console.error(`  Nodes per template (count):`);
for (const scene of SCENES) {
  for (const fnName of scene.fns) {
    const tpl = templates.find((t) => t.name === fnName);
    if (!tpl) continue;
    const n = extractAllCalls(extractNodesBody(tpl.body)).filter((c) => c.fn === 'n').length;
    const e = extractAllCalls(extractEdgesBody(tpl.body)).filter((c) => c.fn !== 'n').length;
    console.error(`    ${fnName}: ${n} nodes, ${e} edges`);
  }
}
