<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories } from '../api/category'
import { mergeUpload, uploadCover } from '../api/video'
import { chunkUpload } from '../utils/chunkUpload'

const router = useRouter()

const categories = ref([])
const file = ref(null)
const fileName = ref('')
const fileSize = ref(0)
const status = ref('idle') // idle | hashing | uploading | done | instant | error
const progress = ref(0)
const uploaded = ref(0)
const total = ref(0)
const uploadId = ref('')
const instantVideoId = ref(null)
const duration = ref(0)

const form = ref({
  title: '',
  description: '',
  categoryId: null,
  tags: '',
  visibility: 0
})

const coverUrl = ref('')
const coverUploading = ref(false)
const submitting = ref(false)

const sizeText = computed(() => (fileSize.value / 1024 / 1024).toFixed(2) + ' MB')
const progressText = computed(() => {
  switch (status.value) {
    case 'hashing':
      return '正在计算文件指纹（MD5）…'
    case 'uploading':
      return `正在上传分片 ${uploaded.value}/${total.value}`
    case 'done':
      return '文件上传完成'
    case 'instant':
      return '秒传成功'
    case 'error':
      return '上传失败，请重新选择文件'
    default:
      return '等待选择文件'
  }
})

async function loadCategories() {
  categories.value = (await listCategories()) || []
}
loadCategories()

function readDuration(f) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(f)
    const v = document.createElement('video')
    v.preload = 'metadata'
    v.onloadedmetadata = () => {
      URL.revokeObjectURL(url)
      resolve(Math.round(v.duration) || 0)
    }
    v.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(0)
    }
    v.src = url
  })
}

async function onVideoChange(uploadFile) {
  const f = uploadFile.raw
  if (!f) return
  file.value = f
  fileName.value = f.name
  fileSize.value = f.size
  uploadId.value = ''
  coverUrl.value = ''
  // 默认标题取文件名
  if (!form.value.title) form.value.title = f.name.replace(/\.[^.]+$/, '')
  duration.value = await readDuration(f)
  await startUpload(f)
}

async function startUpload(f) {
  status.value = 'hashing'
  progress.value = 0
  uploaded.value = 0
  total.value = 0
  try {
    const res = await chunkUpload(f, (u, t) => {
      if (status.value === 'hashing') status.value = 'uploading'
      uploaded.value = u
      total.value = t
      progress.value = t ? Math.round((u / t) * 100) : 0
    })
    uploadId.value = res.uploadId
    if (res.alreadyExists) {
      status.value = 'instant'
      instantVideoId.value = res.videoId
      ElMessage.success('秒传成功：该文件已上传过，可直接观看')
    } else {
      status.value = 'done'
      ElMessage.success('文件上传完成，请填写视频信息')
    }
  } catch (e) {
    status.value = 'error'
  }
}

async function onCoverChange(uploadFile) {
  const f = uploadFile.raw
  if (!f) return
  coverUploading.value = true
  try {
    coverUrl.value = await uploadCover(f)
    ElMessage.success('封面上传成功')
  } finally {
    coverUploading.value = false
  }
}

function goInstant() {
  if (instantVideoId.value) router.push(`/video/${instantVideoId.value}`)
}

async function onPublish() {
  if (!form.value.title.trim()) return ElMessage.warning('请填写标题')
  if (!form.value.categoryId) return ElMessage.warning('请选择分区')
  if (!uploadId.value) return ElMessage.warning('请先上传视频文件')
  submitting.value = true
  try {
    const videoId = await mergeUpload({
      uploadId: uploadId.value,
      title: form.value.title.trim(),
      description: form.value.description || undefined,
      categoryId: form.value.categoryId,
      tags: form.value.tags || undefined,
      coverUrl: coverUrl.value || undefined,
      visibility: form.value.visibility,
      duration: duration.value || undefined
    })
    ElMessage.success('发布成功')
    router.push(`/video/${videoId}`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="upload-page">
    <el-card class="up-card">
      <template #header>视频上传</template>

      <!-- 视频文件选择 + 进度 -->
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept="video/*"
        :on-change="onVideoChange"
      >
        <div class="drop">
          <el-icon class="drop-icon"><UploadFilled /></el-icon>
          <div class="drop-main" v-if="!file">将视频拖到此处，或<em>点击上传</em></div>
          <div class="drop-main" v-else>
            <div class="fname">{{ fileName }}</div>
            <div class="fsize">{{ sizeText }}</div>
          </div>
        </div>
      </el-upload>

      <div v-if="file" class="progress-area">
        <el-progress
          :percentage="progress"
          :status="status === 'error' ? 'exception' : status === 'done' || status === 'instant' ? 'success' : ''"
        />
        <div class="ptext">{{ progressText }}</div>
        <el-button v-if="status === 'instant'" type="success" @click="goInstant">前往观看</el-button>
      </div>

      <!-- 元信息表单 -->
      <el-form label-width="80px" class="up-form">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" placeholder="视频标题" />
        </el-form-item>
        <el-form-item label="分区" required>
          <el-select v-model="form.categoryId" placeholder="选择分区" style="width: 200px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" maxlength="500" placeholder="逗号分隔，如：教程,后端" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" placeholder="视频简介" />
        </el-form-item>
        <el-form-item label="权限">
          <el-radio-group v-model="form.visibility">
            <el-radio :value="0">公开</el-radio>
            <el-radio :value="1">仅粉丝</el-radio>
            <el-radio :value="2">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="onCoverChange"
          >
            <div class="cover-picker" v-loading="coverUploading">
              <img v-if="coverUrl" :src="coverUrl" alt="封面" />
              <div v-else class="cover-empty">点击上传封面</div>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="status !== 'done' && status !== 'instant'"
            @click="onPublish"
          >发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.upload-page {
  max-width: 760px;
  margin: 0 auto;
}
.drop {
  padding: 20px 0;
  text-align: center;
}
.drop-icon {
  font-size: 44px;
  color: #c0c4cc;
}
.drop-main {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}
.drop-main em {
  color: #fb7299;
  font-style: normal;
}
.fname {
  font-size: 15px;
  color: #303133;
  word-break: break-all;
}
.fsize {
  font-size: 12px;
  color: #909399;
}
.progress-area {
  margin: 16px 0;
}
.ptext {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
.up-form {
  margin-top: 20px;
}
.cover-picker {
  width: 240px;
  height: 135px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-picker img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-empty {
  color: #909399;
  font-size: 13px;
}
</style>
