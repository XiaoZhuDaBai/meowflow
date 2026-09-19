#!/usr/bin/env node
/**
 * MeowFlow 端到端冒烟测试
 *
 * 目的：不依赖浏览器，直接通过网关(8080)跑通「前端能点出来的那几条路」，
 *       用于验证"前端无法创建工作流"这类问题是否真的修好了。
 *
 * 覆盖路径：
 *   1. 登录                     POST /user/api/v1/auth/login
 *   2. 空白创建工作流            POST /workflow/api/workflow
 *   3. 保存版本（节点+连线）      POST /workflow/api/workflow/{id}/versions
 *   4. 发布版本                 POST /workflow/api/workflow/{id}/versions/publish
 *   5. 执行工作流               POST /workflow/api/execution
 *   6. 校验执行结果与节点记录     GET  /workflow/api/execution/{id}[/nodes]
 *   7. 模板模块创建工作流        GET  /template/api/template/search -> 用模板定义建工作流
 *   8. 清理测试数据
 *
 * 用法：
 *   node scripts/e2e-smoke.mjs
 *   BASE_URL=http://localhost:8080 node scripts/e2e-smoke.mjs
 *   KEEP=1 node scripts/e2e-smoke.mjs          # 不删除测试工作流，便于人工查看
 *
 * 退出码：0 = 全部通过；1 = 有失败
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const USERNAME = process.env.MF_USER || 'admin';
const PASSWORD = process.env.MF_PASS || 'meow@2026';
const KEEP = process.env.KEEP === '1';
const RUN_ID = Date.now().toString(36);

let token = null;
let passed = 0;
let failed = 0;
const failures = [];

// ---------------------------------------------------------------- 输出helpers
const C = {
  reset: '\u001b[0m',
  gray: '\u001b[90m',
  red: '\u001b[31m',
  green: '\u001b[32m',
  yellow: '\u001b[33m',
  cyan: '\u001b[36m',
};

function section(title) {
  console.log(`\n${C.cyan}=== ${title} ===${C.reset}`);
}

function ok(msg, detail) {
  passed += 1;
  console.log(`  ${C.green}✓${C.reset} ${msg}${detail ? ` ${C.gray}${detail}${C.reset}` : ''}`);
}

function bad(msg, detail) {
  failed += 1;
  failures.push(msg);
  console.log(`  ${C.red}✗${C.reset} ${msg}${detail ? ` ${C.gray}${detail}${C.reset}` : ''}`);
}

function info(msg) {
  console.log(`  ${C.gray}· ${msg}${C.reset}`);
}

// ---------------------------------------------------------------- HTTP 封装
/**
 * 网关返回统一信封 { code, message, data }；code===200 才算成功。
 * 这里保留信封以便断言 code，不做解包。
 */
async function api(method, path, body, { raw = false } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (e) {
    return { httpStatus: 0, code: 0, message: `网络错误: ${e.message}`, data: null };
  }

  const text = await res.text();
  if (raw) return { httpStatus: res.status, raw: text };

  let json;
  try {
    json = JSON.parse(text);
  } catch {
    return {
      httpStatus: res.status,
      code: 0,
      message: `响应不是合法 JSON: ${text.slice(0, 200)}`,
      data: null,
    };
  }
  return {
    httpStatus: res.status,
    code: typeof json.code === 'number' ? json.code : 0,
    message: json.message,
    data: json.data,
    envelope: json,
  };
}

function expectOk(label, r, detail) {
  if (!r || typeof r !== 'object') {
    bad(label, `调用未返回结果（r=${JSON.stringify(r)}）`);
    return false;
  }
  if (r.code === 200) {
    ok(label, detail);
    return true;
  }
  bad(label, `HTTP ${r.httpStatus} code=${r.code} msg=${r.message}`);
  return false;
}

// ---------------------------------------------------------------- 基础设施检查
async function checkHealth() {
  section('0. 服务健康检查');
  const services = [
    ['Gateway', `${BASE_URL}/actuator/health`],
    ['User', `${BASE_URL}/user/actuator/health`],
    ['Workflow', `${BASE_URL}/workflow/actuator/health`],
    ['Executor', `${BASE_URL}/executor/actuator/health`],
    ['Template', `${BASE_URL}/template/actuator/health`],
    ['Monitor', `${BASE_URL}/monitor/actuator/health`],
    ['Infra', `${BASE_URL}/infra/actuator/health`],
  ];
  for (const [name, url] of services) {
    try {
      const res = await fetch(url, { signal: AbortSignal.timeout(8000) });
      if (res.ok) ok(`${name} 健康`, `HTTP ${res.status}`);
      else bad(`${name} 未就绪`, `HTTP ${res.status}`);
    } catch (e) {
      bad(`${name} 不可达`, e.message);
    }
  }
}

// ---------------------------------------------------------------- 登录
async function login() {
  section('1. 登录');
  const attempts = [
    ['用户名密码', { username: USERNAME, password: PASSWORD }],
    ['仅用户名', { username: USERNAME }],
  ];
  for (const [label, body] of attempts) {
    const r = await api('POST', '/user/api/v1/auth/login', body);
    if (r.code === 200 && r.data?.accessToken) {
      token = r.data.accessToken;
      ok(`登录成功（${label}）`, `token=${token.slice(0, 12)}… 过期=${r.data.expiresIn}s`);
      return true;
    }
    info(`登录方式「${label}」失败：code=${r.code} ${r.message}`);
  }
  bad('登录失败', '两种请求体都未拿到 accessToken');
  return false;
}

// ---------------------------------------------------------------- 工作流定义
/** 一个最小可执行 DAG：trigger.form -> code.transform -> end.aggregator */
function buildDefinition(name) {
  return {
    version: 'v1',
    changelog: 'e2e-smoke',
    nodes: [
      {
        id: 'trigger',
        type: 'trigger.form',
        name: '表单触发',
        x: 0,
        y: 0,
        data: {
          fields: [{ key: 'word', label: '词', type: 'string', required: false }],
        },
      },
      {
        id: 'step1',
        type: 'code.transform',
        name: '拼接',
        x: 320,
        y: 0,
        data: {
          language: 'javascript',
          // 注意：JS 源码里一律用单引号。这段字符串会经过 shell/JSON 多层转义，
          // 双引号在部分终端下会被剥掉，导致被测脚本自身语法错误（而不是引擎的问题）。
          source: "return { echo: 'smoke-' + (input.word || 'none') };",
        },
      },
      { id: 'end', type: 'end.aggregator', name: '结束', x: 640, y: 0, data: {} },
    ],
    edges: [
      { id: 'e1', source: 'trigger', target: 'step1', type: 'default' },
      { id: 'e2', source: 'step1', target: 'end', type: 'default' },
    ],
  };
}

// ---------------------------------------------------------------- 主流程
async function createBlankWorkflow() {
  section('2. 空白创建工作流');
  const name = `e2e-smoke-${RUN_ID}`;
  // 刻意不带 definition：先验证"裸创建"（这是 tags 曾经 500 的那条路），
  // 定义留到下一步用 /versions 保存，从而把两个接口分开验证。
  const r = await api('POST', '/workflow/api/workflow', {
    name,
    description: '端到端冒烟测试自动创建，可安全删除',
    isPublic: false,
    tags: ['e2e-smoke'],
  });
  if (!expectOk('创建工作流', r)) return null;
  const id = r.data?.id ?? r.data?.workflowId;
  if (!id) {
    bad('创建工作流未返回 id', JSON.stringify(r.data).slice(0, 200));
    return null;
  }
  ok('返回工作流 id', `id=${id} name=${r.data?.name} version=${r.data?.currentVersion ?? r.data?.version}`);

  // 回读确认真的落库了
  const back = await api('GET', `/workflow/api/workflow/${id}`);
  expectOk('按 id 回读工作流', back, `name=${back.data?.name} status=${back.data?.status}`);
  if (back.data?.tags !== undefined) {
    ok('tags 字段往返正常', `tags=${JSON.stringify(back.data.tags)}`);
  } else {
    info('响应中未包含 tags（不影响创建成功判定）');
  }
  return id;
}

async function saveAndPublish(id) {
  section('3. 保存版本 + 发布');
  const def = buildDefinition(`e2e-smoke-${RUN_ID}`);
  // 显式指定 v1：该工作流尚无任何版本行，v1 是干净的起点。
  const v = await api('POST', `/workflow/api/workflow/${id}/versions`, { ...def, version: 'v1' });
  if (!expectOk('保存工作流版本', v, `version=${v.data?.version}`)) return null;
  const version = v.data?.version;
  if (!version) {
    bad('保存版本未返回 version');
    return null;
  }

  const listed = await api('GET', `/workflow/api/workflow/${id}/versions`);
  if (expectOk('列出工作流版本', listed)) {
    const items = Array.isArray(listed.data) ? listed.data : (listed.data?.records ?? []);
    ok('版本列表', `${items.length} 条：${items.map((x) => `${x.version}(${x.publishStatus})`).join(', ')}`);
  }

  // 发布会对定义做编译校验（环检测/孤立节点/executor 注册）
  const p = await api('POST', `/workflow/api/workflow/${id}/versions/publish`, {
    version,
    changelog: 'e2e-smoke 发布',
  });
  expectOk('发布版本', p, `version=${version}`);

  const after = await api('GET', `/workflow/api/workflow/${id}`);
  const status = after.data?.status ?? after.data?.publishStatus;
  if (status && ['running', 'published'].includes(String(status).toLowerCase())) {
    ok('工作流已进入可执行状态', `status=${status}`);
  } else {
    bad('发布后状态不正确', `status=${status}`);
  }
  return version;
}

async function execute(id) {
  section('4. 执行工作流');
  const ex = await api('POST', '/workflow/api/execution', {
    workflowId: Number(id),
    triggerType: 'manual',
    input: { word: 'hello' },
    async: false,
  });
  if (!expectOk('提交执行', ex, `executionId=${ex.data?.id ?? ex.data?.executionId}`)) return null;

  const executionId = ex.data?.id ?? ex.data?.executionId;
  if (!executionId) {
    bad('执行未返回 executionId');
    return null;
  }

  const status = ex.data?.status;
  ok('执行返回状态', `status=${status} 耗时=${ex.data?.durationMs ?? ex.data?.duration ?? '?'}ms`);
  if (status && !['success', 'completed', 'succeeded'].includes(String(status).toLowerCase())) {
    bad('同步执行未成功', `status=${status} error=${ex.data?.errorMessage ?? ex.data?.error ?? ''}`);
  }

  const detail = await api('GET', `/workflow/api/execution/${executionId}`);
  if (expectOk('回读执行详情', detail, `status=${detail.data?.status}`)) {
    const d = detail.data || {};
    const out = d.output ?? d.result ?? d.outputData;
    ok('执行输出', typeof out === 'object' ? JSON.stringify(out).slice(0, 160) : String(out));
  }

  const nodes = await api('GET', `/workflow/api/execution/${executionId}/nodes`);
  if (expectOk('回读节点执行记录', nodes)) {
    const list = Array.isArray(nodes.data) ? nodes.data : (nodes.data?.records ?? []);
    ok('节点记录条数', `${list.length} 条`);
    const stuck = list.filter(
      (n) => n.status && !['success', 'skipped', 'failed'].includes(String(n.status).toLowerCase()),
    );
    if (stuck.length) bad('存在未终结的节点', stuck.map((n) => `${n.nodeId}=${n.status}`).join(', '));
    else ok('所有节点均已终结', list.map((n) => `${n.nodeId}:${n.status}`).join(' '));
  }
  return executionId;
}

async function createFromTemplate() {
  section('5. 模板模块创建工作流（用户报的"使用模块"路径）');
  // 后端契约：POST /api/template/search，body 为 TemplateSearchRequest
  const search = await api('POST', '/template/api/template/search', {
    keyword: '',
    pageNum: 1,
    pageSize: 5,
    sortBy: 'useCount',
    sortOrder: 'desc',
  });
  if (!expectOk('模板搜索', search)) return null;
  const list = Array.isArray(search.data)
    ? search.data
    : (search.data?.records ?? search.data?.list ?? []);
  if (!list.length) {
    bad('模板列表为空', '无法验证"从模板创建"路径');
    return null;
  }
  ok('模板搜索返回', `${list.length} 条，首条=${list[0].name ?? list[0].templateName}`);

  const tpl = list[0];
  const tplId = tpl.id ?? tpl.templateId;
  const detail = await api('GET', `/template/api/template/${tplId}`);
  if (!expectOk('读取模板详情', detail, `id=${tplId}`)) return null;

  let definition = detail.data?.definition ?? detail.data?.workflowJson ?? tpl.definition;
  if (typeof definition === 'string') {
    try {
      definition = JSON.parse(definition);
    } catch {
      bad('模板 definition 不是合法 JSON', String(definition).slice(0, 120));
      return null;
    }
  }
  if (!definition) {
    bad('模板没有 definition', '无法据此创建工作流');
    return null;
  }

  const name = `e2e-smoke-from-tpl-${RUN_ID}`;
  const r = await api('POST', '/workflow/api/workflow', {
    name,
    description: `由模板 ${tpl.name ?? tplId} 创建（e2e-smoke）`,
    isPublic: false,
    tags: ['e2e-smoke'],
    definition,
  });
  if (!expectOk('从模板创建工作流', r)) return null;
  ok('从模板创建出工作流', `id=${r.data?.id} name=${r.data?.name}`);
  return r.data?.id;
}

async function cleanup(ids) {
  section('6. 清理测试数据');
  if (KEEP) {
    info(`KEEP=1，保留测试工作流：${ids.filter(Boolean).join(', ')}`);
    return;
  }
  for (const id of ids.filter(Boolean)) {
    const r = await api('DELETE', `/workflow/api/workflow/${id}`);
    if (r.code === 200) ok(`已删除工作流 ${id}`);
    else bad(`删除工作流 ${id} 失败`, `code=${r.code} ${r.message}`);
  }
}

// ---------------------------------------------------------------- entry
async function main() {
  console.log(`${C.cyan}MeowFlow E2E 冒烟测试${C.reset}`);
  console.log(`${C.gray}  BASE_URL = ${BASE_URL}`);
  console.log(`  用户     = ${USERNAME}${C.reset}`);

  await checkHealth();
  if (!(await login())) {
    console.log(`\n${C.red}登录失败，后续用例无法执行。${C.reset}`);
    return finish();
  }

  const created = [];
  const blankId = await createBlankWorkflow();
  created.push(blankId);
  if (blankId) {
    await saveAndPublish(blankId);
    await execute(blankId);
  } else {
    bad('跳过保存/发布/执行', '空白创建工作流已失败');
  }

  const tplId = await createFromTemplate();
  created.push(tplId);

  await cleanup(created);
  return finish();
}

function finish() {
  console.log(`\n${C.cyan}=== 结果 ===${C.reset}`);
  console.log(`  通过 ${C.green}${passed}${C.reset}  失败 ${failed ? C.red : C.gray}${failed}${C.reset}`);
  if (failed) {
    console.log(`\n${C.red}失败项：${C.reset}`);
    failures.forEach((f) => console.log(`  - ${f}`));
  }
  console.log('');
  process.exit(failed ? 1 : 0);
}

main().catch((e) => {
  console.error(`\n${C.red}冒烟测试异常终止：${e.stack || e.message}${C.reset}`);
  process.exit(1);
});
