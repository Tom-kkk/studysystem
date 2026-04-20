<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginUser } from '../stores/auth'
import { createCourse } from '../services/courseApi'

const router = useRouter()
const form = ref({
  courseName: '',
  className: '',
  file: null,
})
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const fileInputRef = ref(null)

function onFileChange(event) {
  const file = event.target.files?.[0] || null
  form.value.file = file
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

function removeFile() {
  form.value.file = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function fileSizeText(size = 0) {
  if (!size) return '0 KB'
  const mb = size / (1024 * 1024)
  if (mb >= 1) return `${mb.toFixed(2)} MB`
  return `${Math.max(1, Math.round(size / 1024))} KB`
}

async function handleSubmit() {
  errorMsg.value = ''
  successMsg.value = ''
  if (!form.value.courseName.trim() || !form.value.className.trim()) {
    errorMsg.value = '课程名和班级不能为空'
    return
  }
  if (!form.value.file) {
    errorMsg.value = '请上传 Excel 文件'
    return
  }
  if (!form.value.file.name.toLowerCase().endsWith('.xlsx')) {
    errorMsg.value = '只支持 .xlsx 文件'
    return
  }
  if (form.value.file.size > 5 * 1024 * 1024) {
    errorMsg.value = '文件大小不能超过 5MB'
    return
  }
  loading.value = true
  try {
    const teacherId = loginUser.value?.userName
    await createCourse({
      teacherId,
      courseName: form.value.courseName.trim(),
      className: form.value.className.trim(),
      file: form.value.file,
    })
    successMsg.value = '提交成功，课程已创建'
    form.value.courseName = ''
    form.value.className = ''
    removeFile()
  } catch (e) {
    errorMsg.value = e?.message || '提交失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="page">
    <section class="page-shell">
      <header class="top-nav">
        <nav class="links">
          <router-link to="/teacher/home">教师首页</router-link>
          <router-link to="/teacher/course/new">新建课程</router-link>
        </nav>
        <button type="button" class="logout-btn" @click="router.push('/teacher/home')">返回</button>
      </header>
      <section class="card teacher-home">
        <h1>新建课程</h1>
        <p class="sub-title">请上传符合模板的教学班 Excel（.xlsx）</p>
        <form class="form" @submit.prevent="handleSubmit">
          <label class="field">
            <span>课程名</span>
            <input v-model="form.courseName" type="text" placeholder="例如：程序设计实验" />
          </label>
          <label class="field">
            <span>班级</span>
            <input v-model="form.className" type="text" placeholder="例如：201" />
          </label>
          <div class="field">
            <span>Excel 文件</span>
            <div class="upload-card" :class="{ selected: form.file }">
              <input ref="fileInputRef" class="hidden-file-input" type="file" accept=".xlsx" @change="onFileChange" />
              <p class="upload-title">{{ form.file ? '已选择文件' : '拖拽或点击选择 .xlsx 文件' }}</p>
              <p class="upload-hint">仅支持 `.xlsx`，文件大小不超过 5MB</p>
              <div class="upload-actions">
                <button type="button" class="pick-btn" @click="triggerFilePick">
                  {{ form.file ? '重新选择' : '选择文件' }}
                </button>
                <button v-if="form.file" type="button" class="remove-btn" @click="removeFile">移除</button>
              </div>
              <p v-if="form.file" class="file-meta">
                {{ form.file.name }}（{{ fileSizeText(form.file.size) }}）
              </p>
            </div>
          </div>
          <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交' }}</button>
        </form>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success">{{ successMsg }}</p>
      </section>
    </section>
  </main>
</template>

<style scoped>
.form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  font-size: 13px;
  color: #334155;
}

.field input[type='text'] {
  height: 38px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  padding: 0 12px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input[type='text']:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.hidden-file-input {
  display: none;
}

.upload-card {
  border: 1px dashed #94a3b8;
  border-radius: 12px;
  background: #f8fafc;
  padding: 14px;
}

.upload-card.selected {
  border-color: #22c55e;
  background: #f0fdf4;
}

.upload-title {
  margin: 0;
  font-size: 14px;
  color: #0f172a;
}

.upload-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
}

.upload-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.pick-btn,
.remove-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.pick-btn {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.pick-btn:hover {
  background: #1d4ed8;
}

.remove-btn {
  background: #fff;
  color: #0f172a;
}

.file-meta {
  margin: 10px 0 0;
  color: #047857;
  font-size: 13px;
}
</style>
