/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./public/index.html"
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: {
          50: "#f0f9ff",
          100: "#e0f2fe", 
          200: "#bae6fd",
          300: "#7dd3fc",
          400: "#38bdf8",
          500: "#0891b2",
          600: "#0e7490",
          700: "#164e63",
          800: "#155e75",
          900: "#0f4c75"
        },
        secondary: {
          50: "#fef7f0",
          100: "#feede0",
          200: "#fcd9c0",
          300: "#fabf96",
          400: "#f79f6a",
          500: "#ff6b5a",
          600: "#e85a4a",
          700: "#d14539",
          800: "#b83429",
          900: "#9f2419"
        },
        neutral: {
          50: "#fafafa",
          100: "#f5f5f5",
          200: "#e5e5e5",
          300: "#d4d4d4",
          400: "#a3a3a3",
          500: "#737373",
          600: "#525252",
          700: "#404040",
          800: "#262626",
          900: "#171717"
        },
        success: {
          50: "#ecfdf5",
          100: "#d1fae5",
          200: "#a7f3d0",
          300: "#6ee7b7",
          400: "#34d399",
          500: "#10b981",
          600: "#059669",
          700: "#047857",
          800: "#065f46",
          900: "#064e3b"
        },
        warning: {
          50: "#fffbeb", 
          100: "#fef3c7",
          200: "#fde68a",
          300: "#fcd34d",
          400: "#fbbf24",
          500: "#f59e0b",
          600: "#d97706",
          700: "#b45309",
          800: "#92400e",
          900: "#78350f"
        },
        error: {
          50: "#fef2f2",
          100: "#fee2e2",
          200: "#fecaca",
          300: "#fca5a5",
          400: "#f87171",
          500: "#ef4444", 
          600: "#dc2626",
          700: "#b91c1c",
          800: "#991b1b",
          900: "#7f1d1d"
        },
        info: {
          50: "#eff6ff",
          100: "#dbeafe",
          200: "#bfdbfe",
          300: "#93c5fd",
          400: "#60a5fa",
          500: "#3b82f6",
          600: "#2563eb", 
          700: "#1d4ed8",
          800: "#1e40af",
          900: "#1e3a8a"
        }
      },
      fontFamily: {
        sans: ["Inter", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "Helvetica Neue", "Arial", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "Consolas", "Monaco", "Courier New", "monospace"]
      },
      fontSize: {
        xs: ["0.75rem", { lineHeight: "1rem" }],
        sm: ["0.875rem", { lineHeight: "1.25rem" }],
        base: ["1rem", { lineHeight: "1.5rem" }],
        lg: ["1.125rem", { lineHeight: "1.75rem" }],
        xl: ["1.25rem", { lineHeight: "1.75rem" }],
        "2xl": ["1.5rem", { lineHeight: "2rem" }],
        "3xl": ["1.875rem", { lineHeight: "2.25rem" }],
        "4xl": ["2.25rem", { lineHeight: "2.5rem" }],
        "5xl": ["3rem", { lineHeight: "1" }],
        "6xl": ["3.75rem", { lineHeight: "1" }]
      },
      spacing: {
        "0": "0",
        "px": "1px",
        "0.5": "0.125rem",
        "1": "0.25rem",
        "1.5": "0.375rem", 
        "2": "0.5rem",
        "2.5": "0.625rem",
        "3": "0.75rem",
        "3.5": "0.875rem",
        "4": "1rem",
        "5": "1.25rem",
        "6": "1.5rem",
        "7": "1.75rem",
        "8": "2rem",
        "9": "2.25rem",
        "10": "2.5rem",
        "11": "2.75rem",
        "12": "3rem",
        "14": "3.5rem",
        "16": "4rem",
        "18": "4.5rem",
        "20": "5rem",
        "24": "6rem",
        "28": "7rem",
        "32": "8rem",
        "36": "9rem",
        "40": "10rem",
        "44": "11rem",
        "48": "12rem",
        "52": "13rem",
        "56": "14rem",
        "60": "15rem",
        "64": "16rem",
        "72": "18rem",
        "80": "20rem",
        "96": "24rem"
      },
      borderRadius: {
        none: "0",
        sm: "0.375rem",
        DEFAULT: "0.5rem", 
        md: "0.5rem",
        lg: "0.75rem",
        xl: "1rem",
        "2xl": "1.5rem",
        "3xl": "2rem",
        full: "9999px"
      },
      boxShadow: {
        sm: "0 1px 2px 0 rgba(15, 76, 117, 0.05)",
        DEFAULT: "0 1px 3px 0 rgba(15, 76, 117, 0.1), 0 1px 2px -1px rgba(15, 76, 117, 0.1)",
        md: "0 4px 6px -1px rgba(15, 76, 117, 0.1), 0 2px 4px -2px rgba(15, 76, 117, 0.1)",
        lg: "0 10px 15px -3px rgba(15, 76, 117, 0.1), 0 4px 6px -4px rgba(15, 76, 117, 0.1)",
        xl: "0 20px 25px -5px rgba(15, 76, 117, 0.1), 0 8px 10px -6px rgba(15, 76, 117, 0.1)",
        "2xl": "0 25px 50px -12px rgba(15, 76, 117, 0.25)",
        inner: "inset 0 2px 4px 0 rgba(15, 76, 117, 0.05)"
      },
      animation: {
        "fade-in": "fadeIn 0.2s cubic-bezier(0.2, 0.9, 0.3, 1)",
        "slide-up": "slideUp 0.2s cubic-bezier(0.2, 0.9, 0.3, 1)",
        "slide-down": "slideDown 0.2s cubic-bezier(0.2, 0.9, 0.3, 1)",
        "scale-in": "scaleIn 0.2s cubic-bezier(0.2, 0.9, 0.3, 1)",
        "pulse-slow": "pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite"
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" }
        },
        slideUp: {
          "0%": { transform: "translateY(0.5rem)", opacity: "0" },
          "100%": { transform: "translateY(0)", opacity: "1" }
        },
        slideDown: {
          "0%": { transform: "translateY(-0.5rem)", opacity: "0" },
          "100%": { transform: "translateY(0)", opacity: "1" }
        },
        scaleIn: {
          "0%": { transform: "scale(0.95)", opacity: "0" },
          "100%": { transform: "scale(1)", opacity: "1" }
        }
      },
      backdropBlur: {
        xs: "2px"
      },
      transitionTimingFunction: {
        "in-expo": "cubic-bezier(0.95, 0.05, 0.795, 0.035)",
        "out-expo": "cubic-bezier(0.19, 1, 0.22, 1)",
        "smooth": "cubic-bezier(0.2, 0.9, 0.3, 1)"
      },
      zIndex: {
        "1": "1",
        "2": "2",
        "3": "3",
        dropdown: "10",
        sticky: "20",
        fixed: "30", 
        modal: "40",
        popover: "50",
        tooltip: "60",
        max: "99999"
      }
    }
  },
  plugins: [
    require("@tailwindcss/forms")({
      strategy: "class"
    }),
    require("@tailwindcss/typography"),
    require("@tailwindcss/aspect-ratio"),
    // Plugin personalizado para utilidades adicionais
    function({ addUtilities, addBase, theme }) {
      addBase({
        "*": {
          "box-sizing": "border-box"
        },
        "html": {
          "font-feature-settings": '"cv11", "ss01"',
          "font-variation-settings": '"opsz" 32'
        },
        "body": {
          "font-feature-settings": '"cv11", "ss01"',
          "font-variation-settings": '"opsz" 32'
        }
      })
      
      addUtilities({
        ".scrollbar-hide": {
          "-ms-overflow-style": "none",
          "scrollbar-width": "none",
          "&::-webkit-scrollbar": {
            display: "none"
          }
        },
        ".scrollbar-thin": {
          "scrollbar-width": "thin",
          "scrollbar-color": `${theme("colors.neutral.300")} transparent`,
          "&::-webkit-scrollbar": {
            width: "6px",
            height: "6px"
          },
          "&::-webkit-scrollbar-track": {
            background: "transparent"
          },
          "&::-webkit-scrollbar-thumb": {
            background: theme("colors.neutral.300"),
            "border-radius": "3px"
          },
          "&::-webkit-scrollbar-thumb:hover": {
            background: theme("colors.neutral.400")
          }
        },
        ".text-balance": {
          "text-wrap": "balance"
        }
      })
    }
  ]
}