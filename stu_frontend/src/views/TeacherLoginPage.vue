<script setup>
import { useRoute, useRouter } from 'vue-router'
import TeacherLogin from '../components/TeacherLogin.vue'
import { setLoginUser } from '../stores/auth'

const router = useRouter()
const route = useRoute()

function handleLoginSuccess(user) {
  setLoginUser(user)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/teacher/home'
  router.push(redirect)
}
</script>

<template>
  <main class="page">
    <TeacherLogin @login-success="handleLoginSuccess" />
  </main>
</template>

