<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listConversations, chatWith, sendPm } from '../api/pm'
import { getUserProfile } from '../api/user'
import { useAuthStore } from '../stores/auth'
import { useUnreadStore } from '../stores/unread'
import { formatTime } from '../utils/format'

const route = useRoute()
const auth = useAuthStore()

const conversations = ref([])
const activeId = ref(null)
const activeUser = ref({ id: null, name: '', avatar: '' })
const messages = ref([])
const input = ref('')
const listRef = ref()

async function loadConversations() {
  conversations.value = await listConversations()
}

async function openChat(id, name, avatar) {
  activeId.value = id
  activeUser.value = { id, name: name || '', avatar: avatar || '' }
  messages.value = await chatWith(id)
  const conv = conversations.value.find((c) => c.otherUserId === id)
  if (conv) conv.unreadCount = 0
  useUnreadStore().refresh()
  scrollToBottom()
}

async function onSend() {
  const text = input.value.trim()
  if (!text) return
  if (!activeId.value) return
  const msg = await sendPm({ toUserId: activeId.value, content: text })
  messages.value.push(msg)
  input.value = ''
  await loadConversations()
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

onMounted(async () => {
  await loadConversations()
  const to = Number(route.query.to)
  if (to) {
    const conv = conversations.value.find((c) => c.otherUserId === to)
    if (conv) {
      await openChat(conv.otherUserId, conv.otherUserName, conv.otherUserAvatar)
    } else {
      try {
        const p = await getUserProfile(to)
        await openChat(to, p.nickname || p.username, p.avatar)
      } catch (e) {
        ElMessage.error('无法打开该会话')
      }
    }
  }
})
</script>

<template>
  <div class="pm-page" :class="{ 'chat-open': activeId }">
    <el-card class="pm-card">
      <div class="pm-left">
        <div class="pm-left-head">私信</div>
        <div v-if="conversations.length === 0" class="empty">暂无会话</div>
        <div
          v-for="c in conversations"
          :key="c.otherUserId"
          class="conv-item"
          :class="{ active: c.otherUserId === activeId }"
          @click="openChat(c.otherUserId, c.otherUserName, c.otherUserAvatar)"
        >
          <el-avatar :size="40" :src="c.otherUserAvatar">{{ (c.otherUserName || '?')[0] }}</el-avatar>
          <div class="conv-info">
            <div class="conv-line1">
              <span class="conv-name">{{ c.otherUserName || '用户' }}</span>
              <span class="conv-time">{{ formatTime(c.lastTime) }}</span>
            </div>
            <div class="conv-line2">
              <span class="conv-last">{{ c.lastContent }}</span>
              <el-badge v-if="c.unreadCount > 0" :value="c.unreadCount" :max="99" />
            </div>
          </div>
        </div>
      </div>

      <div class="pm-right">
        <template v-if="activeId">
          <div class="pm-head">
            <el-button class="pm-back" text size="small" @click="activeId = null">
              <el-icon><Back /></el-icon> 返回
            </el-button>
            <span>{{ activeUser.name || '私信' }}</span>
          </div>
          <div ref="listRef" class="pm-msgs">
            <div
              v-for="m in messages"
              :key="m.id"
              class="pm-msg"
              :class="m.fromUserId === auth.userId ? 'mine' : 'other'"
            >
              <div class="bubble">{{ m.content }}</div>
            </div>
            <div v-if="messages.length === 0" class="empty">和 TA 说点什么吧~</div>
          </div>
          <div class="pm-input">
            <el-input
              v-model="input"
              type="textarea"
              :rows="3"
              maxlength="500"
              placeholder="发送私信..."
              @keyup.enter.exact.prevent="onSend"
            />
            <el-button type="primary" class="send-btn" @click="onSend">发送</el-button>
          </div>
        </template>
        <div v-else class="pm-placeholder">选择左侧会话开始聊天</div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.pm-page {
  max-width: 960px;
  margin: 0 auto;
}
.pm-card :deep(.el-card__body) {
  padding: 0;
  display: flex;
  height: 600px;
}
.pm-left {
  width: 280px;
  border-right: 1px solid #f0f0f0;
  overflow-y: auto;
  flex-shrink: 0;
}
.pm-left-head {
  padding: 14px 16px;
  font-weight: 600;
  border-bottom: 1px solid #f5f5f5;
}
.conv-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
}
.conv-item:hover,
.conv-item.active {
  background: #f5f6f7;
}
.conv-info {
  flex: 1;
  min-width: 0;
}
.conv-line1 {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.conv-name {
  font-size: 14px;
  font-weight: 500;
}
.conv-time {
  font-size: 12px;
  color: #99a2aa;
  flex-shrink: 0;
}
.conv-line2 {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.conv-last {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.pm-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.pm-head {
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.pm-back {
  display: none;
  margin-right: 4px;
}
.pm-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #fafafa;
}
.pm-msg {
  display: flex;
  margin-bottom: 12px;
}
.pm-msg.mine {
  justify-content: flex-end;
}
.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 20px;
  word-break: break-word;
  background: #fff;
  border: 1px solid #f0f0f0;
}
.pm-msg.mine .bubble {
  background: #fb7299;
  color: #fff;
  border: none;
}
.pm-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}
.send-btn {
  flex-shrink: 0;
}
.pm-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #99a2aa;
}
.empty {
  text-align: center;
  color: #99a2aa;
  padding: 20px 0;
  font-size: 13px;
}
@media (max-width: 768px) {
  .pm-card :deep(.el-card__body) {
    height: calc(100vh - 96px);
  }
  .pm-left {
    width: 100%;
  }
  .pm-right {
    display: none;
  }
  .pm-page.chat-open .pm-left {
    display: none;
  }
  .pm-page.chat-open .pm-right {
    display: flex;
    width: 100%;
  }
  .pm-back {
    display: inline-flex;
  }
}
</style>
