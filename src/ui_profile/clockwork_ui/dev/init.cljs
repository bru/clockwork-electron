(ns clockwork-ui.init
  (:require [figwheel.client :as fw :include-macros true]
            [clockwork-ui.core :as core]
            [clockwork-ui.conf :as conf]
            [devtools.core :as devtools]
            ))

(enable-console-print!)
(devtools/install!)

(fw/watch-and-reload
 :websocket-url   "ws://localhost:3449/figwheel-ws"
 :jsload-callback 'start-clockwork!)

(defn start-clockwork! []
  (core/init! conf/setting))

(start-clockwork!)
