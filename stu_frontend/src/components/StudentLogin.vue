<script setup>
import { ref } from 'vue'
import { login } from '../services/authApi'

const emit = defineEmits(['login-success'])

const form = ref({
  userName: '',
  password: '',
})
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (!form.value.userName.trim() || !form.value.password) {
    errorMsg.value = '请输入学号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const user = await login({
      userName: form.value.userName.trim(),
      password: form.value.password,
    })
    if (user?.roleType && user.roleType !== 'student') {
      throw new Error('当前账号不是学生角色')
    }
    emit('login-success', user)
  } catch (e) {
    errorMsg.value = e?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card">
    <h1>学生登录</h1>
    <p class="sub-title">登录后进入实验报告上传界面</p>
    <form class="form" @submit.prevent="handleLogin">
      <label class="field">
        <span>学号</span>
        <input v-model="form.userName" type="text" placeholder="请输入学号" />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </label>
      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <router-link class="primary-link" to="/student/forget-password">忘记密码？去重置</router-link>
  </section>
</template>

