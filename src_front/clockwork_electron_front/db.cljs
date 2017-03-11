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

(def default-value
  {:timeslips {}
   :clock nil})

(defn timeslips-file [day]
  "Build the filename for this day"
  (let [data-path (.getPath app "userData")
        year (time/year day)
        week (time/week-number-of-year day)
        filename (str year "-" week "-timeslips.edn")]
    (.resolve filepath data-path filename)))

(defn load-timeslips [day]
  "Load timeslips for a given day"
  (let [file (timeslips-file day)
        timeslip-data (try
                        (.readFileSync fs file)
                        (catch js/Error e nil))]
    (edn/read-string (str (if (nil? timeslip-data) {} timeslip-data)))))

(defn save-timeslips [timeslips day]
  "Save the timeslips to the right file"
  (let [file (timeslips-file day)]
    (try
      (.writeFileSync fs file timeslips)
      (catch js/Error e nil))))

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
 :active-timeslips
 (fn [cofx _]
   "Get timeslips for week including active day"
   (let [day (:active-day (:db cofx))
         timeslips (load-timeslips day)]
     (assoc cofx :timeslips timeslips))))

(re-frame/reg-fx
 :timeslips->file
 (fn [[ timeslips active-day ]]
   (save-timeslips timeslips active-day)))
