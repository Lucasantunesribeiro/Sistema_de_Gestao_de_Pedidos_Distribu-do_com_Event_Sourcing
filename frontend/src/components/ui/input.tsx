import * as React from "react"
import { cn } from "@/lib/utils"
import { Search, Eye, EyeOff, AlertCircle, CheckCircle2 } from "lucide-react"
import { cva, type VariantProps } from "class-variance-authority"

const inputVariants = cva(
  "flex h-10 w-full rounded-lg border bg-background px-3 py-2 text-sm transition-all duration-200 file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "border-input focus-visible:ring-ring",
        error: "border-red-500 focus-visible:ring-red-500 bg-red-50 dark:bg-red-950/10",
        success: "border-green-500 focus-visible:ring-green-500 bg-green-50 dark:bg-green-950/10",
        search: "pl-10 border-input focus-visible:ring-ring",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement>,
    VariantProps<typeof inputVariants> {
  error?: boolean
  success?: boolean
  errorMessage?: string
  successMessage?: string
  showPasswordToggle?: boolean
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ 
    className, 
    type, 
    variant,
    error = false,
    success = false,
    errorMessage,
    successMessage,
    showPasswordToggle = false,
    ...props 
  }, ref) => {
    const [showPassword, setShowPassword] = React.useState(false)
    const [inputType, setInputType] = React.useState(type)
    
    // Determine variant based on state
    const currentVariant = error ? 'error' : success ? 'success' : variant

    React.useEffect(() => {
      if (type === 'password' && showPasswordToggle) {
        setInputType(showPassword ? 'text' : 'password')
      } else {
        setInputType(type)
      }
    }, [type, showPassword, showPasswordToggle])

    const isSearchInput = type === 'search' || currentVariant === 'search'
    const isPasswordInput = type === 'password' && showPasswordToggle

    return (
      <div className="relative w-full">
        {isSearchInput && (
          <Search 
            className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground pointer-events-none" 
            aria-hidden="true"
          />
        )}
        
        <input
          type={inputType}
          className={cn(
            inputVariants({ variant: currentVariant, className }),
            isPasswordInput && "pr-10"
          )}
          ref={ref}
          aria-invalid={error}
          aria-describedby={
            error && errorMessage ? `${props.id}-error` : 
            success && successMessage ? `${props.id}-success` : undefined
          }
          {...props}
        />

        {isPasswordInput && (
          <button
            type="button"
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 rounded-sm"
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
            tabIndex={-1}
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </button>
        )}

        {error && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
            <AlertCircle className="h-4 w-4 text-red-500" aria-hidden="true" />
          </div>
        )}

        {success && !isPasswordInput && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
            <CheckCircle2 className="h-4 w-4 text-green-500" aria-hidden="true" />
          </div>
        )}

        {error && errorMessage && (
          <p 
            id={`${props.id}-error`}
            className="mt-1 text-xs text-red-600 dark:text-red-400"
            role="alert"
          >
            {errorMessage}
          </p>
        )}

        {success && successMessage && (
          <p 
            id={`${props.id}-success`}
            className="mt-1 text-xs text-green-600 dark:text-green-400"
          >
            {successMessage}
          </p>
        )}
      </div>
    )
  }
)
Input.displayName = "Input"

export { Input, inputVariants }