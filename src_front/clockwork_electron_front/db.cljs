(ns clockwork-electron-front.db
  (:require [reagent.core :as r]))

(defonce db (r/atom {:timeslips {}
                     :clock nil}))

(defn subscribe [path]
  (r/cursor db path))
