(ns clockwork-ui.init
    (:require [clockwork-ui.core :as core]
              [clockwork-ui.conf :as conf]))

(enable-console-print!)

(defn start-clockwork! []
  (core/init! conf/setting))

(start-clockwork!)
