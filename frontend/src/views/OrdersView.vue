<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const { token, logout } = useAuth()
const orders = ref([])
const error = ref('')
const mailNote = ref('')
const payingId = ref(null)
const receivingId = ref(null)
const cancellingId = ref(null)
const loading = ref(true)
let mailTimer = 0
const history = computed(() => route.name === 'order-history')
const closed = (order) => order.status === 'COMPLETED' || order.status === 'CANCELLED'
const visibleOrders = computed(() =>
  orders.value.filter((order) => (history.value ? closed(order) : !closed(order))),
)

const statusText = {
  UNPAID: '待付款',
  CREATED: '待付款',
  PAID: '已抵達',
  SHIPPED: '已抵達',
  DELIVERED: '已抵達',
  COMPLETED: '已收貨',
  CANCELLED: '已取消',
}

const shippingText = {
  UNPAID: '尚未出貨',
  CREATED: '尚未出貨',
  PAID: '已抵達',
  SHIPPED: '已抵達',
  DELIVERED: '已抵達',
  COMPLETED: '已收貨',
  CANCELLED: '已取消',
}

async function cancel(order) {
  error.value = ''
  cancellingId.value = order.id
  try {
    const updated = await api(`/api/orders/${order.id}/cancel`, {
      method: 'POST',
      token: token.value,
    })
    const index = orders.value.findIndex((entry) => entry.id === order.id)
    if (index >= 0) orders.value[index] = updated
  } catch (e) {
    error.value = describeError(e)
  } finally {
    cancellingId.value = null
  }
}

async function receive(order) {
  error.value = ''
  receivingId.value = order.id
  try {
    const updated = await api(`/api/orders/${order.id}/receive`, {
      method: 'POST',
      token: token.value,
    })
    const index = orders.value.findIndex((entry) => entry.id === order.id)
    if (index >= 0) orders.value[index] = updated
  } catch (e) {
    error.value = describeError(e)
  } finally {
    receivingId.value = null
  }
}

async function pay(order) {
  error.value = ''
  mailNote.value = ''
  payingId.value = order.id
  try {
    const result = await api(`/api/orders/${order.id}/pay`, { method: 'POST', token: token.value })
    const index = orders.value.findIndex((entry) => entry.id === order.id)
    if (index >= 0) orders.value[index] = result.order
    if (!result.mailSent) {
      mailNote.value = '通知信尚未寄出。'
      window.clearTimeout(mailTimer)
      mailTimer = window.setTimeout(() => {
        mailNote.value = ''
      }, 2500)
    }
  } catch (e) {
    error.value = describeError(e)
  } finally {
    payingId.value = null
  }
}

onUnmounted(() => window.clearTimeout(mailTimer))

onMounted(async () => {
  try {
    orders.value = await api('/api/orders', { token: token.value })
  } catch (e) {
    if (e.message === 'Invalid or expired token' || e.message === 'Invalid authorization') {
      logout()
      router.push({ name: 'login', query: { redirect: '/orders' } })
      return
    }
    error.value = describeError(e)
  } finally {
    loading.value = false
  }
})

function formatPrice(price) {
  return `NT$ ${(price || 0).toLocaleString('zh-Hant')}`
}

function formatDate(value) {
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value
    return new Date(year, month - 1, day, hour, minute).toLocaleString('zh-Hant')
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('zh-Hant')
}
</script>

<template>
  <main class="page">
    <div class="page-head">
      <h1>{{ history ? '歷史訂單' : '訂單' }}</h1>
      <RouterLink :to="history ? '/orders' : '/orders/history'">
        {{ history ? '回到訂單' : '歷史訂單' }}
      </RouterLink>
    </div>
    <p v-if="loading" class="lead">載入中…</p>
    <p v-if="mailNote" class="note">{{ mailNote }}</p>
    <p v-if="error && !loading" class="error">{{ error }}</p>
    <p v-if="!loading && !error && visibleOrders.length === 0" class="lead">
      {{ history ? '尚無歷史訂單。' : '尚無進行中的訂單。' }}
    </p>

    <ul v-if="!loading && visibleOrders.length" class="orders">
      <li v-for="order in visibleOrders" :key="order.id">
        <div class="heading">
          <h2>訂單編號 {{ order.id }}</h2>
          <span>{{ statusText[order.status] || order.status }}</span>
        </div>
        <p class="shipping">貨態　{{ shippingText[order.status] || '尚未出貨' }}</p>
        <p class="when">{{ formatDate(order.createdAt) }}</p>
        <p v-if="order.shippingAddress" class="ship">
          {{ order.recipientName }}　{{ order.recipientPhone }}<br />
          {{ order.shippingAddress }}
        </p>
        <ul>
          <li v-for="item in order.orderItems" :key="item.id">
            {{ item.title }} × {{ item.quantity }}
          </li>
        </ul>
        <p class="total">{{ formatPrice(order.totalPrice) }}</p>
        <div v-if="order.status === 'UNPAID' || order.status === 'CREATED'" class="actions">
          <button type="button" class="pay" :disabled="payingId === order.id" @click="pay(order)">
            {{ payingId === order.id ? '付款中…' : '付款' }}
          </button>
          <button type="button" class="cancel" :disabled="cancellingId === order.id" @click="cancel(order)">
            {{ cancellingId === order.id ? '取消中…' : '取消訂單' }}
          </button>
        </div>
        <button
          v-if="!history && order.status === 'DELIVERED'"
          type="button"
          class="pay"
          :disabled="receivingId === order.id"
          @click="receive(order)"
        >
          {{ receivingId === order.id ? '確認中…' : '確認已收貨' }}
        </button>
      </li>
    </ul>
  </main>
</template>

<style scoped>
.page {
  max-width: 40rem;
  padding-top: 2rem;
}

h1,
h2 {
  font-family: var(--font-serif);
}

.page-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
}

.page-head a {
  color: var(--accent);
  font-weight: 700;
}

h1 {
  font-size: 2.2rem;
}

.lead,
.when,
.shipping,
.orders span {
  color: var(--muted);
}

.note {
  margin-top: 0.75rem;
  color: #1f6a62;
}

.lead {
  margin-top: 0.5rem;
}

.error {
  margin-top: 0.5rem;
  color: var(--accent);
}

.orders {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  margin-top: 1.5rem;
  padding: 0;
  list-style: none;
}

.orders > li {
  padding: 1rem 1.1rem;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--bg-elevated);
}

.heading {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

h2 {
  font-size: 1.2rem;
}

.when,
.ship {
  margin-top: 0.2rem;
  font-size: 0.9rem;
}

.ship {
  color: var(--muted);
  line-height: 1.6;
}

.orders ul {
  margin: 0.75rem 0 0;
  padding-left: 1.1rem;
}

.total {
  margin-top: 0.75rem;
  font-weight: 700;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.85rem;
}

.actions .pay,
.actions .cancel {
  margin-top: 0;
}

.pay,
.cancel {
  border-radius: 999px;
  padding: 0.55rem 1rem;
  cursor: pointer;
  font-weight: 700;
}

.pay {
  margin-top: 0.85rem;
  border: 0;
  background: var(--accent);
  color: #fffaf6;
}

.cancel {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--ink);
}

.pay:disabled,
.cancel:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
