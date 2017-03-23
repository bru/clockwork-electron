(ns clockwork-ui.db
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [cljs.nodejs :as nodejs]
            [cljs.spec :as s]
            [cljs-time.core :as time]
            [cljs.reader :as edn]
            ))

;; NODE REQUIRES -----------------------

(def Electron (nodejs/require "electron"))
(def remote (.-remote Electron))
(def app (.-app remote))
(def filepath (nodejs/require "path"))
(def fs (nodejs/require "fs"))

;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handers
;; have an "after" interceptor which does the spec re-check.
;;
;; None of this is strictly necessary. It could be omitted. But we find it
;; good practice.

(s/def ::id string?)
(s/def ::task string?)
(s/def ::project string?)
(s/def ::client string?)
(s/def ::description string?)
(s/def ::started-at string?)
(s/def ::updated-at string?)
(s/def ::stopped-at string?)
(s/def ::timeslip
  (s/keys :req-un [::id
                   ::description
                   ::started-at
                   ::updated-at
                   ::stopped-at]
          :opt-un [::task ::client ::project]))
(s/def ::running-timeslip
  (s/keys :req-un [::id
                   ::description
                   ::started-at
                   ::updated-at]
          :opt-un [::task ::client ::project]))
(s/def ::timeslips (s/map-of ::id ::timeslip))
(s/def ::active-day time/date?)
(s/def ::clock time/date?)
(s/def ::db (s/keys :req-un [::clock ::active-day ::timeslips]
                    :opt-un [::running-timeslip]))

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

(def running-file
  (let [data-path (.getPath app "userData")
        filename "running-timeslip.edn"]
    (.resolve filepath data-path filename)))

(defn load-running []
  "Load running timeslip, if any"
  (let [running-data (try
                       (.readFileSync fs running-file)
                       (catch js/Error e nil))]
    (edn/read-string (str (if (nil? running-data) nil running-data)))))

(defn save-running [timeslip]
  "Save the timeslip to the running file"
  (try
    (if timeslip
      (.writeFileSync fs running-file timeslip)
      (.unlink fs running-file))
    (catch js/Error e nil)))

;; CO-EFFECTS --------------------------

(re-frame/reg-cofx
 :read-clock
 (fn [cofx _]
   "Get local time from system clock"
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

(re-frame/reg-cofx
 :running-timeslip
 (fn [cofx _]
   "Get the currently running timeslip, if any"
   (let [timeslip (load-running)]
     (assoc cofx :running-timeslip timeslip))))

(re-frame/reg-fx
 :timeslips->file
 (fn [[ timeslips active-day ]]
   "Save the timeslips for the week including the specified day"
   (save-timeslips timeslips active-day)))

(re-frame/reg-fx
 :running->file
 (fn [timeslip]
   "Save the state of the running timeslip (or lack thereof) to file"
   (save-running timeslip)))
