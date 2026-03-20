import { WaitlistEvent, type WaitlistEventName } from '@/types/waitlistEvents'

export type SseHandlers = {
  onNewEntry?: (payload: any) => void
  onDeletedEntry?: (payload: any) => void
  onUpdatedEntry?: (payload: any) => void
  onNotifiedEntry?: (payload: any) => void
  onWaitlistDisabled?: (payload: any) => void
  onError?: (error: any) => void
}

export function useWaitlistSse() {
  let source: EventSource | null = null

  function addSseListener(event: WaitlistEventName, handler: (e: MessageEvent) => void) {
    if (!source) return
    source.addEventListener(event, handler)
  }

  async function init(streamUrl: string, handlers: SseHandlers = {}) {
    stop()
    try {
      source = new EventSource(streamUrl)

      addSseListener(WaitlistEvent.NEW_ENTRY, (e: MessageEvent) => {
        handlers.onNewEntry?.(JSON.parse(e.data))
      })
      addSseListener(WaitlistEvent.DELETED_ENTRY, (e: MessageEvent) => {
        handlers.onDeletedEntry?.(JSON.parse(e.data))
      })
      addSseListener(WaitlistEvent.UPDATED_ENTRY, (e: MessageEvent) => {
        handlers.onUpdatedEntry?.(JSON.parse(e.data))
      })
      addSseListener(WaitlistEvent.NOTIFIED_ENTRY, (e: MessageEvent) => {
        handlers.onNotifiedEntry?.(JSON.parse(e.data))
      })
      addSseListener(WaitlistEvent.WAITLIST_DISABLED, (e: MessageEvent) => {
        handlers.onWaitlistDisabled?.(JSON.parse(e.data))
      })

    } catch (ex) {
      console.warn('could not open event stream', ex)
      handlers.onError?.(ex)
    }
  }

  function stop() {
    if (source) {
      source.close()
      source = null
    }
  }

  return { init, stop }
}
