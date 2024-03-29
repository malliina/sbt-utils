import resolve from "@rollup/plugin-node-resolve"
import commonjs from "@rollup/plugin-commonjs"
import replace from "@rollup/plugin-replace"
import terser from "@rollup/plugin-terser"
import {scalajs, production, outputDir, urlOptions} from "./scalajs.rollup.config.js"
import path from "path"
import extractcss from "./rollup-extract-css"
import type {RollupOptions} from "rollup"
import {defaultSourcemapFix, sourcemaps} from "./rollup-sourcemaps"

const resourcesDir = "../src/main/resources"
const cssDir = path.resolve(resourcesDir, "css")

const entryNames = "[name].js"

const css = () => extractcss({
  outDir: outputDir,
  minimize: production,
  sourcemap: !production,
  urlOptions: urlOptions
})

const config: RollupOptions[] = [
  {
    input: scalajs.input,
    plugins: [
      sourcemaps(),
      replace({
        "process.env.NODE_ENV": JSON.stringify(production ? "production" : "development"),
        preventAssignment: true
      }),
      css(),
      resolve({browser: true, preferBuiltins: false}),
      commonjs(),
      production && terser()
    ],
    output: {
      dir: outputDir,
      format: "iife",
      name: "version",
      entryFileNames: entryNames,
      inlineDynamicImports: true,
      sourcemap: !production,
      sourcemapPathTransform: (relativeSourcePath, sourcemapPath) =>
        defaultSourcemapFix(relativeSourcePath)
    },
    context: "window"
  },
  {
    input: {
      fonts: path.resolve(cssDir, "fonts.js"),
      styles: path.resolve(cssDir, "app.js")
    },
    plugins: [
      css(),
      production && terser()
    ],
    output: {
      dir: outputDir,
      entryFileNames: entryNames
    }
  }
]

export default config
