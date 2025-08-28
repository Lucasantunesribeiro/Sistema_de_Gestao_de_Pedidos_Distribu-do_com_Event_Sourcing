import * as React from "react"
import { ChevronDown, ChevronUp, ChevronsUpDown, MoreHorizontal, Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "./button"
import { 
  Table,
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from "./table"

// Types
export type SortDirection = "asc" | "desc" | null

export interface TableColumn<T = unknown> {
  key: string
  title: string
  accessor: keyof T | ((item: T) => React.ReactNode)
  sortable?: boolean
  width?: string
  className?: string
  headerClassName?: string
  cellClassName?: string
}

export interface TableAction<T = unknown> {
  label: string
  icon?: React.ComponentType<{ className?: string }>
  onClick: (item: T) => void
  variant?: "default" | "danger"
  disabled?: (item: T) => boolean
}

export interface TablePagination {
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
}

export interface DataTableProps<T = unknown> {
  data: T[]
  columns: TableColumn<T>[]
  pagination?: TablePagination
  actions?: TableAction<T>[]
  selectable?: boolean
  selectedRows?: T[]
  onSelectionChange?: (selectedRows: T[]) => void
  sortBy?: string
  sortDirection?: SortDirection
  onSort?: (column: string, direction: SortDirection) => void
  loading?: boolean
  emptyMessage?: string
  errorMessage?: string
  className?: string
  rowKey?: keyof T | ((item: T) => string | number)
}

const DataTable = <T extends Record<string, any>>({
  data,
  columns,
  pagination,
  actions,
  selectable = false,
  selectedRows = [],
  onSelectionChange,
  sortBy,
  sortDirection,
  onSort,
  loading = false,
  emptyMessage = "Nenhum registro encontrado",
  errorMessage,
  className,
  rowKey = "id" as keyof T,
}: DataTableProps<T>) => {
  const [internalSort, setInternalSort] = React.useState<{
    column: string | null
    direction: SortDirection
  }>({ column: null, direction: null })

  // Use internal or controlled sorting
  const currentSortColumn = sortBy ?? internalSort.column
  const currentSortDirection = sortDirection ?? internalSort.direction

  const handleSort = React.useCallback((column: string) => {
    const col = columns.find(c => c.key === column)
    if (!col?.sortable) return

    let newDirection: SortDirection = "asc"
    if (currentSortColumn === column) {
      if (currentSortDirection === "asc") {
        newDirection = "desc"
      } else if (currentSortDirection === "desc") {
        newDirection = null
      }
    }

    if (onSort) {
      onSort(column, newDirection)
    } else {
      setInternalSort({ column: newDirection ? column : null, direction: newDirection })
    }
  }, [currentSortColumn, currentSortDirection, columns, onSort])

  const handleRowSelect = React.useCallback((item: T) => {
    if (!onSelectionChange) return

    const itemKey = typeof rowKey === "function" ? rowKey(item) : item[rowKey]
    const isSelected = selectedRows.some(row => {
      const selectedKey = typeof rowKey === "function" ? rowKey(row) : row[rowKey]
      return selectedKey === itemKey
    })

    if (isSelected) {
      const newSelection = selectedRows.filter(row => {
        const selectedKey = typeof rowKey === "function" ? rowKey(row) : row[rowKey]
        return selectedKey !== itemKey
      })
      onSelectionChange(newSelection)
    } else {
      onSelectionChange([...selectedRows, item])
    }
  }, [selectedRows, onSelectionChange, rowKey])

  const handleSelectAll = React.useCallback(() => {
    if (!onSelectionChange) return
    
    const allSelected = selectedRows.length === data.length && data.length > 0
    onSelectionChange(allSelected ? [] : [...data])
  }, [data, selectedRows, onSelectionChange])

  const isRowSelected = React.useCallback((item: T) => {
    const itemKey = typeof rowKey === "function" ? rowKey(item) : item[rowKey]
    return selectedRows.some(row => {
      const selectedKey = typeof rowKey === "function" ? rowKey(row) : row[rowKey]
      return selectedKey === itemKey
    })
  }, [selectedRows, rowKey])

  const getSortIcon = React.useCallback((column: string) => {
    if (!columns.find(c => c.key === column)?.sortable) return null
    
    if (currentSortColumn !== column) {
      return <ChevronsUpDown className="h-4 w-4 text-muted-foreground/50" />
    }
    
    if (currentSortDirection === "asc") {
      return <ChevronUp className="h-4 w-4 text-foreground" />
    }
    
    if (currentSortDirection === "desc") {
      return <ChevronDown className="h-4 w-4 text-foreground" />
    }
    
    return <ChevronsUpDown className="h-4 w-4 text-muted-foreground/50" />
  }, [currentSortColumn, currentSortDirection, columns])

  const getCellValue = React.useCallback((item: T, column: TableColumn<T>) => {
    if (typeof column.accessor === "function") {
      return column.accessor(item)
    }
    return item[column.accessor]
  }, [])

  // Loading skeleton
  if (loading) {
    return (
      <div className={cn("w-full", className)}>
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                {selectable && <TableHead className="w-12" />}
                {columns.map((column) => (
                  <TableHead 
                    key={column.key}
                    style={{ width: column.width }}
                    className={column.headerClassName}
                  >
                    <div className="h-4 bg-muted animate-pulse rounded" />
                  </TableHead>
                ))}
                {actions && actions.length > 0 && <TableHead className="w-12" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {Array.from({ length: 5 }).map((_, index) => (
                <TableRow key={index}>
                  {selectable && (
                    <TableCell>
                      <div className="h-4 w-4 bg-muted animate-pulse rounded" />
                    </TableCell>
                  )}
                  {columns.map((column) => (
                    <TableCell key={column.key} className={column.cellClassName}>
                      <div className="h-4 bg-muted animate-pulse rounded" />
                    </TableCell>
                  ))}
                  {actions && actions.length > 0 && (
                    <TableCell>
                      <div className="h-4 w-4 bg-muted animate-pulse rounded" />
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    )
  }

  // Error state
  if (errorMessage) {
    return (
      <div className={cn("w-full", className)}>
        <div className="rounded-lg border p-8 text-center">
          <div className="text-destructive text-sm font-medium mb-2">Erro</div>
          <div className="text-muted-foreground text-sm">{errorMessage}</div>
        </div>
      </div>
    )
  }

  // Empty state
  if (data.length === 0) {
    return (
      <div className={cn("w-full", className)}>
        <div className="rounded-lg border p-8 text-center">
          <div className="text-muted-foreground text-sm">{emptyMessage}</div>
        </div>
      </div>
    )
  }

  const allSelected = data.length > 0 && selectedRows.length === data.length
  const someSelected = selectedRows.length > 0 && selectedRows.length < data.length

  return (
    <div className={cn("w-full space-y-4", className)}>
      <div className="rounded-lg border overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                {selectable && (
                  <TableHead className="w-12">
                    <div className="flex items-center">
                      <input
                        type="checkbox"
                        checked={allSelected}
                        ref={(el) => {
                          if (el) el.indeterminate = someSelected
                        }}
                        onChange={handleSelectAll}
                        className="h-4 w-4 rounded border-input focus:ring-2 focus:ring-primary"
                        aria-label="Selecionar todos"
                      />
                    </div>
                  </TableHead>
                )}
                {columns.map((column) => (
                  <TableHead 
                    key={column.key}
                    style={{ width: column.width }}
                    className={cn(
                      column.headerClassName,
                      column.sortable && "cursor-pointer select-none hover:bg-muted/80 transition-colors"
                    )}
                    onClick={column.sortable ? () => handleSort(column.key) : undefined}
                  >
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{column.title}</span>
                      {getSortIcon(column.key)}
                    </div>
                  </TableHead>
                ))}
                {actions && actions.length > 0 && (
                  <TableHead className="w-12">
                    <span className="sr-only">Ações</span>
                  </TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((item, index) => {
                const key = typeof rowKey === "function" ? rowKey(item) : item[rowKey]
                const selected = isRowSelected(item)
                
                return (
                  <TableRow 
                    key={key || index}
                    data-state={selected ? "selected" : undefined}
                    className={cn(
                      "transition-colors hover:bg-muted/50",
                      selected && "bg-muted/50"
                    )}
                  >
                    {selectable && (
                      <TableCell>
                        <div className="flex items-center">
                          <input
                            type="checkbox"
                            checked={selected}
                            onChange={() => handleRowSelect(item)}
                            className="h-4 w-4 rounded border-input focus:ring-2 focus:ring-primary"
                            aria-label={`Selecionar linha ${index + 1}`}
                          />
                        </div>
                      </TableCell>
                    )}
                    {columns.map((column) => (
                      <TableCell 
                        key={column.key} 
                        className={column.cellClassName}
                      >
                        {getCellValue(item, column)}
                      </TableCell>
                    ))}
                    {actions && actions.length > 0 && (
                      <TableCell>
                        <div className="flex items-center gap-1">
                          {actions.map((action, actionIndex) => {
                            const Icon = action.icon
                            const disabled = action.disabled?.(item) || false
                            
                            return (
                              <Button
                                key={actionIndex}
                                variant={action.variant === "danger" ? "danger" : "ghost"}
                                size="sm"
                                disabled={disabled}
                                onClick={() => action.onClick(item)}
                                className="h-8 w-8 p-0"
                                aria-label={action.label}
                              >
                                {Icon ? (
                                  <Icon className="h-4 w-4" />
                                ) : (
                                  <MoreHorizontal className="h-4 w-4" />
                                )}
                              </Button>
                            )
                          })}
                        </div>
                      </TableCell>
                    )}
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
      </div>
      
      {pagination && (
        <TablePagination
          page={pagination.page}
          pageSize={pagination.pageSize}
          total={pagination.total}
          onPageChange={pagination.onPageChange}
          onPageSizeChange={pagination.onPageSizeChange}
        />
      )}
    </div>
  )
}

// Pagination component
interface TablePaginationProps {
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
}

const TablePagination: React.FC<TablePaginationProps> = ({
  page,
  pageSize,
  total,
  onPageChange,
  onPageSizeChange,
}) => {
  const totalPages = Math.ceil(total / pageSize)
  const startItem = (page - 1) * pageSize + 1
  const endItem = Math.min(page * pageSize, total)
  
  const pageSizeOptions = [10, 25, 50, 100]

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm">
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground">Linhas por página:</span>
        <select
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
          className="h-8 w-16 rounded border border-input bg-background px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        >
          {pageSizeOptions.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>
      </div>
      
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground">
          {total > 0 ? `${startItem}-${endItem} de ${total}` : "0 registros"}
        </span>
      </div>
      
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(1)}
          disabled={page <= 1}
          className="h-8 px-2"
        >
          Primeira
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 1}
          className="h-8 px-2"
        >
          Anterior
        </Button>
        
        <div className="flex items-center gap-1">
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            let pageNumber: number
            if (totalPages <= 5) {
              pageNumber = i + 1
            } else if (page <= 3) {
              pageNumber = i + 1
            } else if (page >= totalPages - 2) {
              pageNumber = totalPages - 4 + i
            } else {
              pageNumber = page - 2 + i
            }

            return (
              <Button
                key={pageNumber}
                variant={page === pageNumber ? "primary" : "outline"}
                size="sm"
                onClick={() => onPageChange(pageNumber)}
                className="h-8 w-8 p-0"
              >
                {pageNumber}
              </Button>
            )
          })}
        </div>
        
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages}
          className="h-8 px-2"
        >
          Próxima
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(totalPages)}
          disabled={page >= totalPages}
          className="h-8 px-2"
        >
          Última
        </Button>
      </div>
    </div>
  )
}

DataTable.displayName = "DataTable"

export { DataTable, TablePagination }