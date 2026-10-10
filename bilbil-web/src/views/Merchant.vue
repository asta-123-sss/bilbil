<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyMerchant, myApply } from '../api/merchant'
import { createAd, myAds, updateAdStatus, deleteAd, uploadAdCover } from '../api/ad'
import { listCategories } from '../api/category'
import { getMe } from '../api/user'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const role = ref(0)
const balance = ref(0)
const loading = ref(false)

// 商家申请
const applyForm = ref({ realName: '', contact: '', reason: '' })
const applying = ref(false)
const myApplyInfo = ref(null)
const applyStatusText = { 0: '待审核', 1: '已通过', 2: '已拒绝' }

// 广告
const categories = ref([])
const ads = ref([])
const adTotal = ref(0)
const adPage = ref(1)
const createVisible = ref(false)
const createForm = ref({ title: '', coverUrl: '', targetUrl: '', categoryId: null, cpm: 10, budget: 100 })
const adStatusText = { 1: '投放中', 2: '已暂停', 3: '已结束' }

async function refreshUser() {
  const me = await getMe()
  auth.setUser({ ...(auth.user || {}), ...me })
  role.value = me.role || 0
  balance.value = me.balance || 0
}

async function loadMyApply() {
  myApplyInfo.value = await myApply()
}

async function submitApply() {
  if (!applyForm.value.realName || !applyForm.value.contact || !applyForm.value.reason) {
    return ElMessage.warning('请填写完整')
  }
  applying.value = true
  try {
    await applyMerchant(applyForm.value)
    ElMessage.success('申请已提交，等待管理员审核')
    loadMyApply()
  } finally {
    applying.value = false
  }
}

async function loadCategories() {
  categories.value = (await listCategories()) || []
}

async function loadAds(p = 1) {
  loading.value = true
  try {
    const res = await myAds({ page: p, size: 10 })
    ads.value = res.list || []
    adTotal.value = res.total
    adPage.value = p
  } finally {
    loading.value = false
  }
}

function openCreate() {
  loadCategories()
  createForm.value = { title: '', coverUrl: '', targetUrl: '', categoryId: null, cpm: 10, budget: 100 }
  createVisible.value = true
}

async function onSubmitCreate() {
  const f = createForm.value
  if (!f.title || !f.targetUrl || !f.categoryId) return ElMessage.warning('请填写完整')
  if (!f.cpm || Number(f.cpm) <= 0) return ElMessage.warning('CPM 必须为正数')
  if (!f.budget || Number(f.budget) < Number(f.cpm)) return ElMessage.warning('预算至少需覆盖一次 CPM')
  await createAd({
    title: f.title,
    coverUrl: f.coverUrl,
    targetUrl: f.targetUrl,
    categoryId: f.categoryId,
    cpm: Number(f.cpm),
    budget: Number(f.budget)
  })
  ElMessage.success('广告已发布')
  createVisible.value = false
  loadAds(adPage.value)
  refreshUser()
}

async function onAdCoverChange(uploadFile) {
  const f = uploadFile.raw
  if (!f) return
  const url = await uploadAdCover(f)
  createForm.value.coverUrl = url
  ElMessage.success('广告图已上传')
}

async function onAdStatus(a, next) {
  const action = next === 1 ? '投放' : next === 2 ? '暂停' : '结束'
  try {
    await ElMessageBox.confirm(`确定${action}该广告吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await updateAdStatus(a.id, next)
  ElMessage.success(`已${action}`)
  loadAds(adPage.value)
}

async function onDeleteAd(a) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除该广告吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteAd(a.id)
  ElMessage.success('已删除')
  loadAds(adPage.value)
}

onMounted(async () => {
  await refreshUser()
  if (role.value === 2) loadAds(1)
  else if (role.value === 0) loadMyApply()
})
</script>

<template>
  <div class="merchant-page">
    <!-- 商家后台 -->
    <template v-if="role === 2">
      <el-card class="head-card">
        <div class="m-head">
          <div>
            <div class="m-title">商家中心</div>
            <div class="m-balance">积分余额：<b>{{ balance }}</b> 积分</div>
          </div>
          <el-button type="primary" @click="openCreate">发布广告</el-button>
        </div>
      </el-card>

      <el-card>
        <el-table v-loading="loading" :data="ads" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="广告" min-width="220">
            <template #default="{ row }">
              <div class="cell-title">{{ row.title }}</div>
            </template>
          </el-table-column>
          <el-table-column label="分区" width="100">
            <template #default="{ row }">{{ row.categoryName }}</template>
          </el-table-column>
          <el-table-column label="曝光" width="90">
            <template #default="{ row }">{{ row.impressions }}</template>
          </el-table-column>
          <el-table-column label="点击" width="90">
            <template #default="{ row }">{{ row.clicks }}</template>
          </el-table-column>
          <el-table-column label="消耗/预算" width="130">
            <template #default="{ row }">{{ row.spent }} / {{ row.budget }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small">
                {{ adStatusText[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" size="small" @click="onAdStatus(row, 2)">暂停</el-button>
              <el-button v-if="row.status === 1" size="small" type="danger" @click="onAdStatus(row, 3)">结束</el-button>
              <el-button v-if="row.status === 2" size="small" type="primary" @click="onAdStatus(row, 1)">继续投放</el-button>
              <el-button size="small" type="danger" plain @click="onDeleteAd(row)">删除</el-button>
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
      </el-card>
    </template>

    <!-- 普通用户：申请 / 申请状态 -->
    <el-card v-else-if="role === 0" class="apply-card">
      <template v-if="myApplyInfo && myApplyInfo.status === 0">
        <el-result icon="info" title="申请已提交，等待管理员审核" sub-title="审核通过后即可成为商家并获赠初始积分" />
      </template>
      <template v-else-if="myApplyInfo && myApplyInfo.status === 1">
        <el-result icon="success" title="申请已通过" sub-title="刷新页面或重新登录后进入商家后台" />
      </template>
      <template v-else>
        <div class="apply-title">申请成为商家</div>
        <el-form label-width="80px" class="apply-form">
          <el-form-item label="姓名/公司">
            <el-input v-model="applyForm.realName" maxlength="100" placeholder="个人姓名或公司名称" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="applyForm.contact" maxlength="100" placeholder="手机号 / 微信 / 邮箱" />
          </el-form-item>
          <el-form-item label="申请理由">
            <el-input v-model="applyForm.reason" type="textarea" :rows="3" maxlength="500" placeholder="请简单说明推广内容" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>

    <!-- 管理员提示 -->
    <el-card v-else class="apply-card">
      <el-result icon="info" title="您是管理员" sub-title="商家中心仅对商家开放" />
    </el-card>

    <!-- 发布广告 -->
    <el-dialog v-model="createVisible" title="发布广告" width="520px">
      <el-form label-width="80px">
        <el-form-item label="广告标题">
          <el-input v-model="createForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="广告图">
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onAdCoverChange">
            <img v-if="createForm.coverUrl" :src="createForm.coverUrl" class="cover-preview" />
            <el-button v-else>上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="落地链接">
          <el-input v-model="createForm.targetUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="目标分区">
          <el-select v-model="createForm.categoryId" placeholder="选择分区" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="CPM">
          <el-input-number v-model="createForm.cpm" :min="1" :step="1" style="width: 100%" />
          <div class="tip">每千次曝光消耗的积分</div>
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="createForm.budget" :min="1" :step="10" style="width: 100%" />
          <div class="tip">广告总预算（积分），余额须覆盖预算</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.merchant-page {
  max-width: 1080px;
  margin: 0 auto;
}
.head-card {
  margin-bottom: 16px;
}
.m-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.m-title {
  font-size: 20px;
  font-weight: 600;
}
.m-balance {
  margin-top: 8px;
  color: #666;
}
.m-balance b {
  color: #fb7299;
  font-size: 18px;
}
.cell-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pager {
  justify-content: center;
  margin-top: 16px;
}
.apply-card {
  max-width: 640px;
  margin: 40px auto;
}
.apply-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}
.cover-preview {
  width: 160px;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
}
.tip {
  font-size: 12px;
  color: #99a2aa;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
