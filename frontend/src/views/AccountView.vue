<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { token, email, role, setName, logout } = useAuth()

const name = ref('')
const phone = ref('')
const address = ref('')
const error = ref('')
const success = ref('')
const loading = ref(true)
const submitting = ref(false)

function signOut() {
  logout()
  router.push('/')
}

function phoneError(value) {
  const text = value.trim()
  if (!text) return ''
  const digits = text.replace(/\D/g, '')
  if (!/^[0-9+\-()\s]+$/.test(text) || digits.length < 8 || digits.length > 15) {
    return '請填寫有效的電話'
  }
  return ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const profile = await api('/api/users/me', { token: token.value })
    name.value = profile.name || ''
    phone.value = profile.phone || ''
    address.value = profile.address || ''
    setName(profile.name)
  } catch (e) {
    error.value = describeError(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  success.value = ''
  if (!name.value.trim()) {
    error.value = '請填寫姓名'
    return
  }
  if (name.value.trim().length > 40) {
    error.value = '姓名最多 40 個字'
    return
  }
  const invalidPhone = phoneError(phone.value)
  if (invalidPhone) {
    error.value = invalidPhone
    return
  }
  if (address.value.trim().length > 200) {
    error.value = '地址最多 200 個字'
    return
  }
  submitting.value = true
  try {
    const profile = await api('/api/users/me', {
      method: 'PUT',
      token: token.value,
      body: {
        name: name.value.trim(),
        phone: phone.value.trim(),
        address: address.value.trim(),
      },
    })
    name.value = profile.name || ''
    phone.value = profile.phone || ''
    address.value = profile.address || ''
    setName(profile.name)
    success.value = '資料已儲存'
  } catch (e) {
    error.value = describeError(e)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <h1>帳號</h1>
    <p class="email">{{ email }}</p>
    <p class="role">{{ role === 'ADMIN' ? '管理員' : '一般會員' }}</p>

    <p v-if="loading" class="state">正在讀取資料…</p>
    <form v-else class="profile" novalidate @submit.prevent="save">
      <h2>收件資料</h2>
      <p class="hint">結帳時可帶入以下資料。</p>
      <label>
        姓名
        <input v-model="name" type="text" autocomplete="name" maxlength="40" />
      </label>
      <label>
        電話
        <input v-model="phone" type="tel" autocomplete="tel" />
      </label>
      <label>
        地址
        <textarea v-model="address" rows="3" autocomplete="street-address" maxlength="200"></textarea>
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '儲存中…' : '儲存資料' }}</button>
    </form>

    <div class="actions">
      <RouterLink to="/orders">查看訂單</RouterLink>
      <button type="button" @click="signOut">登出</button>
    </div>
  </main>
</template>

<style scoped>
.page {
  max-width: 32rem;
  padding-top: 2rem;
}

h1,
h2 {
  font-family: var(--font-serif);
}

h1 {
  font-size: 2.2rem;
}

h2 {
  font-size: 1.35rem;
}

.email,
.role,
.state,
.hint {
  margin-top: 0.35rem;
  color: var(--muted);
}

.profile {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  margin-top: 1.75rem;
}

.profile .hint {
  margin-top: -0.35rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

input,
textarea {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 0.7rem 0.8rem;
  background: var(--bg-elevated);
  color: var(--ink);
  resize: vertical;
}

.profile button {
  align-self: flex-start;
  margin-top: 0.2rem;
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1.1rem;
  background: var(--accent);
  color: #fffaf6;
  cursor: pointer;
  font-weight: 700;
}

.profile button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.error {
  color: var(--accent);
}

.success {
  color: #1f6a62;
}

.actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.75rem;
}

.actions a,
.actions button {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.55rem 0.95rem;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
<<<<<<< HEAD
  font-weight: 700;
=======
  font-weight: 600;
  transition: all var(--transition-base);
}

.actions a:hover {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(212, 101, 74, 0.3);
}

.actions button {
  background: transparent;
>>>>>>> a4bc78dffc644df7f3fba307cf2b5f510a938bac
}

.actions button:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 101, 74, 0.05);
}
</style>
