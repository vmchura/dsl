const colors = require('tailwindcss/colors')
module.exports = {
  purge: {
    content: ['../server/app/views/**/*.scala.html',
              '../client/src/main/scala/**/*.scala']
  },
  darkMode: false, // or 'media' or 'class'
  theme: {
    colors: {
      primary: colors.violet,
      transparent: 'transparent',
      current: 'currentColor',
      black: colors.black,
      white: colors.white,
      gray: colors.trueGray,
      indigo: colors.indigo,
      red: colors.rose,
      yellow: colors.amber
    }
  },
  variants: {
    extend: {},
  },
  plugins: [],

}
