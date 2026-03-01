import { h, render } from 'vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

export type ConfirmOptions = {
  // Either provide raw strings or i18n keys
  title?: string
  message?: string
  titleKey?: string
  messageKey?: string
  values?: Record<string, any>
  confirmLabel?: string
  cancelLabel?: string
  confirmLabelKey?: string
  cancelLabelKey?: string
}

export function confirm(opts: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    const container = document.createElement('div')
    document.body.appendChild(container)

    function cleanup() {
      render(null, container)
      if (container.parentNode) container.parentNode.removeChild(container)
    }

    const title = opts.title ?? (opts.titleKey ? opts.titleKey : '')
    const message = opts.message ?? (opts.messageKey ? opts.messageKey : '')
    const confirmLabel = opts.confirmLabel ?? (opts.confirmLabelKey ? opts.confirmLabelKey : 'confirm.confirm')
    const cancelLabel = opts.cancelLabel ?? (opts.cancelLabelKey ? opts.cancelLabelKey : 'confirm.cancel')

    const vnode = h(ConfirmDialog, {
      title,
      message,
      confirmLabel,
      cancelLabel,
      onConfirm: () => {
        resolve(true)
        cleanup()
      },
      onCancel: () => {
        resolve(false)
        cleanup()
      },
    })

    render(vnode, container)
  })
}
