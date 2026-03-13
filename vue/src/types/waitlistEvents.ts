export const WaitlistEvent = {
  NEW_ENTRY: 'new-entry',
  DELETED_ENTRY: 'deleted-entry',
  UPDATED_ENTRY: 'updated-entry',
  NOTIFIED_ENTRY: 'notified-entry',
  WAITLIST_DISABLED: 'waitlist-disabled'
} as const

export type WaitlistEventName = typeof WaitlistEvent[keyof typeof WaitlistEvent]
