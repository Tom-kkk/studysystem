<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { forgetPassword, sendEmailCheckcode } from '../services/studentPasswordApi'

const router = useRouter()

const form = ref({
  userName: '',
  email: '',
  emailCheckcode: '',
  newPass: '',
  confirmPass: '',
})
const loading = ref(false)
const sendingCode = ref(false)
const resendSeconds = ref(0)
const msg = ref('')
const errorMsg = ref('')
let resendTimer = null

function startResendCountdown() {
  resendSeconds.value = 60
  if (resendTimer) {
    clearInterval(resendTimer)
  }
  resendTimer = setInterval(() => {
    if (resendSeconds.value <= 1) {
      resendSeconds.value = 0
      clearInterval(resendTimer)
      resendTimer = null
      return
    }
    resendSeconds.value -= 1
  }, 1000)
}

onBeforeUnmount(() => {
  if (resendTimer) {
    clearInterval(resendTimer)
    resendTimer = null
  }
})

async function onSendCode() {
  if (!form.value.email.trim()) {
    errorMsg.value = '请先输入邮箱'
    return
  }
  if (resendSeconds.value > 0) {
    return
  }
  sendingCode.value = true
  errorMsg.value = ''
  msg.value = ''
  try {
    await sendEmailCheckcode(form.value.email.trim())
    msg.value = '验证码已发送，请检查邮箱'
    startResendCountdown()
  } catch (e) {
    errorMsg.value = e?.message || '发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function onSubmit() {
  if (!form.value.userName.trim() || !form.value.email.trim() || !form.value.emailCheckcode.trim()) {
    errorMsg.value = '请完整填写用户名、邮箱和验证码'
    return
  }
  if (!form.value.newPass || form.value.newPass.length < 6) {
    errorMsg.value = '新密码至少 6 位'
    return
  }
  if (form.value.newPass !== form.value.confirmPass) {
    errorMsg.value = '两次新密码不一致'
    return
  }
  loading.value = true
  errorMsg.value = ''
  msg.value = ''
  try {
    await forgetPassword({
      userName: form.value.userName.trim(),
      email: form.value.email.trim(),
      emailCheckcode: form.value.emailCheckcode.trim(),
      newPass: form.value.newPass,
      confirmPass: form.value.confirmPass,
    })
    msg.value = '密码重置成功，正在返回登录页'
    setTimeout(() => router.push('/student/login'), 900)
  } catch (e) {
    errorMsg.value = e?.message || '重置失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card">
    <h1>学生忘记密码</h1>
    <p class="sub-title">通过预留邮箱验证码重置密码</p>
    <form class="form" @submit.prevent="onSubmit">
      <label class="field">
        <span>学号</span>
        <input v-model="form.userName" type="text" placeholder="请输入学号" />
      </label>
      <label class="field">
        <span>预留邮箱</span>
        <input v-model="form.email" type="email" placeholder="请输入预留邮箱" />
      </label>
      <label class="field">
        <span>邮箱验证码</span>
        <div class="inline-row">
          <input v-model="form.emailCheckcode" type="text" placeholder="请输入验证码" />
          <button
            type="button"
            class="small-btn"
            :disabled="sendingCode || resendSeconds > 0"
            @click="onSendCode"
          >
            {{ sendingCode ? '发送中...' : resendSeconds > 0 ? `${resendSeconds}s后重试` : '获取验证码' }}
          </button>
        </div>
      </label>
      <label class="field">
        <span>新密码</span>
        <input v-model="form.newPass" type="password" placeholder="请输入新密码（至少6位）" />
      </label>
      <label class="field">
        <span>确认新密码</span>
        <input v-model="form.confirmPass" type="password" placeholder="请再次输入新密码" />
      </label>
      <button type="submit" :disabled="loading">
        {{ loading ? '提交中...' : '重置密码' }}
      </button>
    </form>
    <p v-if="msg" class="success">{{ msg }}</p>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <router-link class="primary-link" to="/student/login">返回学生登录</router-link>
  </section>
</template>

<style scoped>
.inline-row {
  display: flex;
  gap: 8px;
}

.inline-row input {
  flex: 1;
}

.small-btn {
  margin-top: 0;
  min-width: 108px;
}
</style>

