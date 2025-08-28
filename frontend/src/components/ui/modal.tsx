import * as React from "react"
import * as ReactDOM from "react-dom"
import { X } from "lucide-react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { Button } from "./button"

// Hook para gerenciar portal
const usePortal = () => {
  const [portalContainer, setPortalContainer] = React.useState<HTMLElement | null>(null)
  
  React.useEffect(() => {
    const container = document.createElement('div')
    container.id = 'modal-portal'
    document.body.appendChild(container)
    setPortalContainer(container)
    
    return () => {
      if (document.body.contains(container)) {
        document.body.removeChild(container)
      }
    }
  }, [])
  
  return portalContainer
}

// Hook para lock do scroll do body
const useBodyScrollLock = (lock: boolean) => {
  React.useEffect(() => {
    if (lock) {
      const originalStyle = window.getComputedStyle(document.body).overflow
      document.body.style.overflow = 'hidden'
      document.body.style.paddingRight = 'var(--scrollbar-width, 0px)'
      
      return () => {
        document.body.style.overflow = originalStyle
        document.body.style.paddingRight = ''
      }
    }
  }, [lock])
}

// Hook para detectar largura do scrollbar
const useScrollbarWidth = () => {
  React.useEffect(() => {
    const scrollDiv = document.createElement('div')
    scrollDiv.style.width = '100px'
    scrollDiv.style.height = '100px'
    scrollDiv.style.overflow = 'scroll'
    scrollDiv.style.position = 'absolute'
    scrollDiv.style.top = '-9999px'
    
    document.body.appendChild(scrollDiv)
    const scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth
    document.documentElement.style.setProperty('--scrollbar-width', `${scrollbarWidth}px`)
    document.body.removeChild(scrollDiv)
  }, [])
}

// Variantes do modal
const modalVariants = cva(
  "fixed inset-0 z-50 flex items-center justify-center p-4",
  {
    variants: {
      size: {
        sm: "max-w-sm",
        md: "max-w-md", 
        lg: "max-w-lg",
        xl: "max-w-xl",
        "2xl": "max-w-2xl",
        "3xl": "max-w-3xl",
        "4xl": "max-w-4xl",
        full: "max-w-full h-full p-0",
      },
    },
    defaultVariants: {
      size: "md",
    },
  }
)

const modalContentVariants = cva(
  "relative bg-background rounded-lg shadow-lg border max-h-full flex flex-col",
  {
    variants: {
      size: {
        sm: "w-full",
        md: "w-full",
        lg: "w-full", 
        xl: "w-full",
        "2xl": "w-full",
        "3xl": "w-full",
        "4xl": "w-full",
        full: "w-full h-full rounded-none",
      },
    },
    defaultVariants: {
      size: "md",
    },
  }
)

// Types
export interface ModalProps extends VariantProps<typeof modalVariants> {
  open: boolean
  onOpenChange: (open: boolean) => void
  children: React.ReactNode
  closeOnClickOutside?: boolean
  closeOnEscape?: boolean
  preventClose?: boolean
  className?: string
  overlayClassName?: string
}

export interface ModalContentProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: VariantProps<typeof modalVariants>["size"]
}

export interface ModalHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  showCloseButton?: boolean
  onClose?: () => void
}

export interface ModalFooterProps extends React.HTMLAttributes<HTMLDivElement> {
  justify?: "start" | "center" | "end" | "between"
}

// Context para comunicação entre componentes
interface ModalContextValue {
  onClose: () => void
  size: VariantProps<typeof modalVariants>["size"]
}

const ModalContext = React.createContext<ModalContextValue | null>(null)

const useModalContext = () => {
  const context = React.useContext(ModalContext)
  if (!context) {
    throw new Error("Modal components must be used within a Modal")
  }
  return context
}

// Hook para focus management
const useFocusManagement = (isOpen: boolean, modalRef: React.RefObject<HTMLDivElement>) => {
  const previousActiveElement = React.useRef<HTMLElement | null>(null)
  
  React.useEffect(() => {
    if (isOpen) {
      previousActiveElement.current = document.activeElement as HTMLElement
      
      // Focus primeiro elemento focável no modal
      const focusableElements = modalRef.current?.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      )
      
      if (focusableElements && focusableElements.length > 0) {
        (focusableElements[0] as HTMLElement).focus()
      }
    } else {
      // Restaura focus anterior
      if (previousActiveElement.current) {
        previousActiveElement.current.focus()
      }
    }
  }, [isOpen, modalRef])
  
  // Trap focus dentro do modal
  React.useEffect(() => {
    if (!isOpen) return
    
    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab' || !modalRef.current) return
      
      const focusableElements = modalRef.current.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      )
      
      const firstElement = focusableElements[0] as HTMLElement
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement
      
      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault()
          lastElement.focus()
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault()
          firstElement.focus()
        }
      }
    }
    
    document.addEventListener('keydown', handleTabKey)
    return () => document.removeEventListener('keydown', handleTabKey)
  }, [isOpen, modalRef])
}

// Modal principal
const Modal: React.FC<ModalProps> = ({
  open,
  onOpenChange,
  children,
  closeOnClickOutside = true,
  closeOnEscape = true,
  preventClose = false,
  size,
  className,
  overlayClassName,
}) => {
  const portalContainer = usePortal()
  const modalRef = React.useRef<HTMLDivElement>(null)
  
  useScrollbarWidth()
  useBodyScrollLock(open)
  useFocusManagement(open, modalRef)
  
  const handleClose = React.useCallback(() => {
    if (!preventClose) {
      onOpenChange(false)
    }
  }, [onOpenChange, preventClose])
  
  // Escape key handler
  React.useEffect(() => {
    if (!open || !closeOnEscape) return
    
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        handleClose()
      }
    }
    
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [open, closeOnEscape, handleClose])
  
  const handleOverlayClick = React.useCallback((e: React.MouseEvent) => {
    if (closeOnClickOutside && e.target === e.currentTarget) {
      handleClose()
    }
  }, [closeOnClickOutside, handleClose])
  
  const contextValue: ModalContextValue = React.useMemo(() => ({
    onClose: handleClose,
    size,
  }), [handleClose, size])
  
  if (!open || !portalContainer) return null
  
  return ReactDOM.createPortal(
    <ModalContext.Provider value={contextValue}>
      <div
        className={cn(
          "fixed inset-0 z-50 bg-black/50 backdrop-blur-sm",
          overlayClassName
        )}
        onClick={handleOverlayClick}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div className={cn(modalVariants({ size }), className)}>
          <div 
            ref={modalRef}
            className={cn(modalContentVariants({ size }))}
            onClick={(e) => e.stopPropagation()}
          >
            {children}
          </div>
        </div>
      </div>
    </ModalContext.Provider>,
    portalContainer
  )
}

// Modal Content
const ModalContent: React.FC<ModalContentProps> = ({ 
  className, 
  children, 
  ...props 
}) => {
  return (
    <div className={cn("flex-1 overflow-hidden", className)} {...props}>
      {children}
    </div>
  )
}

// Modal Header
const ModalHeader: React.FC<ModalHeaderProps> = ({ 
  className,
  showCloseButton = true,
  onClose,
  children,
  ...props 
}) => {
  const { onClose: contextOnClose } = useModalContext()
  const handleClose = onClose || contextOnClose
  
  return (
    <div 
      className={cn(
        "flex items-center justify-between p-6 pb-0", 
        className
      )}
      {...props}
    >
      <div className="flex-1">
        {children}
      </div>
      {showCloseButton && (
        <Button
          variant="ghost"
          size="sm"
          onClick={handleClose}
          className="h-8 w-8 p-0 ml-4"
          aria-label="Fechar modal"
        >
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  )
}

// Modal Title
const ModalTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({ 
  className, 
  children,
  ...props 
}) => {
  return (
    <h2 
      id="modal-title"
      className={cn("text-lg font-semibold", className)} 
      {...props}
    >
      {children}
    </h2>
  )
}

// Modal Description
const ModalDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({ 
  className, 
  children,
  ...props 
}) => {
  return (
    <p 
      className={cn("text-sm text-muted-foreground mt-2", className)} 
      {...props}
    >
      {children}
    </p>
  )
}

// Modal Body
const ModalBody: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({ 
  className, 
  children,
  ...props 
}) => {
  return (
    <div 
      className={cn("flex-1 overflow-y-auto p-6", className)} 
      {...props}
    >
      {children}
    </div>
  )
}

// Modal Footer
const ModalFooter: React.FC<ModalFooterProps> = ({ 
  className, 
  justify = "end",
  children,
  ...props 
}) => {
  const justifyClasses = {
    start: "justify-start",
    center: "justify-center", 
    end: "justify-end",
    between: "justify-between"
  }
  
  return (
    <div 
      className={cn(
        "flex items-center gap-2 p-6 pt-0",
        justifyClasses[justify],
        className
      )} 
      {...props}
    >
      {children}
    </div>
  )
}

// Hook personalizado para controlar modais
export const useModal = () => {
  const [open, setOpen] = React.useState(false)
  
  const openModal = React.useCallback(() => setOpen(true), [])
  const closeModal = React.useCallback(() => setOpen(false), [])
  const toggleModal = React.useCallback(() => setOpen(prev => !prev), [])
  
  return {
    open,
    openModal,
    closeModal,
    toggleModal,
    setOpen,
  }
}

Modal.displayName = "Modal"
ModalContent.displayName = "ModalContent"
ModalHeader.displayName = "ModalHeader"
ModalTitle.displayName = "ModalTitle"
ModalDescription.displayName = "ModalDescription"
ModalBody.displayName = "ModalBody"
ModalFooter.displayName = "ModalFooter"

export {
  Modal,
  ModalContent,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalBody,
  ModalFooter,
}