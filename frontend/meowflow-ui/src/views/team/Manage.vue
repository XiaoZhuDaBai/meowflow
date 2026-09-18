<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">团队管理</div>
        <div class="page-subtitle">
          <span v-if="isMock">Mock 数据 · 邀请 / 角色 / 状态 / 移除均已对接真实后端</span>
          <span v-else>共 {{ pageData.total }} 位成员</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="openCreateOrg"><i class="fa-solid fa-building"></i><span style="margin-left:6px">创建团队</span></el-button>
        <el-button type="primary" @click="openInvite"><i class="fa-solid fa-user-plus"></i><span style="margin-left:6px">邀请成员</span></el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="icon" style="background: var(--primary-bg); color: var(--primary);"><i class="fa-solid fa-users"></i></div>
        <div>
          <div class="label">总成员</div>
          <div class="value">{{ pageData.total }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--success-bg); color: var(--success);"><i class="fa-solid fa-circle-check"></i></div>
        <div>
          <div class="label">活跃</div>
          <div class="value">{{ activeCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--info-bg); color: var(--info);"><i class="fa-solid fa-building"></i></div>
        <div>
          <div class="label">部门数</div>
          <div class="value">{{ depts }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--warning-bg); color: var(--warning);"><i class="fa-solid fa-user-shield"></i></div>
        <div>
          <div class="label">管理员</div>
          <div class="value">{{ adminCount }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px;">
      <div class="card-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名 / 邮箱"
          clearable
          style="width: 220px;"
          @input="debouncedLoad"
        >
          <template #prefix><i class="fa-solid fa-search"></i></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px;" @change="loadMembers(1)">
          <el-option label="全部" value="" />
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
        <el-button @click="loadMembers(1)"><i class="fa-solid fa-refresh"></i></el-button>
      </div>

      <el-table :data="pageData.records" v-loading="loading" stripe class="member-table">
        <el-table-column label="成员" min-width="200">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="36" :src="row.avatar">
                {{ (row.displayName || row.username)?.[0]?.toUpperCase() }}
              </el-avatar>
              <div class="member-info">
                <span class="member-name">{{ row.displayName || row.username }}</span>
                <span class="member-email">{{ row.email }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'admin' ? '管理员' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span :style="{ color: isActive(row) ? 'var(--success)' : 'var(--danger)' }">
              <i :class="isActive(row) ? 'fa-solid fa-circle' : 'fa-solid fa-circle-xmark'"></i>
              {{ isActive(row) ? '活跃' : '禁用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="加入时间" width="140">
          <template #default="{ row }">{{ formatDate(row.joinedAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link :disabled="!hasBackend" @click="openRole(row)">角色</el-button>
            <el-button size="small" link :disabled="!hasBackend" @click="toggleStatus(row)">
              {{ isActive(row) ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" link type="danger" :disabled="!hasBackend" @click="confirmRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageData.current"
          :page-size="pageData.size"
          :total="pageData.total"
          layout="total, prev, pager, next"
          @current-change="loadMembers"
        />
      </div>
    </div>

    <!-- 邀请弹窗 -->
    <el-dialog v-model="inviteVisible" title="邀请成员" width="480px" @close="inviteFormRef?.resetFields()">
      <el-form ref="inviteFormRef" :model="inviteForm" :rules="inviteRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="inviteForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model="inviteForm.displayName" placeholder="显示名称（选填）" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="inviteForm.email" placeholder="成员邮箱" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="inviteForm.password" type="password" show-password placeholder="设置初始密码（至少6位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviteLoading" @click="doInvite">确认邀请</el-button>
      </template>
    </el-dialog>

    <!-- 创建团队弹窗 -->
    <el-dialog v-model="createOrgVisible" title="创建团队" width="480px" @close="createOrgFormRef?.resetFields()">
      <el-form ref="createOrgFormRef" :model="createOrgForm" :rules="createOrgRules" label-width="90px">
        <el-form-item label="团队名称" prop="name">
          <el-input v-model="createOrgForm.name" placeholder="请输入团队名称" />
        </el-form-item>
        <el-form-item label="上级团队" prop="parentId">
          <el-tree-select
            v-model="createOrgForm.parentId"
            :data="orgTreeData"
            :props="treeSelectProps"
            placeholder="选择上级团队（可选）"
            clearable
            check-strictly
            :render-after-expand="false"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="团队编码" prop="code">
          <el-input v-model="createOrgForm.code" placeholder="团队唯一标识（可选）" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="createOrgForm.leader" placeholder="团队负责人（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOrgVisible = false">取消</el-button>
        <el-button type="primary" :loading="createOrgLoading" @click="doCreateOrg">创建</el-button>
      </template>
    </el-dialog>

    <!-- 角色弹窗 -->
    <el-dialog v-model="roleVisible" title="修改角色" width="380px">
      <el-form label-width="60px">
        <el-form-item label="成员">
          <span>{{ currentRow?.displayName || currentRow?.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="selectedRole">
            <el-radio v-for="role in roleOptions" :key="role.id" :value="String(role.id)">
              {{ role.name }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="doUpdateRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { teamApi, type TeamMember, type TeamRole } from '@/api/team';
import { organizationApi } from '@/api/organization';
import { isMockEnabled } from '@/api/endpoints';
import { formatDate } from '@/utils/format';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { MOCK_TEAM_MEMBERS } from '@/mock/team';
import { MOCK_ORGS } from '@/mock/organization';
import type { OrgTree } from '@/types/organization';

const isMock = isMockEnabled;
const hasBackend = !isMockEnabled;
const treeSelectProps: any = { label: 'name', value: 'id', children: 'children' };

const loading = ref(false);
const keyword = ref('');
const statusFilter = ref('');
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

// pageData.records holds mixed mock/real data; use TeamMember[] (with extended status field)
const pageData = reactive({
  records: [] as TeamMember[],
  total: 0,
  current: 1,
  size: 10,
});

// Mock fallback members (imported from mock/team.ts)
const mockMembers = ref<TeamMember[]>([...MOCK_TEAM_MEMBERS]);

// el-table slot row 类型为 DefaultRow（looser than TeamMember）；运行时通过可选链安全访问
function isActive(row: any) {
  return row?.status === 'active' || row?.status === '1';
}

const activeCount = computed(() => pageData.records.filter((m) => isActive(m)).length);
const adminCount  = computed(() => pageData.records.filter((m) => m.role === 'admin').length);
const depts = computed(() => {
  const set = new Set(pageData.records.map((m) => m.email.split('@')[1]).filter(Boolean));
  return set.size;
});

async function loadMembers(page = pageData.current) {
  loading.value = true;
  try {
    if (isMockEnabled) {
      const list = mockMembers.value;
      const filtered = list.filter((m) => {
        const kw = keyword.value.toLowerCase();
        const matchKw = !kw || (m.username + m.displayName + m.email).toLowerCase().includes(kw);
        const matchStatus = !statusFilter.value || m.status === (statusFilter.value === '1' ? 'active' : 'disabled');
        return matchKw && matchStatus;
      });
      const start = (page - 1) * pageData.size;
      pageData.records = filtered.slice(start, start + pageData.size);
      pageData.total = filtered.length;
      pageData.current = page;
    } else {
      const result = await teamApi.pageMembers({
        current: page,
        size: pageData.size,
        keyword: keyword.value || undefined,
        status: statusFilter.value || undefined,
      });
      pageData.records = result.records.map((member: any) => ({
        ...member,
        id: String(member.id),
        nickname: member.nickName ?? member.nickname,
        displayName: member.nickName ?? member.nickname ?? member.displayName ?? member.username,
        role: Array.isArray(member.roles) && member.roles.some((role: any) =>
          String(role).toLowerCase() === 'admin' || String(role) === '1'
        ) ? 'admin' : 'user',
        status: member.status === '1' || member.status === 'active' ? 'active' : 'disabled',
      }));
      pageData.total = Number(result.total ?? 0);
      pageData.current = Number(result.current ?? page);
      pageData.size = Number(result.size ?? pageData.size);
    }
  } catch {
    pageData.records = mockMembers.value;
    pageData.total = mockMembers.value.length;
  } finally {
    loading.value = false;
  }
}

function debouncedLoad() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => loadMembers(1), 350);
}

// ---- Invite ----
const inviteVisible = ref(false);
const inviteLoading = ref(false);
const inviteFormRef = ref<FormInstance>();
const inviteForm = reactive({ username: '', displayName: '', email: '', password: '' });
const inviteRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
};

function openInvite() {
  inviteVisible.value = true;
}

// ---- Create Org ----
const createOrgVisible = ref(false);
const createOrgLoading = ref(false);
const createOrgFormRef = ref<FormInstance>();
const createOrgForm = reactive({ name: '', parentId: '', code: '', leader: '' });
const createOrgRules: FormRules = {
  name: [{ required: true, message: '请输入团队名称', trigger: 'blur' }],
};
const orgTreeData = ref<OrgTree[]>([]);

function flattenOrgTree(orgs: OrgTree[], result: OrgTree[] = []): OrgTree[] {
  for (const org of orgs) {
    result.push(org);
    if (org.children?.length) {
      flattenOrgTree(org.children, result);
    }
  }
  return result;
}

async function loadOrgTree() {
  try {
    if (isMockEnabled) {
      orgTreeData.value = MOCK_ORGS;
    } else {
      const tree = await organizationApi.getOrgTree();
      orgTreeData.value = tree || [];
    }
  } catch {
    orgTreeData.value = [];
  }
}

function openCreateOrg() {
  loadOrgTree();
  createOrgVisible.value = true;
}

async function doCreateOrg() {
  if (!createOrgFormRef.value) return;
  await createOrgFormRef.value.validate(async (valid) => {
    if (!valid) return;
    createOrgLoading.value = true;
    try {
      if (isMockEnabled) {
        const newOrg: OrgTree = {
          id: `mock-org-${Date.now()}`,
          name: createOrgForm.name,
          parentId: createOrgForm.parentId || '0',
          code: createOrgForm.code,
          leader: createOrgForm.leader,
          status: '1',
        };
        ElMessage.success('团队创建成功（Mock）');
      } else {
        await organizationApi.createOrg({
          name: createOrgForm.name,
          parentId: createOrgForm.parentId || undefined,
          code: createOrgForm.code || undefined,
          leader: createOrgForm.leader || undefined,
        });
        ElMessage.success('团队创建成功');
      }
      createOrgVisible.value = false;
      loadOrgTree();
    } catch {
      // handled by interceptor
    } finally {
      createOrgLoading.value = false;
    }
  });
}

async function doInvite() {
  if (!inviteFormRef.value) return;
  await inviteFormRef.value.validate(async (valid) => {
    if (!valid) return;
    inviteLoading.value = true;
    try {
      if (isMockEnabled) {
        const newMember: TeamMember = {
          id: `mock-${Date.now()}`,
          username: inviteForm.username,
          nickname: inviteForm.displayName || inviteForm.username,
          displayName: inviteForm.displayName || inviteForm.username,
          email: inviteForm.email,
          role: 'user',
          status: 'active',
          joinedAt: new Date().toISOString().split('T')[0],
          permissions: [],
        };
        mockMembers.value.unshift(newMember);
        ElMessage.success('成员创建成功（Mock）');
      } else {
        await teamApi.inviteMember({
          username: inviteForm.username,
          displayName: inviteForm.displayName,
          email: inviteForm.email,
          password: inviteForm.password,
        });
        ElMessage.success('邀请成功');
      }
      inviteVisible.value = false;
      loadMembers(1);
    } catch {
      // handled by interceptor
    } finally {
      inviteLoading.value = false;
    }
  });
}

// ---- Role ----
const roleVisible = ref(false);
const roleLoading = ref(false);
const currentRow = ref<TeamMember | null>(null);
const roleOptions = ref<TeamRole[]>([
  { id: '1', name: '管理员', code: 'admin' },
  { id: '2', name: '成员', code: 'common' },
]);
const selectedRole = ref<string>('2');

async function loadRoles() {
  if (isMockEnabled) return;
  roleOptions.value = await teamApi.listRoles();
}

function openRole(row: any) {
  currentRow.value = row;
  const matched = roleOptions.value.find((role) =>
    row.role === 'admin' ? role.code === 'admin' : role.code !== 'admin'
  ) ?? roleOptions.value[0];
  selectedRole.value = String(matched.id);
  roleVisible.value = true;
}

async function doUpdateRole() {
  if (!currentRow.value) return;
  roleLoading.value = true;
  try {
    if (isMockEnabled) {
      currentRow.value.role = selectedRole.value === '1' ? 'admin' : 'user';
      ElMessage.success('角色已更新（Mock）');
    } else {
      await teamApi.updateMemberRole(currentRow.value.id, [selectedRole.value]);
      ElMessage.success('角色已更新');
    }
    roleVisible.value = false;
    loadMembers();
  } catch {
    // handled by interceptor
  } finally {
    roleLoading.value = false;
  }
}

// ---- Status ----
async function toggleStatus(row: any) {
  const newStatus = isActive(row) ? '0' : '1';
  const label = newStatus === '1' ? '启用' : '禁用';
  try {
    if (isMockEnabled) {
      row.status = newStatus === '1' ? 'active' : 'disabled';
      ElMessage.success(`${label}成功（Mock）`);
    } else {
      await teamApi.updateMemberStatus(row.id, newStatus);
      ElMessage.success(`${label}成功`);
    }
    loadMembers();
  } catch {
    // handled by interceptor
  }
}

// ---- Remove ----
async function confirmRemove(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${row.displayName || row.username} 移出团队？此操作不可恢复。`,
      '移除成员',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' },
    );
    if (isMockEnabled) {
      const idx = mockMembers.value.findIndex((m) => m.id === row.id);
      if (idx !== -1) mockMembers.value.splice(idx, 1);
      ElMessage.success('已移除（Mock）');
    } else {
      await teamApi.removeMember(row.id);
      ElMessage.success('已移除');
    }
    loadMembers();
  } catch {
    // user cancelled
  }
}

onMounted(() => {
  loadRoles();
  loadMembers(1);
});
</script>

<style scoped>
.card-title { font-size: 14px; font-weight: 600; }
.card-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}
.member-cell { display: flex; align-items: center; gap: 10px; }
.member-info { display: flex; flex-direction: column; }
.member-name { font-weight: 500; font-size: 13px; }
.member-email { font-size: 11px; color: var(--text-tertiary); }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
.header-actions { display: flex; gap: 8px; }

/* Member table fills width */
.member-table {
  width: 100%;
}

/* Stat card layout */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.stat-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-card .icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.stat-card > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.stat-card .label {
  color: var(--text-secondary);
  font-size: 12px;
}
.stat-card .value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
