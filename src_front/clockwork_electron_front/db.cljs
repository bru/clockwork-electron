(ns clockwork-electron-front.db
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [cljs.nodejs :as nodejs]
            [cljs-time.core :as time]
            [cljs.reader :as edn]
            ))

;; NODE REQUIRES -----------------------

(def Electron (nodejs/require "electron"))
(def remote (.-remote Electron))
(def app (.-app remote))
(def filepath (nodejs/require "path"))
(def fs (nodejs/require "fs"))

(defonce db (r/atom {:timeslips {}
                     :clock nil}))

(def default-value
  {:timeslips {}
   :clock nil})


;; CO-EFFECTS --------------------------

(re-frame/reg-cofx
 :read-clock
 (fn [cofx _]
   "Get local time form system clock."
   (let [now (time/now)]
     (assoc cofx :clock now))))

(re-frame/reg-cofx
 :current-day
 (fn [cofx _]
   "Get active date"
   (assoc cofx :current-day (time/today))))

(re-frame/reg-cofx
 :load-timeslips
 (fn [cofx _]
   "Load timeslips"
   (let [data-path (.getPath app "userData")
         clock (:clock cofx)
         year (time/year clock)
         week (time/week-number-of-year clock)
         filename (str year "-" week "-timeslips.edn")
         file (.resolve filepath data-path filename)
         timeslip-data (.readFileSync fs file)
         timeslips (edn/read-string (str (if (nil? timeslip-data) {} timeslip-data)))]
     (assoc cofx :timeslips timeslips))))

(defn subscribe [path]
  (r/cursor db path))
