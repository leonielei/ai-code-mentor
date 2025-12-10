<template>
  <div class="code-editor-container">
    <div class="editor-header">
      <h5 class="mb-0">{{ title }}</h5>
      <div class="editor-controls">
        <select v-model="selectedLanguage" class="form-select form-select-sm" @change="onLanguageChange">
          <option value="java">Java</option>
          <option value="python">Python</option>
          <option value="javascript">JavaScript</option>
        </select>
        <select v-model="selectedTheme" class="form-select form-select-sm ms-2" @change="onThemeChange">
          <option value="dark">Dark</option>
          <option value="light">Light</option>
        </select>
      </div>
    </div>
    <div class="editor-wrapper" :class="themeClass">
      <pre class="code-highlight"><code :class="`language-${selectedLanguage}`" ref="codeRef">{{ modelValue }}</code></pre>
      <textarea
        ref="textareaRef"
        :value="modelValue"
        @input="handleInput"
        @scroll="handleScroll"
        :readonly="readonly"
        :style="textareaStyle"
        class="code-textarea"
        spellcheck="false"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import Prism from 'prismjs'
import 'prismjs/themes/prism-tomorrow.css'
import 'prismjs/components/prism-java.js'
import 'prismjs/components/prism-python.js'
import 'prismjs/components/prism-javascript.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'java'
  },
  theme: {
    type: String,
    default: 'dark'
  },
  height: {
    type: String,
    default: '600px'
  },
  title: {
    type: String,
    default: 'Éditeur de Code'
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'languageChange', 'themeChange'])

const textareaRef = ref(null)
const codeRef = ref(null)
const selectedLanguage = ref(props.language)
const selectedTheme = ref(props.theme)

// Expose clean API
defineExpose({
  getValue() {
    return textareaRef.value?.value || ''
  },
  setValue(value) {
    if (textareaRef.value) {
      textareaRef.value.value = value || ''
      emit('update:modelValue', value || '')
      updateHighlight()
    }
  },
  hasContent() {
    const value = textareaRef.value?.value || ''
    return value && value.trim().length > 0
  }
})

// Theme class
const themeClass = computed(() => {
  return `theme-${selectedTheme.value}`
})

// Computed style for textarea
const textareaStyle = computed(() => {
  const isDark = selectedTheme.value === 'dark'
  return {
    height: props.height,
    fontFamily: 'Consolas, "Courier New", monospace',
    fontSize: '14px',
    lineHeight: '1.6',
    padding: '1rem',
    border: 'none',
    outline: 'none',
    resize: 'none',
    width: '100%',
    backgroundColor: 'transparent',
    color: 'transparent',
    caretColor: isDark ? '#d4d4d4' : '#000000',
    tabSize: 4
  }
})

// Update syntax highlighting
const updateHighlight = () => {
  nextTick(() => {
    if (codeRef.value) {
      Prism.highlightElement(codeRef.value)
    }
  })
}

// Handle input
const handleInput = (event) => {
  const value = event.target.value
  emit('update:modelValue', value)
  emit('change', value)
  updateHighlight()
}

// Sync scroll between textarea and code
const handleScroll = (event) => {
  if (codeRef.value) {
    const pre = codeRef.value.parentElement
    if (pre) {
      pre.scrollTop = event.target.scrollTop
      pre.scrollLeft = event.target.scrollLeft
    }
  }
}

// Watch for external value changes
watch(() => props.modelValue, (newValue) => {
  if (textareaRef.value && textareaRef.value.value !== newValue) {
    textareaRef.value.value = newValue || ''
    updateHighlight()
  }
})

// Watch for language changes
watch(() => selectedLanguage.value, () => {
  updateHighlight()
})

// Language change
const onLanguageChange = () => {
  updateHighlight()
  emit('languageChange', selectedLanguage.value)
}

// Theme change
const onThemeChange = () => {
  emit('themeChange', selectedTheme.value)
}

// Initial highlight
onMounted(() => {
  updateHighlight()
})
</script>

<style lang="scss" scoped>
.code-editor-container {
  border: 2px solid #dee2e6;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  background: white;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2);
}

.editor-header h5 {
  font-weight: 700;
  color: white;
  margin: 0;
}

.editor-controls {
  display: flex;
  gap: 0.5rem;
}

.form-select-sm {
  max-width: 150px;
  border-radius: 0.5rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.editor-wrapper {
  position: relative;
  height: v-bind('props.height');
  overflow: hidden;
  
  &.theme-dark {
    background-color: #1e1e1e;
  }
  
  &.theme-light {
    background-color: #ffffff;
  }
}

.code-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  padding: 1rem;
  overflow: auto;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  pointer-events: none;
  
  code {
    display: block;
    width: 100%;
    height: 100%;
    background: transparent !important;
    color: inherit;
  }
}

.code-textarea {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: auto;
  z-index: 1;
  
  &[readonly] {
    cursor: not-allowed;
    opacity: 0.7;
  }
  
  &:focus {
    outline: none;
  }
  
  // Tab support
  tab-size: 4;
  -moz-tab-size: 4;
  -o-tab-size: 4;
  
  // Make text transparent so syntax highlighting shows through
  &::selection {
    background: rgba(0, 123, 255, 0.3);
  }
}
</style>
