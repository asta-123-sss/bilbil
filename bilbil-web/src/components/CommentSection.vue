<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listComments, listReplies, publishComment, deleteComment } from '../api/comment'
import { useAuthStore } from '../stores/auth'
import { formatTime } from '../utils/format'

const props = defineProps({
  videoId: { type: [Number, String], required: true }
})

const auth = useAuthStore()

const comments = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const content = ref('')

// rootId -> { list, total, page, loading, expanded }
const replyMap = ref({})

// 正在回复的评论
const replyTarget = ref(null)
const replyContent = ref('')

async function loadComments(p = 1) {
  loading.value = true
  try {
    const res = await listComments({ videoId: props.videoId, page: p, size })
    comments.value = res.list || []
    total.value = res.total
    page.value = p
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (!auth.isLogin) {
    ElMessage.warning('请先登录')
    return false
  }
  return true
}

async function onSubmitComment() {
  if (!requireLogin()) return
  const text = content.value.trim()
  if (!text) return
  await publishComment({ videoId: Number(props.videoId), content: text, parentId: 0 })
  content.value = ''
  ElMessage.success('评论成功')
  loadComments(1)
}

async function toggleReplies(c) {
  const slot = getSlot(c.id)
  slot.expanded = !slot.expanded
  if (!slot.expanded || slot.list.length > 0) return
  await loadReplies(c, slot, 1)
}

async function loadReplies(c, slot, p) {
  slot.loading = true
  try {
    const res = await listReplies({ videoId: props.videoId, rootId: c.id, page: p, size: 5 })
    if (p === 1) slot.list = res.list || []
    else slot.list = slot.list.concat(res.list || [])
    slot.total = res.total
    slot.page = p
  } finally {
    slot.loading = false
  }
}

function getSlot(rootId) {
  if (!replyMap.value[rootId]) {
    replyMap.value[rootId] = { list: [], total: 0, page: 0, loading: false, expanded: false }
  }
  return replyMap.value[rootId]
}

function startReply(c) {
  if (!requireLogin()) return
  replyTarget.value = c
  replyContent.value = ''
}

async function onSubmitReply() {
  const target = replyTarget.value
  if (!target) return
  const text = replyContent.value.trim()
  if (!text) return
  await publishComment({
    videoId: Number(props.videoId),
    content: text,
    parentId: target.id,
    replyUserId: target.userId
  })
  replyContent.value = ''
  replyTarget.value = null
  ElMessage.success('回复成功')

  // 刷新所属根评论的回复列表
  const rootId = target.rootId && target.rootId !== 0 ? target.rootId : target.id
  const rootComment = comments.value.find((x) => x.id === rootId)
  if (rootComment) {
    const slot = getSlot(rootId)
    slot.list = []
    slot.total = 0
    slot.expanded = true
    rootComment.replyCount = (rootComment.replyCount || 0) + 1
    await loadReplies(rootComment, slot, 1)
  }
}

async function onDelete(c) {
  await deleteComment(c.id)
  ElMessage.success('删除成功')
  const isReply = c.parentId && c.parentId !== 0
  if (isReply) {
    const rootId = c.rootId
    const rootComment = comments.value.find((x) => x.id === rootId)
    if (rootComment) {
      rootComment.replyCount = Math.max(0, (rootComment.replyCount || 1) - 1)
      const slot = getSlot(rootId)
      slot.list = []
      slot.total = 0
      await loadReplies(rootComment, slot, 1)
    }
  } else {
    loadComments(page.value)
  }
}

function canDelete(c) {
  return auth.isLogin && auth.user && c.userId === auth.user.id
}

onMounted(() => loadComments(1))
</script>

<template>
  <div class="comment-section">
    <h3 class="cs-title">评论 <span class="cnt">{{ total }}</span></h3>

    <div class="publish">
      <el-avatar :size="32" :src="auth.user && auth.user.avatar">
        {{ auth.user ? (auth.user.nickname || auth.user.username || '?')[0] : '?' }}
      </el-avatar>
      <el-input
        v-model="content"
        type="textarea"
        :rows="2"
        maxlength="1000"
        placeholder="发一条友善的评论"
      />
      <el-button type="primary" @click="onSubmitComment">发布</el-button>
    </div>

    <div v-loading="loading" class="comment-list">
      <div v-for="c in comments" :key="c.id" class="comment">
        <el-avatar :size="36" :src="c.userAvatar">{{ (c.userName || '?')[0] }}</el-avatar>
        <div class="c-body">
          <div class="c-head">
            <span class="c-name">{{ c.userName }}</span>
            <span class="c-time">{{ formatTime(c.createTime) }}</span>
          </div>
          <div class="c-content">{{ c.content }}</div>
          <div class="c-actions">
            <el-button text size="small" @click="startReply(c)">回复</el-button>
            <el-button
              v-if="c.replyCount > 0"
              text
              size="small"
              @click="toggleReplies(c)"
            >
              {{ getSlot(c.id).expanded ? '收起' : '展开' }} {{ c.replyCount }} 条回复
            </el-button>
            <el-button v-if="canDelete(c)" text size="small" type="danger" @click="onDelete(c)">
              删除
            </el-button>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyTarget && replyTarget.id === c.id" class="reply-box">
            <el-input
              v-model="replyContent"
              size="small"
              :placeholder="`回复 @${c.userName}`"
              @keyup.enter="onSubmitReply"
            />
            <el-button size="small" type="primary" @click="onSubmitReply">发送</el-button>
            <el-button size="small" @click="replyTarget = null">取消</el-button>
          </div>

          <!-- 回复列表 -->
          <div v-if="getSlot(c.id).expanded" class="replies">
            <div v-for="r in getSlot(c.id).list" :key="r.id" class="reply">
              <div class="reply-line">
                <span class="r-name">{{ r.userName }}</span>
                <span v-if="r.replyUserName" class="r-to"> 回复 @{{ r.replyUserName }}</span>
                <span class="r-text">：{{ r.content }}</span>
              </div>
              <div class="reply-meta">
                <span class="c-time">{{ formatTime(r.createTime) }}</span>
                <el-button text size="small" @click="startReply(r)">回复</el-button>
                <el-button v-if="canDelete(r)" text size="small" type="danger" @click="onDelete(r)">
                  删除
                </el-button>
              </div>
              <!-- 回复某条回复的输入框 -->
              <div v-if="replyTarget && replyTarget.id === r.id" class="reply-box">
                <el-input
                  v-model="replyContent"
                  size="small"
                  :placeholder="`回复 @${r.userName}`"
                  @keyup.enter="onSubmitReply"
                />
                <el-button size="small" type="primary" @click="onSubmitReply">发送</el-button>
                <el-button size="small" @click="replyTarget = null">取消</el-button>
              </div>
            </div>
            <el-button
              v-if="getSlot(c.id).list.length < getSlot(c.id).total"
              text
              size="small"
              @click="loadReplies(c, getSlot(c.id), getSlot(c.id).page + 1)"
            >
              加载更多回复
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="!loading && comments.length === 0" class="empty">还没有评论，快来抢沙发~</div>

      <el-pagination
        v-if="total > size"
        class="pager"
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="loadComments"
      />
    </div>
  </div>
</template>

<style scoped>
.comment-section {
  margin-top: 16px;
}
.cs-title {
  font-size: 16px;
  margin: 0 0 12px;
}
.cs-title .cnt {
  color: #999;
  font-weight: 400;
}
.publish {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.publish .el-input {
  flex: 1;
}
.comment-list {
  min-height: 80px;
}
.comment {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.c-body {
  flex: 1;
}
.c-head {
  display: flex;
  gap: 10px;
  align-items: center;
}
.c-name {
  color: #6d757a;
  font-size: 13px;
  font-weight: 500;
}
.c-time {
  color: #99a2aa;
  font-size: 12px;
}
.c-content {
  margin: 6px 0;
  font-size: 14px;
  line-height: 22px;
  word-break: break-word;
}
.c-actions {
  display: flex;
  gap: 4px;
}
.reply-box {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}
.replies {
  margin-top: 8px;
  padding: 8px 10px;
  background: #f6f7f8;
  border-radius: 6px;
}
.reply {
  padding: 6px 0;
  font-size: 13px;
  line-height: 20px;
}
.reply-line {
  display: inline;
}
.r-name {
  color: #6d757a;
  font-weight: 500;
}
.r-to {
  color: #6d757a;
}
.r-text {
  word-break: break-word;
}
.reply-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}
.empty {
  text-align: center;
  color: #99a2aa;
  padding: 30px 0;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
