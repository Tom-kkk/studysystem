<script setup>
import { useRoute, useRouter } from 'vue-router'
import StudentLogin from '../components/StudentLogin.vue'
import { setStudentLoginUser } from '../stores/studentAuth'

const router = useRouter()
const route = useRoute()

function handleLoginSuccess(user) {
  setStudentLoginUser(user)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/student/home'
  router.push(redirect)
}
</script>

<template>
  <main class="page">
    <StudentLogin @login-success="handleLoginSuccess" />
  </main>
</template>

