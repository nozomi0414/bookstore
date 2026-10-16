import { ref } from 'vue'

const message = ref('')
let timer = 0

export function useToast() {
  function show(text) {
    message.value = text
    window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      message.value = ''
    }, 1800)
  }

  return { message, show }
}
