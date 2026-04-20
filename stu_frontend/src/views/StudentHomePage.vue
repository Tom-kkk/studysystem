<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import StudentHome from '../components/StudentHome.vue'
import { clearStudentLoginUser, studentLoginUser } from '../stores/studentAuth'

const router = useRouter()
const student = computed(() => studentLoginUser.value)

function handleLogout() {
  clearStudentLoginUser()
  router.push('/student/login')
}
</script>

<template>
  <main class="page">
    <section class="page-shell" style="max-width: 980px">
      <header class="top-nav">
        <nav class="links">
          <router-link to="/student/home">学生首页</router-link>
          <router-link to="/student/login">登录页</router-link>
        </nav>
        <button type="button" class="logout-btn" @click="handleLogout">安全退出</button>
      </header>
      <StudentHome :student="student" />
    </section>
  </main>
</template>

