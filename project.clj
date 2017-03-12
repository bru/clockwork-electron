(defproject clockwork "0.1.2-SNAPSHOT"
  :description "A convenient time tracker"
  :url "http://100starlings.github.io/clockwork"
  :license {
    :name "Eclipse Public License"
    :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.9.473" :exclusions [org.apache.ant/ant]]
    [org.clojure/core.async "0.2.395"]
    [figwheel "0.5.9"]
    [reagent "0.6.1"]
    [re-frame "0.9.2"]
    [ring/ring-core "1.5.0"]
    [com.andrewmcveigh/cljs-time "0.5.0-alpha2"]
    [binaryage/devtools "0.9.2"]]
  :plugins [
    [lein-cljsbuild "1.1.5"  :exclusions [[org.clojure/clojure]]]
    [lein-externs "0.1.6"]
    [lein-shell "0.5.0"]
    [lein-figwheel "0.5.9" :exclusions [org.clojure/core.cache]]]
  :source-paths ["src/tools"]

  ;; Configuration variables used by our own build processes.
  :electron-version "1.6.2"
  :electron-packager-version "^8.5.2"

  :aliases {
    "clockwork-init" [
      "do"
      ["shell" "scripts/setup.sh"
       :project/name :project/description
       :project/version :project/url
       :project/electron-version
       :project/electron-packager-version]
      ["shell" "npm" "install"]]
    "clockwork-externs-dev" [
      "do"
      ["externs" "dev-main" ".out/dev/js/externs.js"]
      ["externs" "dev-ui" ".out/dev/js/externs_ui.js"]]
    "clockwork-externs-prod" [
      "do"
      ["externs" "prod-main" ".out/prod/js/externs.js"]
      ["externs" "prod-ui" ".out/prod/js/externs_ui.js"]]
    "clockwork-figwheel" ["trampoline" "figwheel" "dev-ui"]
    "clockwork-once-dev" [
      "do"
      ["cljsbuild" "once" "dev-main"]
      ["cljsbuild" "once" "dev-ui"]
      ["shell" "grunt" "generate-manifest" "--target=.out/dev"]
      ["shell" "grunt" "copy-file" "--source=./src/main/hoist/dev.js" "--target=.out/dev/js/main.js"]
      ["shell" "grunt" "copy-file" "--source=./src/ui/hoist/dev.html" "--target=.out/dev/index.html"]
      ;; ["shell" "grunt" "symlink" "--source=./src/ui/public" "--target=.out/dev/public"]
      ;; ["shell" "grunt" "symlink" "--source=.out/dev" "--target=resources/public"]
      ]
    "clockwork-once-prod" [
      "do"
      ["cljsbuild" "once" "prod-main"]
      ["cljsbuild" "once" "prod-ui"]
      ["shell" "grunt" "generate-manifest" "--target=.out/prod"]
      ["shell" "grunt" "copy-file" "--source=./src/main/hoist/prod.js" "--target=.out/prod/js/main.js"]
      ["shell" "grunt" "copy-file" "--source=./src/ui/hoist/prod.html" "--target=.out/prod/index.html"]
      ;; ["shell" "grunt" "symlink" "--source=ui/public" "--target=.out/prod/public"]
      ]

    "clockwork-dist" ["shell" "scripts/package.sh"]}

  :hooks [leiningen.cljsbuild]
  :clean-targets ^{:protect false} [".out" ".dist"]

  :cljsbuild {
    :builds {
      :dev-main {
        :source-paths ["src/main"]
        :compiler {
          :output-to ".out/dev/js/electron-main.js"
          :externs [
            ".out/dev/js/externs.js"
            "node_modules/closurecompiler-externs/path.js"
            "node_modules/closurecompiler-externs/process.js"]
          :target :nodejs
          :optimizations :simple
          :output-wrapper true}}
      :dev-ui {
        :source-paths ["src/ui" "src/ui_profile/clockwork_ui/dev"]
        :compiler {
          :output-to ".out/dev/js/ui.js"
          :externs [".out/dev/js/externs_ui.js"]
          :output-dir ".out/dev/js/out"
          :optimizations :none
          :source-map true
          :source-map-timestamp true
          :output-wrapper true
          :preloads [devtools.preload]}}
      :prod-main {
        :source-paths ["src/main"]
        :compiler {
          :output-to ".out/prod/js/cljsbuild-main.js"
          :externs [
            ".out/prod/js/externs.js"
            "node_modules/closurecompiler-externs/path.js"
            "node_modules/closurecompiler-externs/process.js"]
          :elide-asserts true
          :target :nodejs
          :output-dir ".out/prod/js/out-main"
          :optimizations :simple
          :output-wrapper true}}
      :prod-ui {
        :source-paths ["src/ui" "src/ui_profile/clockwork_ui/prod"]
        :compiler {
          :output-to ".out/prod/js/ui.js"
          :externs [".out/prod/js/externs_ui.js"]
          :elide-asserts true
          :output-dir ".out/prod/js/out"
          :optimizations :simple
          :output-wrapper true}}}}
  :figwheel {
    :server-logfile ".out/dev/logs/figwheel-logfile.log"
    :http-server-root "public"
    :ring-handler figwheel-middleware/app
    :server-port 3449})
