const bundledImages = require.context('../assets/images', false, /\.(png|jpe?g|gif|webp|bmp)$/i)

export function resolveImageUrl(imagePath) {
  if (!imagePath) return ''
  const value = String(imagePath).trim().replace(/\\/g, '/')
  if (/^(https?:|blob:|data:)/i.test(value)) return value

  const filename = value.substring(value.lastIndexOf('/') + 1)
  try {
    return bundledImages('./' + filename)
  } catch (e) {
    if (/^\/?static\/uploads\//i.test(value)) {
      const host = process.env.VUE_APP_WS_URL || 'localhost:8088'
      const path = value.startsWith('/') ? value : '/' + value
      return location.protocol + '//' + host + path
    }
    return ''
  }
}
