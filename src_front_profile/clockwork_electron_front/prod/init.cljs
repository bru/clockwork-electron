(ns clockwork-electron-front.init
    (:require [clockwork-electron-front.core :as core]
              [clockwork-electron-front.conf :as conf]))

(enable-console-print!)

(defn start-descjop! []
  (core/init! conf/setting))

(start-descjop!)
