import { useToast, type ToastOptions } from 'vue-toastification'

export function success(message: string, options?: ToastOptions) {
  useToast().success(message, options)
}

export function error(message: string, options?: ToastOptions) {
  useToast().error(message, options)
}

export function info(message: string, options?: ToastOptions) {
  useToast().info(message, options)
}

export function warn(message: string, options?: ToastOptions) {
  useToast().warning(message, options)
}

const notify = { success, error, info, warn }
export default notify
