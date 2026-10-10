<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const router = useRouter()

const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  confirm: '',
  nickname: '',
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在3-20之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6-32之间', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) => {
        if (value !== form.value.password) cb(new Error('两次输入的密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      username: form.value.username,
      password: form.value.password,
      nickname: form.value.nickname || undefined,
      email: form.value.email || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="用户名（3-20位）" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码（6-32位）" />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" size="large" type="password" show-password placeholder="确认密码" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="昵称（选填）" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" size="large" placeholder="邮箱（选填）" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
          注册
        </el-button>
      </el-form>
      <div class="auth-foot">
        已有账号？
        <el-link type="primary" @click="router.push('/login')">去登录</el-link>
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
@media (max-width: 768px) {
  .auth-card {
    width: 100%;
    max-width: 360px;
    padding: 24px 16px;
  }
}
</style>
