import { computed, ref, watch } from 'vue'

function readCart() {
  try {
    const saved = JSON.parse(localStorage.getItem('cart') || '[]')
    return Array.isArray(saved) ? saved : []
  } catch {
    return []
  }
}

const items = ref(readCart())

watch(
  items,
  (value) => {
    localStorage.setItem('cart', JSON.stringify(value))
  },
  { deep: true },
)

export function useCart() {
  const count = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const total = computed(() =>
    items.value.reduce((sum, item) => sum + (item.price || 0) * item.quantity, 0),
  )

  function addBook(book) {
    const existing = items.value.find((item) => item.bookId === book.id)
    if (book.stock != null && (existing?.quantity || 0) >= book.stock) {
      return false
    }
    if (existing) {
      existing.quantity += 1
      return true
    }
    items.value.push({
      bookId: book.id,
      title: book.title,
      price: book.price,
      quantity: 1,
      stock: book.stock,
    })
    return true
  }

  function setQuantity(bookId, quantity) {
    const item = items.value.find((entry) => entry.bookId === bookId)
    if (!item) return
    if (quantity < 1) {
      remove(bookId)
      return
    }
    item.quantity = item.stock == null ? quantity : Math.min(quantity, item.stock)
  }

  function remove(bookId) {
    items.value = items.value.filter((item) => item.bookId !== bookId)
  }

  function clear() {
    items.value = []
  }

  return { items, count, total, addBook, setQuantity, remove, clear }
}
