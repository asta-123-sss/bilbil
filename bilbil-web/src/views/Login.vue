<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/user'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form.value)
    auth.setLogin(res.token, res.user)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

function quickSwitch(acc) {
  auth.switchAccount(acc)
  ElMessage.success(`已切换到 ${acc.user.nickname || acc.user.username}`)
  router.push(route.query.redirect || '/')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>
      <div v-if="auth.accounts.length" class="saved-accounts">
        <div class="saved-title">已记住的账号，点击直接登录</div>
        <div v-for="acc in auth.accounts" :key="acc.user.id" class="saved-item" @click="quickSwitch(acc)">
          <el-avatar :size="28" :src="acc.user.avatar">
            {{ (acc.user.nickname || acc.user.username)[0] }}
          </el-avatar>
          <span class="saved-name">{{ acc.user.nickname || acc.user.username }}</span>
        </div>
      </div>
      <div class="auth-foot">
        还没有账号？
        <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 96px);
}
.auth-card {
  width: 360px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
.auth-title {
  margin: 0 0 24px;
  text-align: center;
  color: #fb7299;
}
.submit {
  width: 100%;
}
.auth-foot {
  margin-top: 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.saved-accounts {
  margin-top: 16px;
  border-top: 1px solid #eee;
  padding-top: 12px;
}
.saved-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.saved-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.saved-item:hover {
  background: #f5f6f7;
}
.saved-name {
  color: #333;
  font-size: 14px;
}
@media (max-width: 768px) {
  .auth-card {
    width: 100%;
    max-width: 360px;
    padding: 24px 16px;
  }
}
</style>
