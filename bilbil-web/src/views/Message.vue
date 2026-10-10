<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMessages, markRead, markAllRead, unreadCount } from '../api/message'
import { useUnreadStore } from '../stores/unread'
import { formatTime } from '../utils/format'

const router = useRouter()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const unread = ref(0)
const loading = ref(false)

const typeMap = {
  1: { label: '点赞', color: '#fb7299' },
  2: { label: '评论', color: '#00a1d6' },
  3: { label: '关注', color: '#7ab80e' },
  4: { label: '回复', color: '#ffa726' },
  5: { label: '系统', color: '#909399' }
}

async function load(p = 1) {
  loading.value = true
  try {
    const res = await listMessages({ page: p, size })
    list.value = res.list || []
    total.value = res.total
    page.value = p
  } finally {
    loading.value = false
  }
}

async function refreshUnread() {
  unread.value = await unreadCount()
}

async function onRead(msg) {
  if (msg.isRead === 0) {
    try {
      await markRead(msg.id)
      msg.isRead = 1
      refreshUnread()
      useUnreadStore().refresh()
    } catch (e) {
      /* ignore */
    }
  }
  if (msg.videoId) router.push(`/video/${msg.videoId}`)
  else if (msg.senderId) router.push(`/user/${msg.senderId}`)
}

async function onMarkAll() {
  await markAllRead()
  list.value.forEach((m) => (m.isRead = 1))
  unread.value = 0
  useUnreadStore().refresh()
  ElMessage.success('已全部标记为已读')
}

onMounted(() => {
  load(1)
  refreshUnread()
})
</script>

<template>
  <div class="message-page">
    <el-card>
      <template #header>
        <div class="m-head">
          <span class="m-title">消息中心 <span class="unread">（{{ unread }} 条未读）</span></span>
          <el-button size="small" :disabled="unread === 0" @click="onMarkAll">全部已读</el-button>
        </div>
      </template>

      <div v-loading="loading">
        <div
          v-for="m in list"
          :key="m.id"
          class="msg-item"
          :class="{ unread: m.isRead === 0 }"
          @click="onRead(m)"
        >
          <el-avatar :size="40" :src="m.senderAvatar">{{ (m.senderName || '系')[0] }}</el-avatar>
          <div class="m-body">
            <div class="m-line1">
              <el-tag size="small" :color="(typeMap[m.type] || typeMap[5]).color" class="m-tag">
                {{ (typeMap[m.type] || typeMap[5]).label }}
              </el-tag>
              <span class="m-sender">{{ m.senderName || '系统' }}</span>
              <span class="m-time">{{ formatTime(m.createTime) }}</span>
            </div>
            <div class="m-content">{{ m.content }}</div>
          </div>
          <span v-if="m.isRead === 0" class="dot"></span>
        </div>

        <div v-if="!loading && list.length === 0" class="empty">暂无消息</div>

        <el-pagination
          v-if="total > size"
          class="pager"
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.message-page {
  max-width: 760px;
  margin: 0 auto;
}
.m-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.m-title {
  font-weight: 600;
}
.unread {
  color: #fb7299;
  font-size: 13px;
  font-weight: 400;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  position: relative;
}
.msg-item:hover {
  background: #fafafa;
}
.msg-item.unread .m-content {
  font-weight: 600;
}
.m-body {
  flex: 1;
  min-width: 0;
}
.m-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
}
.m-tag {
  border: none;
  color: #fff;
}
.m-sender {
  font-size: 14px;
  font-weight: 500;
}
.m-time {
  font-size: 12px;
  color: #99a2aa;
  margin-left: auto;
}
.m-content {
  margin-top: 6px;
  font-size: 14px;
  color: #444;
  line-height: 20px;
  word-break: break-word;
}
.dot {
  position: absolute;
  right: 4px;
  top: 18px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fb7299;
}
.empty {
  text-align: center;
  color: #99a2aa;
  padding: 40px 0;
}
.pager {
  justify-content: center;
  margin-top: 16px;
}
</style>
