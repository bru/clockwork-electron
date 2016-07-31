(ns clockwork-electron-front.timeslips
  (:require [reagent.core :as r]
            [cljs.nodejs :as nodejs]
            [clojure.string :as s]
            [cljs.reader :as edn]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ft]
            [clockwork-electron-front.db :refer [db]]
            [clockwork-electron-front.utils :as u]))

;; node / js requires
(def path (nodejs/require "path"))
(def fs (nodejs/require "fs"))
(def Electron (nodejs/require "electron"))
(def remote (.-remote Electron))
(def dialog (.-dialog remote))
(def app (.-app remote))

(enable-console-print!)

;; low level clock
(defn update-clock []
  (swap! db
         assoc-in [:clock] (time/now)))

(defn get-clock []
  (:clock @db))

(defn read-clock []
  (u/get-timestamp (get-clock)))

(defn set-active-day [date]
  (swap! db
         assoc-in [:active-day] date))

(defn get-active-day []
  (:active-day @db))

(defn goto-previous-day []
  (let [current-day (get-active-day)
        previous-day (time/minus current-day (time/days 1))]
    (set-active-day previous-day)))

(defn goto-next-day []
  (let [current-day (get-active-day)
        next-day (time/plus current-day (time/days 1))]
    (set-active-day next-day)))

;; low level timeslip operations
;; most of these could be refactored further so that we have just
;; one or two functions messing with the state

(defn read-timeslips []
  (:timeslips @db))

(defn write-timeslips [timeslips]
  (swap! db assoc :timeslips timeslips))

;; higher level timeslip operations

(defn active-day-timeslips []
  (let [start-time (time/at-midnight (ft/to-date-time (get-active-day)))
        end-time (time/plus start-time (time/days 1))
        timeslips (vals (read-timeslips))]
    (filter #(time/within? start-time
                           end-time
                           (ft/from-long (* 1000 (:updated-at %))))
            timeslips)))

(defn read-timeslip [id]
  (get (read-timeslips) id))

(defn update-timeslip [id attr value]
  (let [timeslip (-> (read-timeslip id)
                     (assoc attr value))
        timeslips (assoc (read-timeslips) id timeslip)]
    (write-timeslips timeslips))) ;; we don't really care about atomicity of the operation here

(defn get-timeslip-duration [id]
  (let [{:keys [active duration updated-at]} (read-timeslip id)
        now (read-clock)
        elapsed-time (u/seconds-between updated-at now)]
    (if active
      (+ duration elapsed-time)
      duration)))

(defn set-timeslip-state [id active?]
  (let [duration (get-timeslip-duration id)
        timestamp (read-clock)]
    (if active?
      (update-timeslip id :updated-at timestamp)
      (update-timeslip id :duration duration))
    (update-timeslip id :active active?)))

(defn stop-all-timeslips []
  (doseq [id (keys (read-timeslips))]
    (set-timeslip-state id false)))

(defn add-timeslip [timeslip]
  (let [timeslips (read-timeslips)
        id (inc (last (sort (keys timeslips))))
        timeslip (assoc timeslip :id id)
        new-timeslips (assoc timeslips id timeslip)]
    (stop-all-timeslips)
    (write-timeslips new-timeslips)))

(defn remove-timeslip [id]
  (let [timeslips (read-timeslips)
        new-timeslips (dissoc timeslips id)]
    (write-timeslips new-timeslips)))

(defn set-timeslip-clock [id duration]
  (when-let [seconds (u/parse-duration-string duration)]
    (update-timeslip id :updated-at (read-clock))
    (update-timeslip id :clock-input nil)
    (update-timeslip id :duration seconds)))

(defn set-timeslip-clock-input [id duration]
  (update-timeslip id :clock-input duration))

(defn edit-timeslip [id]
  (update-timeslip id :edit? true))

(defn unedit-timeslip [id]
  (let [timeslip (read-timeslip id)
        clock-input (:clock-input timeslip)
        active? (:active timeslip)]
    (when (and (not active?) (not (s/blank? clock-input)))
      (set-timeslip-clock id clock-input))
    (update-timeslip id :edit? false)))

;; timeslip file storage

(defn timeslips-file [clock]
  (let [data-path (.getPath app "userData")
        year (time/year clock)
        week (time/week-number-of-year clock)
        filename (str year "-" week "-timeslips.edn")]
    (.resolve path data-path filename)))

(defn load-timeslips []
  (.readFile fs (timeslips-file (get-active-day))
             (fn [err data]
               (let [d (if (nil? data) {} data)
                     timeslips (edn/read-string (str d))]
                 (write-timeslips timeslips)))))

(defn save-timeslips []
  (let [file (timeslips-file (get-active-day))]
    (.writeFile fs file (read-timeslips))))

;; state and periodic tasks
(defonce init-db
  (do
    (update-clock)
    (set-active-day (time/today))
    (load-timeslips)))

(defonce timeslips-updater
  (js/setInterval update-clock 1000))

(defonce timeslips-saver
  (js/setInterval save-timeslips 10000))
