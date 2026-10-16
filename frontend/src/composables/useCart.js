import { computed, ref, watch } from 'vue'
import { api } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const GUEST_KEY = 'cart'
const { token } = useAuth()

function readGuest() {
  try {
    const saved = JSON.parse(localStorage.getItem(GUEST_KEY) || '[]')
    return Array.isArray(saved) ? saved : []
  } catch {
    return []
  }
}

function writeGuest(value) {
  localStorage.setItem(GUEST_KEY, JSON.stringify(value))
}

function clearGuest() {
  localStorage.removeItem(GUEST_KEY)
}

const items = ref(token.value ? [] : readGuest())
let accountSync = Promise.resolve()

function track(work) {
  const run = accountSync.then(work, work)
  accountSync = run.then(
    () => {},
    () => {},
  )
  return run
}

async function adoptAccountCart() {
  const guest = readGuest()
  clearGuest()
  for (const line of guest) {
    const bookId = Number(line?.bookId)
    const quantity = Number(line?.quantity)
    if (!bookId || quantity < 1) continue
    try {
      await api('/api/cart', {
        method: 'POST',
        token: token.value,
        body: { bookId, quantity },
      })
    } catch {
      // 已下架或售完的項目略過，其餘仍併入帳號
    }
  }
  if (!token.value) return
  items.value = await api('/api/cart', { token: token.value })
}

watch(
  token,
  (next, prev) => {
    if (next) {
      accountSync = adoptAccountCart()
      return
    }
    if (prev) items.value = readGuest()
  },
  { immediate: true },
)

function addLocal(book) {
  const existing = items.value.find((item) => item.bookId === book.id)
  if (book.stock != null && (existing?.quantity || 0) >= book.stock) return false
  if (existing) {
    existing.quantity += 1
    existing.title = book.title
    existing.author = book.author
    existing.price = book.price
    existing.coverImageUrl = book.coverImageUrl
    existing.stock = book.stock
  } else {
    items.value.push({
      bookId: book.id,
      title: book.title,
      author: book.author,
      price: book.price,
      coverImageUrl: book.coverImageUrl,
      quantity: 1,
      stock: book.stock,
    })
  }
  writeGuest(items.value)
  return true
}

function setQuantityLocal(bookId, quantity) {
  const item = items.value.find((entry) => entry.bookId === bookId)
  if (!item) return
  if (quantity < 1) {
    removeLocal(bookId)
    return
  }
  item.quantity = item.stock == null ? quantity : Math.min(quantity, item.stock)
  writeGuest(items.value)
}

function removeLocal(bookId) {
  items.value = items.value.filter((item) => item.bookId !== bookId)
  writeGuest(items.value)
}

export function useCart() {
  const count = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const total = computed(() =>
    items.value.reduce((sum, item) => sum + (item.price || 0) * item.quantity, 0),
  )

  function refresh() {
    if (!token.value) return Promise.resolve()
    return track(async () => {
      items.value = await api('/api/cart', { token: token.value })
    })
  }

  function addBook(book) {
    if (!token.value) return Promise.resolve(addLocal(book))
    return track(async () => {
      const before = items.value.find((item) => item.bookId === book.id)?.quantity || 0
      if (book.stock != null && before >= book.stock) return false
      const next = await api('/api/cart', {
        method: 'POST',
        token: token.value,
        body: { bookId: book.id, quantity: 1 },
      })
      items.value = next
      const after = next.find((item) => item.bookId === book.id)?.quantity || 0
      return after > before
    })
  }

  function setQuantity(bookId, quantity) {
    if (!token.value) {
      setQuantityLocal(bookId, quantity)
      return Promise.resolve()
    }
    return track(async () => {
      if (quantity < 1) {
        items.value = await api(`/api/cart/${bookId}`, { method: 'DELETE', token: token.value })
        return
      }
      items.value = await api(`/api/cart/${bookId}`, {
        method: 'PUT',
        token: token.value,
        body: { quantity },
      })
    })
  }

  function remove(bookId) {
    if (!token.value) {
      removeLocal(bookId)
      return Promise.resolve()
    }
    return track(async () => {
      items.value = await api(`/api/cart/${bookId}`, { method: 'DELETE', token: token.value })
    })
  }

  function clear() {
    if (!token.value) {
      items.value = []
      writeGuest([])
      return Promise.resolve()
    }
    return track(async () => {
      items.value = await api('/api/cart', { method: 'DELETE', token: token.value })
    })
  }

  return { items, count, total, refresh, addBook, setQuantity, remove, clear }
}
