<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listHistory, removeHistory, clearHistory } from '../api/history'
import { formatTime, formatDuration } from '../utils/format'

const router = useRouter()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load(p = 1) {
  loading.value = true
  try {
    const res = await listHistory({ page: p, size })
    list.value = res.list || []
    total.value = res.total
    page.value = p
  } finally {
    loading.value = false
  }
}

async function onRemove(item) {
  try {
    await ElMessageBox.confirm('确定删除这条观看记录吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await removeHistory(item.videoId)
  ElMessage.success('已删除')
  load(page.value)
}

async function onClear() {
  if (total.value === 0) return
  try {
    await ElMessageBox.confirm('确定清空全部观看历史吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await clearHistory()
  ElMessage.success('已清空')
  load(1)
}

function goPlay(videoId) {
  router.push(`/video/${videoId}`)
}

onMounted(() => load(1))
</script>

<template>
  <div class="history-page">
    <el-card>
      <template #header>
        <div class="h-head">
          <span class="h-title">观看历史</span>
          <el-button size="small" :disabled="total === 0" @click="onClear">清空全部</el-button>
        </div>
      </template>

      <div v-loading="loading">
        <div v-for="item in list" :key="item.videoId" class="h-item">
          <div class="h-cover" @click="goPlay(item.videoId)">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="" />
            <div v-else class="no-cover">🎬</div>
            <span v-if="item.duration" class="duration">{{ formatDuration(item.duration) }}</span>
          </div>
          <div class="h-info" @click="goPlay(item.videoId)">
            <div class="h-title" :title="item.title">{{ item.title }}</div>
            <div class="h-meta">
              <span>看到 {{ formatDuration(item.progress) }} / {{ formatDuration(item.duration) }}</span>
              <span>{{ formatTime(item.watchTime) }}</span>
              <span v-if="item.authorName">{{ item.authorName }}</span>
            </div>
          </div>
          <el-button size="small" text type="danger" @click="onRemove(item)">删除</el-button>
        </div>

        <div v-if="!loading && list.length === 0" class="empty">暂无观看记录</div>

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
.history-page {
  max-width: 820px;
  margin: 0 auto;
}
.h-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.h-title {
  font-weight: 600;
}
.h-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 8px;
  border-bottom: 1px solid #f5f5f5;
}
.h-cover {
  position: relative;
  width: 160px;
  height: 90px;
  flex-shrink: 0;
  cursor: pointer;
  background: #e5e9ef;
  border-radius: 6px;
  overflow: hidden;
}
.h-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}
.duration {
  position: absolute;
  right: 4px;
  bottom: 4px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 3px;
}
.h-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.h-title {
  font-size: 14px;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.h-meta {
  margin-top: 6px;
  display: flex;
  gap: 14px;
  color: #999;
  font-size: 12px;
  flex-wrap: wrap;
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
@media (max-width: 768px) {
  .h-cover {
    width: 120px;
    height: 68px;
  }
}
</style>
