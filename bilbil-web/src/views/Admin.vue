<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminVideos, reviewVideo, adminUsers, setUserStatus, adminComments, deleteComment, adminMerchantApplies, reviewMerchantApply, adminMerchants, rechargeMerchant, adminAds, adminAdStatus } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import { formatTime } from '../utils/format'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('videos')

// 视频审核
const videos = ref([])
const videoTotal = ref(0)
const videoPage = ref(1)
const videoStatus = ref('')
const videoKeyword = ref('')

// 用户管理
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')

// 评论管理
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentKeyword = ref('')

// 商家申请
const applies = ref([])
const applyTotal = ref(0)
const applyPage = ref(1)
const applyStatus = ref('')
const applyKeyword = ref('')

// 商家管理
const merchants = ref([])
const merchantTotal = ref(0)
const merchantPage = ref(1)
const merchantKeyword = ref('')
const rechargeVisible = ref(false)
const rechargeForm = ref({ id: null, name: '', amount: 100 })

// 广告管理
const ads = ref([])
const adTotal = ref(0)
const adPage = ref(1)
const adStatus = ref('')
const adKeyword = ref('')

const statusText = { 0: '待审核', 1: '已发布', 2: '已下架', 3: '转码中' }
const applyStatusText = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
const adStatusText = { 1: '投放中', 2: '已暂停', 3: '已结束' }

async function loadVideos(p = 1) {
  const res = await adminVideos({
    status: videoStatus.value === '' ? undefined : videoStatus.value,
    keyword: videoKeyword.value || undefined,
    page: p,
    size: 10
  })
  videos.value = res.list || []
  videoTotal.value = res.total
  videoPage.value = p
}

async function onReviewVideo(v) {
  const next = v.status === 1 ? 2 : 1
  const action = next === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定${action}该视频吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await reviewVideo(v.id, next)
  ElMessage.success(`${action}成功`)
  loadVideos(videoPage.value)
}

async function loadUsers(p = 1) {
  const res = await adminUsers({ keyword: userKeyword.value || undefined, page: p, size: 10 })
  users.value = res.list || []
  userTotal.value = res.total
  userPage.value = p
}

async function onToggleUser(u) {
  const next = u.status === 1 ? 0 : 1
  const action = next === 1 ? '封禁' : '解封'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${u.nickname || u.username}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await setUserStatus(u.id, next)
  ElMessage.success(`${action}成功`)
  loadUsers(userPage.value)
}

async function loadComments(p = 1) {
  const res = await adminComments({ keyword: commentKeyword.value || undefined, page: p, size: 10 })
  comments.value = res.list || []
  commentTotal.value = res.total
  commentPage.value = p
}

async function onDeleteComment(c) {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteComment(c.id)
  ElMessage.success('已删除')
  loadComments(commentPage.value)
}

async function loadApplies(p = 1) {
  const res = await adminMerchantApplies({
    status: applyStatus.value === '' ? undefined : applyStatus.value,
    keyword: applyKeyword.value || undefined,
    page: p,
    size: 10
  })
  applies.value = res.list || []
  applyTotal.value = res.total
  applyPage.value = p
}

async function onReviewApply(a, approve) {
  const action = approve ? '通过' : '拒绝'
  let note = ''
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '审核通过（备注可选，直接确认即可）' : '请填写拒绝原因',
      `确定${action}该申请吗？`,
      {
        confirmButtonText: action,
        cancelButtonText: '取消',
        inputPlaceholder: approve ? '备注（可选）' : '拒绝原因',
        type: approve ? 'success' : 'warning'
      }
    )
    note = value || ''
  } catch (e) {
    return
  }
  await reviewMerchantApply(a.id, { status: approve ? 1 : 2, note })
  ElMessage.success(`${action}成功`)
  loadApplies(applyPage.value)
}

async function loadMerchants(p = 1) {
  const res = await adminMerchants({ keyword: merchantKeyword.value || undefined, page: p, size: 10 })
  merchants.value = res.list || []
  merchantTotal.value = res.total
  merchantPage.value = p
}

function openRecharge(m) {
  rechargeForm.value = { id: m.id, name: m.nickname || m.username, amount: 100 }
  rechargeVisible.value = true
}

async function onRecharge() {
  if (!rechargeForm.value.amount || Number(rechargeForm.value.amount) <= 0) {
    return ElMessage.warning('请输入正数积分')
  }
  await rechargeMerchant(rechargeForm.value.id, Number(rechargeForm.value.amount))
  ElMessage.success('充值成功')
  rechargeVisible.value = false
  loadMerchants(merchantPage.value)
}

async function loadAds(p = 1) {
  const res = await adminAds({
    status: adStatus.value === '' ? undefined : adStatus.value,
    keyword: adKeyword.value || undefined,
    page: p,
    size: 10
  })
  ads.value = res.list || []
  adTotal.value = res.total
  adPage.value = p
}

async function onAdStatus(a, next) {
  const action = next === 1 ? '恢复投放' : next === 2 ? '暂停' : '结束'
  try {
    await ElMessageBox.confirm(`确定${action}该广告吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await adminAdStatus(a.id, next)
  ElMessage.success(`已${action}`)
  loadAds(adPage.value)
}

function onTabChange(name) {
  if (name === 'videos') loadVideos(1)
  else if (name === 'users') loadUsers(1)
  else if (name === 'comments') loadComments(1)
  else if (name === 'applies') loadApplies(1)
  else if (name === 'merchants') loadMerchants(1)
  else if (name === 'ads') loadAds(1)
}

onMounted(() => {
  if (!(auth.user && auth.user.role === 1)) {
    ElMessage.error('无管理员权限')
    router.replace('/')
    return
  }
  loadVideos(1)
})
</script>

<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="a-head">
          <span class="a-title">后台管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 视频审核 -->
        <el-tab-pane label="视频审核" name="videos">
          <div class="toolbar">
            <el-select v-model="videoStatus" placeholder="全部状态" clearable style="width: 140px" @change="loadVideos(1)">
              <el-option :value="0" label="待审核" />
              <el-option :value="1" label="已发布" />
              <el-option :value="2" label="已下架" />
              <el-option :value="3" label="转码中" />
            </el-select>
            <el-input v-model="videoKeyword" placeholder="按标题搜索" clearable style="width: 220px" @keyup.enter="loadVideos(1)" />
            <el-button type="primary" @click="loadVideos(1)">查询</el-button>
          </div>

          <el-table :data="videos" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="视频" min-width="260">
              <template #default="{ row }">
                <div class="cell-title">{{ row.title }}</div>
              </template>
            </el-table-column>
            <el-table-column label="作者" width="140">
              <template #default="{ row }">{{ row.authorName }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small">
                  {{ statusText[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  size="small"
                  :type="row.status === 1 ? 'danger' : 'primary'"
                  @click="onReviewVideo(row)"
                >{{ row.status === 1 ? '下架' : '上架' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="videoTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="videoTotal"
            :page-size="10"
            :current-page="videoPage"
            @current-change="loadVideos"
          />
        </el-tab-pane>

        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <div class="toolbar">
            <el-input v-model="userKeyword" placeholder="按用户名/昵称搜索" clearable style="width: 220px" @keyup.enter="loadUsers(1)" />
            <el-button type="primary" @click="loadUsers(1)">查询</el-button>
          </div>

          <el-table :data="users" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="用户" min-width="160">
              <template #default="{ row }">
                <span>{{ row.nickname || row.username }}</span>
                <span class="username">（{{ row.username }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 1 ? 'warning' : 'info'" size="small">{{ row.role === 1 ? '管理员' : '普通用户' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">{{ row.status === 1 ? '已封禁' : '正常' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  size="small"
                  :type="row.status === 1 ? 'primary' : 'danger'"
                  @click="onToggleUser(row)"
                >{{ row.status === 1 ? '解封' : '封禁' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="userTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="userTotal"
            :page-size="10"
            :current-page="userPage"
            @current-change="loadUsers"
          />
        </el-tab-pane>

        <!-- 评论管理 -->
        <el-tab-pane label="评论管理" name="comments">
          <div class="toolbar">
            <el-input v-model="commentKeyword" placeholder="按内容搜索" clearable style="width: 220px" @keyup.enter="loadComments(1)" />
            <el-button type="primary" @click="loadComments(1)">查询</el-button>
          </div>

          <el-table :data="comments" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="评论内容" min-width="300">
              <template #default="{ row }">{{ row.content }}</template>
            </el-table-column>
            <el-table-column label="用户" width="140">
              <template #default="{ row }">{{ row.userName }}</template>
            </el-table-column>
            <el-table-column label="视频ID" width="90">
              <template #default="{ row }">{{ row.videoId }}</template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="onDeleteComment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="commentTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="10"
            :current-page="commentPage"
            @current-change="loadComments"
          />
        </el-tab-pane>

        <!-- 商家申请 -->
        <el-tab-pane label="商家申请" name="applies">
          <div class="toolbar">
            <el-select v-model="applyStatus" placeholder="全部状态" clearable style="width: 140px" @change="loadApplies(1)">
              <el-option :value="0" label="待审核" />
              <el-option :value="1" label="已通过" />
              <el-option :value="2" label="已拒绝" />
            </el-select>
            <el-input v-model="applyKeyword" placeholder="按用户名/姓名搜索" clearable style="width: 220px" @keyup.enter="loadApplies(1)" />
            <el-button type="primary" @click="loadApplies(1)">查询</el-button>
          </div>

          <el-table :data="applies" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="申请人" min-width="150">
              <template #default="{ row }">
                <span>{{ row.nickname || row.username }}</span>
                <span class="username">（{{ row.username }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="姓名/公司" width="140">
              <template #default="{ row }">{{ row.realName }}</template>
            </el-table-column>
            <el-table-column label="联系方式" width="130">
              <template #default="{ row }">{{ row.contact }}</template>
            </el-table-column>
            <el-table-column label="理由" min-width="180">
              <template #default="{ row }">{{ row.reason }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'" size="small">
                  {{ applyStatusText[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button size="small" type="success" @click="onReviewApply(row, true)">通过</el-button>
                  <el-button size="small" type="danger" @click="onReviewApply(row, false)">拒绝</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="applyTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="applyTotal"
            :page-size="10"
            :current-page="applyPage"
            @current-change="loadApplies"
          />
        </el-tab-pane>

        <!-- 商家管理 -->
        <el-tab-pane label="商家管理" name="merchants">
          <div class="toolbar">
            <el-input v-model="merchantKeyword" placeholder="按用户名/昵称搜索" clearable style="width: 220px" @keyup.enter="loadMerchants(1)" />
            <el-button type="primary" @click="loadMerchants(1)">查询</el-button>
          </div>

          <el-table :data="merchants" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="商家" min-width="160">
              <template #default="{ row }">
                <span>{{ row.nickname || row.username }}</span>
                <span class="username">（{{ row.username }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="积分余额" width="120">
              <template #default="{ row }">{{ row.balance }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">{{ row.status === 1 ? '已封禁' : '正常' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openRecharge(row)">充值</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="merchantTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="merchantTotal"
            :page-size="10"
            :current-page="merchantPage"
            @current-change="loadMerchants"
          />
        </el-tab-pane>

        <!-- 广告管理 -->
        <el-tab-pane label="广告管理" name="ads">
          <div class="toolbar">
            <el-select v-model="adStatus" placeholder="全部状态" clearable style="width: 140px" @change="loadAds(1)">
              <el-option :value="1" label="投放中" />
              <el-option :value="2" label="已暂停" />
              <el-option :value="3" label="已结束" />
            </el-select>
            <el-input v-model="adKeyword" placeholder="按标题搜索" clearable style="width: 220px" @keyup.enter="loadAds(1)" />
            <el-button type="primary" @click="loadAds(1)">查询</el-button>
          </div>

          <el-table :data="ads" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="广告" min-width="220">
              <template #default="{ row }">
                <div class="cell-title">{{ row.title }}</div>
              </template>
            </el-table-column>
            <el-table-column label="商家" width="140">
              <template #default="{ row }">{{ row.merchantName }}</template>
            </el-table-column>
            <el-table-column label="分区" width="100">
              <template #default="{ row }">{{ row.categoryName }}</template>
            </el-table-column>
            <el-table-column label="曝光/点击" width="110">
              <template #default="{ row }">{{ row.impressions }} / {{ row.clicks }}</template>
            </el-table-column>
            <el-table-column label="消耗/预算" width="120">
              <template #default="{ row }">{{ row.spent }} / {{ row.budget }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small">
                  {{ adStatusText[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" size="small" type="danger" @click="onAdStatus(row, 2)">暂停</el-button>
                <el-button v-else-if="row.status === 2" size="small" type="primary" @click="onAdStatus(row, 1)">恢复</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="adTotal > 10"
            class="pager"
            layout="prev, pager, next"
            :total="adTotal"
            :page-size="10"
            :current-page="adPage"
            @current-change="loadAds"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="rechargeVisible" title="商家充值" width="420px">
      <el-form label-width="80px">
        <el-form-item label="商家">
          <span>{{ rechargeForm.name }}</span>
        </el-form-item>
        <el-form-item label="充值积分">
          <el-input-number v-model="rechargeForm.amount" :min="1" :step="10" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="onRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1080px;
  margin: 0 auto;
}
.a-title {
  font-weight: 600;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.cell-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.username {
  color: #99a2aa;
  font-size: 12px;
}
.pager {
  justify-content: center;
  margin-top: 16px;
}
@media (max-width: 768px) {
  .toolbar {
    flex-wrap: wrap;
  }
}
</style>
