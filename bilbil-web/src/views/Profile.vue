<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserProfile, updateInfo, uploadAvatar, changePassword } from '../api/user'
import { myVideos, updateVideo, deleteVideo } from '../api/video'
import { follow, unfollow, followings, fans } from '../api/follow'
import { listCategories } from '../api/category'
import { useAuthStore } from '../stores/auth'
import { formatCount } from '../utils/format'
import VideoCard from '../components/VideoCard.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isOwn = computed(() => route.name === 'me')
const targetId = computed(() => (isOwn.value ? auth.userId : Number(route.params.id)))

const profile = ref(null)
const activeTab = ref('followings')

// 我的投稿
const videos = ref([])
const videoTotal = ref(0)
const videoPage = ref(1)
const loadingVideos = ref(false)

// 关注 / 粉丝
const fList = ref([])
const fTotal = ref(0)
const fPage = ref(1)
const fKind = ref('followings')

// 编辑资料
const editVisible = ref(false)
const editForm = ref({ nickname: '', gender: 0, birthday: '', signature: '', email: '', phone: '' })

async function loadProfile() {
  profile.value = await getUserProfile(targetId.value)
}

async function loadVideos(p = 1) {
  loadingVideos.value = true
  try {
    const res = await myVideos({ page: p, size: 8 })
    videos.value = res.list || []
    videoTotal.value = res.total
    videoPage.value = p
  } finally {
    loadingVideos.value = false
  }
}

async function loadFollow(kind, p = 1) {
  fKind.value = kind
  fPage.value = p
  const res = await (kind === 'followings' ? followings : fans)(targetId.value, { page: p, size: 10 })
  fList.value = res.list || []
  fTotal.value = res.total
}

function onTabChange(name) {
  activeTab.value = name
  if (name === 'videos') loadVideos(1)
  else if (name === 'followings') loadFollow('followings', 1)
  else if (name === 'fans') loadFollow('fans', 1)
}

async function toggleFollow() {
  if (!auth.isLogin) return ElMessage.warning('请先登录')
  if (profile.value.followed) {
    await unfollow(targetId.value)
    profile.value.followed = false
    profile.value.fanCount = Math.max(0, (profile.value.fanCount || 1) - 1)
  } else {
    await follow(targetId.value)
    profile.value.followed = true
    profile.value.fanCount = (profile.value.fanCount || 0) + 1
  }
}

function openEdit() {
  editForm.value = {
    nickname: profile.value.nickname || '',
    gender: profile.value.gender || 0,
    birthday: profile.value.birthday || '',
    signature: profile.value.signature || '',
    email: profile.value.email || '',
    phone: profile.value.phone || ''
  }
  editVisible.value = true
}

async function onSubmitEdit() {
  const data = { ...editForm.value }
  if (!data.birthday) delete data.birthday
  const updated = await updateInfo(data)
  profile.value = { ...profile.value, ...updated }
  auth.setUser({ ...auth.user, ...updated })
  ElMessage.success('资料已更新')
  editVisible.value = false
}

async function onAvatarChange(uploadFile) {
  const f = uploadFile.raw
  if (!f) return
  const url = await uploadAvatar(f)
  profile.value.avatar = url
  auth.setUser({ ...auth.user, avatar: url })
  ElMessage.success('头像已更新')
}

// 修改密码
const pwdVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

function openPwd() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  pwdVisible.value = true
}

async function onSubmitPwd() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) return ElMessage.warning('请填写完整')
  if (pwdForm.value.newPassword.length < 6) return ElMessage.warning('新密码至少6位')
  if (pwdForm.value.newPassword !== pwdForm.value.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  pwdVisible.value = false
  auth.logout()
  router.push('/login')
}

// 视频编辑 / 删除
const statusText = { 0: '待审核', 1: '已发布', 2: '已下架', 3: '转码中' }
const categories = ref([])
const editVideoVisible = ref(false)
const editVideoForm = ref({ id: null, title: '', description: '', categoryId: null, tags: '', visibility: 0 })

async function loadCategories() {
  categories.value = await listCategories()
}

function openEditVideo(v) {
  editVideoForm.value = {
    id: v.id,
    title: v.title || '',
    description: v.description || '',
    categoryId: v.categoryId,
    tags: v.tags || '',
    visibility: v.visibility == null ? 0 : v.visibility
  }
  editVideoVisible.value = true
}

async function onSubmitEditVideo() {
  await updateVideo(editVideoForm.value.id, {
    title: editVideoForm.value.title,
    description: editVideoForm.value.description,
    categoryId: editVideoForm.value.categoryId,
    tags: editVideoForm.value.tags,
    visibility: editVideoForm.value.visibility
  })
  ElMessage.success('已保存')
  editVideoVisible.value = false
  loadVideos(videoPage.value)
}

async function onDeleteVideo(v) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除该视频吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteVideo(v.id)
  ElMessage.success('已删除')
  loadVideos(videoPage.value)
}

// 发私信（他人主页）
function sendPm() {
  router.push(`/pm?to=${profile.value.id}`)
}

function goUser(u) {
  if (auth.userId === u.id) router.push('/me')
  else router.push(`/user/${u.id}`)
}

watch(
  targetId,
  (id) => {
    if (!id) return
    activeTab.value = isOwn.value ? 'videos' : 'followings'
    loadProfile()
    onTabChange(activeTab.value)
  },
  { immediate: true }
)
</script>

<template>
  <div class="profile-page">
    <el-card v-if="profile" class="profile-card">
      <div class="p-head">
        <el-upload
          v-if="isOwn"
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :on-change="onAvatarChange"
        >
          <el-avatar :size="80" :src="profile.avatar" class="avatar">
            {{ (profile.nickname || profile.username || '?')[0] }}
          </el-avatar>
        </el-upload>
        <el-avatar v-else :size="80" :src="profile.avatar" class="avatar">
          {{ (profile.nickname || profile.username || '?')[0] }}
        </el-avatar>

        <div class="p-info">
          <div class="p-name">{{ profile.nickname || profile.username }}</div>
          <div class="p-sig">{{ profile.signature || '这个人很懒，什么都没有写~' }}</div>
          <div class="p-stats">
            <span>关注 {{ formatCount(profile.followCount) }}</span>
            <span>粉丝 {{ formatCount(profile.fanCount) }}</span>
          </div>
        </div>

        <div class="p-actions">
          <template v-if="isOwn">
            <el-button size="small" @click="openEdit">编辑资料</el-button>
            <el-button size="small" @click="openPwd">修改密码</el-button>
          </template>
          <template v-else-if="auth.isLogin && auth.userId !== profile.id">
            <el-button
              size="small"
              :type="profile.followed ? 'default' : 'primary'"
              @click="toggleFollow"
            >{{ profile.followed ? '已关注' : '关注' }}</el-button>
            <el-button size="small" @click="sendPm">发私信</el-button>
          </template>
        </div>
      </div>
    </el-card>

    <el-card class="list-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-if="isOwn" label="我的投稿" name="videos">
          <div v-loading="loadingVideos" class="video-grid">
            <div v-for="v in videos" :key="v.id" class="video-item">
              <VideoCard :video="v" />
              <div class="v-status">
                <el-tag size="small" :type="v.status === 1 ? 'success' : v.status === 2 ? 'danger' : 'info'">
                  {{ statusText[v.status] || '未知' }}
                </el-tag>
              </div>
              <div class="v-actions">
                <el-button size="small" @click="openEditVideo(v)">编辑</el-button>
                <el-button size="small" type="danger" :disabled="v.status === 2" @click="onDeleteVideo(v)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-if="!loadingVideos && videos.length === 0" class="empty">还没有投稿</div>
          <el-pagination
            v-if="videoTotal > 8"
            class="pager"
            layout="prev, pager, next"
            :total="videoTotal"
            :page-size="8"
            :current-page="videoPage"
            @current-change="loadVideos"
          />
        </el-tab-pane>

        <el-tab-pane label="关注" name="followings">
          <div v-for="u in fList" :key="u.id" class="user-row" @click="goUser(u)">
            <el-avatar :size="40" :src="u.avatar">{{ (u.nickname || u.username || '?')[0] }}</el-avatar>
            <div class="u-info">
              <div class="u-name">{{ u.nickname || u.username }}</div>
              <div class="u-sig">{{ u.signature || '这个人很懒~' }}</div>
            </div>
          </div>
          <div v-if="fList.length === 0" class="empty">暂无数据</div>
        </el-tab-pane>

        <el-tab-pane label="粉丝" name="fans">
          <div v-for="u in fList" :key="u.id" class="user-row" @click="goUser(u)">
            <el-avatar :size="40" :src="u.avatar">{{ (u.nickname || u.username || '?')[0] }}</el-avatar>
            <div class="u-info">
              <div class="u-name">{{ u.nickname || u.username }}</div>
              <div class="u-sig">{{ u.signature || '这个人很懒~' }}</div>
            </div>
          </div>
          <div v-if="fList.length === 0" class="empty">暂无数据</div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑资料" width="440px">
      <el-form label-width="60px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="editForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" style="width: 100%" />
        </el-form-item>
        <el-form-item label="签名">
          <el-input v-model="editForm.signature" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitPwd">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVideoVisible" title="编辑视频" width="480px" @open="loadCategories">
      <el-form label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="editVideoForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="分区">
          <el-select v-model="editVideoForm.categoryId" placeholder="选择分区" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editVideoForm.tags" placeholder="逗号分隔" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="editVideoForm.visibility">
            <el-radio :value="0">公开</el-radio>
            <el-radio :value="1">仅粉丝</el-radio>
            <el-radio :value="2">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editVideoForm.description" type="textarea" :rows="3" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVideoVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitEditVideo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
}
.profile-card {
  margin-bottom: 16px;
}
.p-head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.avatar {
  cursor: pointer;
  flex-shrink: 0;
}
.p-info {
  flex: 1;
  min-width: 0;
}
.p-name {
  font-size: 20px;
  font-weight: 600;
}
.p-sig {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
}
.p-stats {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 13px;
}
.p-actions {
  flex-shrink: 0;
}
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.video-item {
  display: flex;
  flex-direction: column;
}
.v-status {
  margin-top: 6px;
}
.v-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.u-info {
  min-width: 0;
}
.u-name {
  font-weight: 600;
  font-size: 14px;
}
.u-sig {
  color: #999;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty {
  text-align: center;
  color: #99a2aa;
  padding: 30px 0;
}
.pager {
  justify-content: center;
  margin-top: 16px;
}
@media (max-width: 768px) {
  .p-head {
    flex-wrap: wrap;
    gap: 12px;
  }
  .p-actions {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>
