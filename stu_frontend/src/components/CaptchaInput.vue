<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  /** 验证码接口，默认与 Vite 代理 `/api` 一致 */
  apiUrl: { type: String, default: '/api/captcha' },
  /** 用户输入的验证码 */
  modelValue: { type: String, default: '' },
  /** 当前 captchaId，由接口返回后同步给父组件 */
  captchaId: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'update:captchaId', 'load', 'error'])

const imageSrc = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function fetchCaptcha() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await fetch(props.apiUrl)
    if (!res.ok) {
      const t = await res.text()
      throw new Error(t || `HTTP ${res.status}`)
    }
    const data = await res.json()
    if (!data?.captchaId || !data?.imageBase64) {
      throw new Error('接口返回格式不正确')
    }
    emit('update:captchaId', data.captchaId)
    imageSrc.value = `data:image/png;base64,${data.imageBase64}`
    emit('update:modelValue', '')
    emit('load', data)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '加载验证码失败'
    errorMsg.value = msg
    emit('error', e)
  } finally {
    loading.value = false
  }
}

function onInput(e) {
  emit('update:modelValue', e.target.value)
}

onMounted(fetchCaptcha)

defineExpose({ refresh: fetchCaptcha })
</script>

<template>
  <div class="captcha-input">
    <div class="captcha-row">
      <div
        class="captcha-img-wrap"
        role="button"
        tabindex="0"
        title="点击刷新验证码"
        @click="fetchCaptcha"
        @keydown.enter.prevent="fetchCaptcha"
      >
        <img v-if="imageSrc && !loading" :src="imageSrc" alt="验证码" class="captcha-img" />
        <span v-else-if="loading" class="captcha-placeholder">加载中…</span>
        <span v-else class="captcha-placeholder">暂无图片</span>
      </div>
      <button type="button" class="captcha-refresh" :disabled="loading" @click="fetchCaptcha">
        刷新
      </button>
    </div>
    <input
      class="captcha-field"
      type="text"
      autocomplete="off"
      autocapitalize="characters"
      placeholder="请输入验证码"
      :value="modelValue"
      :disabled="loading"
      @input="onInput"
    />
    <p v-if="errorMsg" class="captcha-error">{{ errorMsg }}</p>
  </div>
</template>

<style scoped>
.captcha-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 280px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.captcha-img-wrap {
  flex: 1;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #c9d1d9;
  border-radius: 6px;
  background: #f6f8fa;
  cursor: pointer;
  overflow: hidden;
}

.captcha-img-wrap:focus-visible {
  outline: 2px solid #0969da;
  outline-offset: 2px;
}

.captcha-img {
  display: block;
  max-width: 100%;
  height: auto;
  vertical-align: middle;
}

.captcha-placeholder {
  font-size: 0.875rem;
  color: #656d76;
  padding: 0.5rem;
}

.captcha-refresh {
  flex-shrink: 0;
  padding: 0.4rem 0.75rem;
  font-size: 0.875rem;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.captcha-refresh:hover:not(:disabled) {
  background: #f6f8fa;
}

.captcha-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.captcha-field {
  width: 100%;
  box-sizing: border-box;
  padding: 0.5rem 0.65rem;
  font-size: 1rem;
  border: 1px solid #d0d7de;
  border-radius: 6px;
}

.captcha-field:focus {
  outline: none;
  border-color: #0969da;
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.15);
}

.captcha-error {
  margin: 0;
  font-size: 0.8125rem;
  color: #cf222e;
}
</style>
