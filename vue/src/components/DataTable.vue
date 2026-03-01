<template>
  <div class="table-wrapper">
    <table :class="[tableClass || 'data-table', { 'row-hover': rowHover }]">
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" :class="col.sortable ? 'sortable' : ''" @click="onHeaderClick(col)">
            <span v-if="col.label">{{ col.label }}</span>
            <span class="sort-indicator" v-if="sortColumn === col.key && col.sortable">
              {{ sortDirection === 'asc' ? '▲' : '▼' }}
            </span>
          </th>
        </tr>
      </thead>
      <tbody>
        <template v-if="loading">
          <slot name="loading">
            <tr>
              <td :colspan="columns.length" class="loading">Loading...</td>
            </tr>
          </slot>
        </template>

        <template v-else-if="!rows || rows.length === 0">
          <slot name="empty">
            <tr>
              <td :colspan="columns.length" class="no-data">{{ noDataText }}</td>
            </tr>
          </slot>
        </template>

        <template v-else>
          <template v-for="(row, idx) in rows" :key="getRowKey(row, idx)">
            <slot name="row" :row="row" :index="idx">
              <tr>
                <td v-for="col in columns" :key="col.key">{{ row[col.key] }}</td>
              </tr>
            </slot>
          </template>
        </template>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  columns: { type: Array as () => Array<{ key: string; label?: string; sortable?: boolean }>, required: true },
  rows: { type: Array as () => any[], default: () => [] },
  loading: { type: Boolean, default: false },
  sortColumn: { type: String as () => string | null, default: null },
  sortDirection: { type: String as () => 'asc' | 'desc', default: 'asc' },
  noDataText: { type: String, default: 'No data found' },
  rowKey: { type: String, default: 'id' },
  tableClass: { type: String, default: '' },
  rowHover: { type: Boolean, default: true }
})

const emit = defineEmits(['toggle-sort'])

function onHeaderClick(col: { key: string; sortable?: boolean }) {
  if (col.sortable) emit('toggle-sort', col.key)
}

function getRowKey(row: any, idx: number) {
  return row?.[props.rowKey] ?? idx
}
</script>

<style scoped>
/* Base table styles (applies to tables rendered by DataTable) */
.table-wrapper {
  margin-top: 1rem;
  overflow-x: auto;
}

.data-table,
.users-table,
.codes-table,
.boards-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.07);
  font-size: 1rem;
}

.data-table thead,
.users-table thead,
.codes-table thead,
.boards-table thead {
  background-color: var(--theme-color, #42b983);
  color: white;
}

.data-table th,
.data-table td,
.users-table th,
.users-table td,
.codes-table th,
.codes-table td,
.boards-table th,
.boards-table td {
  padding: 12px 16px;
  text-align: left;
  vertical-align: middle;
}

.data-table th {
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Force left alignment for all table body cells and their contents (includes slotted content) */
:deep(.data-table td),
:deep(.users-table td),
:deep(.codes-table td),
:deep(.boards-table td) {
  text-align: left !important;
}

:deep(.data-table td *),
:deep(.users-table td *),
:deep(.codes-table td *),
:deep(.boards-table td *) {
  text-align: left !important;
}

/* Center table headers (titles) */
:deep(.data-table th),
:deep(.users-table th),
:deep(.codes-table th),
:deep(.boards-table th) {
  text-align: center !important;
}

.data-table th.sortable,
.users-table th.sortable,
.codes-table th.sortable,
.boards-table th.sortable {
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s;
}

.data-table th.sortable:hover,
.users-table th.sortable:hover,
.codes-table th.sortable:hover,
.boards-table th.sortable:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.data-table tbody tr {
  border-bottom: 1px solid #e6e6e6;
}

.data-table tbody tr:last-child {
  border-bottom: none;
}

.data-table tbody tr:hover {
  background-color: #f8f9fa;
}

.codes-table td:last-child,
.data-table td:last-child {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.sort-indicator {
  margin-left: 6px;
  font-size: 12px;
}

.loading,
.no-data {
  text-align: left;
  padding: 1.25rem;
  color: #888;
  font-style: italic;
}

/* Optional row hover highlight — enabled by setting rowHover prop (default true) */
.data-table.row-hover tbody tr {
  transition: background-color 0.12s ease;
}
.data-table.row-hover tbody tr:hover {
  background-color: rgba(66,153,225,0.06); /* subtle blue-tint */
}

/* Helpers used by registration codes rows (apply to slotted content) */
:deep(.code-cell-content) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

:deep(.code-badge) {
  font-family: monospace;
  font-weight: 600;
  font-size: 0.95rem;
  letter-spacing: 1px;
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  display: inline-block;
  white-space: nowrap;
}

:deep(.code-badge.active) {
  background: rgba(66, 185, 131, 0.2);
  color: #42b983;
}

:deep(.code-badge.inactive) {
  background: rgba(220, 38, 38, 0.2);
  color: #dc2626;
}

:deep(.code-cell) {
  font-family: monospace;
  font-weight: 600;
  font-size: 1.05rem;
  color: var(--theme-color, #42b983);
  letter-spacing: 1px;
}

:deep(.btn-copy-small) {
  padding: 0.25rem;
  background: transparent;
  border: 1px solid var(--theme-color, #42b983);
  border-radius: 4px;
  cursor: pointer;
  color: var(--theme-color, #42b983);
  display: flex;
  align-items: center;
  transition: all 0.2s;
  font-size: 0.9rem;
}

:deep(.btn-copy-small .material-symbols-outlined) {
  font-size: 16px;
}

:deep(.btn-copy-small:hover) {
  background: var(--theme-color, #42b983);
  color: white;
}

/* Small utilities used by pages (slotted) */
:deep(.code-value) {
  font-family: monospace;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--theme-color, #42b983);
  letter-spacing: 2px;
}

:deep(.btn-copy) {
  margin-left: auto;
  padding: 0.5rem;
  background: transparent;
  border: 1px solid var(--theme-color, #42b983);
  border-radius: 4px;
  cursor: pointer;
  color: var(--theme-color, #42b983);
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

:deep(.btn-copy:hover) {
  background: var(--theme-color, #42b983);
  color: white;
}

</style>
