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
    errorMsg.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const user = await login({
      userName: form.value.userName.trim(),
      password: form.value.password,
    })
    if (user?.roleType && user.roleType !== 'teacher') {
      throw new Error('当前账号不是教师角色')
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
    <h1>教师登录</h1>
    <p class="sub-title">登录成功后进入教师界面</p>

    <form class="form" @submit.prevent="handleLogin">
      <label class="field">
        <span>用户名</span>
        <input v-model="form.userName" type="text" placeholder="请输入教师用户名" />
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
  </section>
</template>

