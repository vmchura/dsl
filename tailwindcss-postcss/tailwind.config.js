module.exports = {
  purge: {
    content: ['../server/app/views/**/*.scala.html',
              '../client/src/main/scala/**/*.scala']
  },
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {},
  },
  variants: {
    extend: {},
  },
  plugins: [],
}
