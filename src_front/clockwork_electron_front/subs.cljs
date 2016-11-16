(ns clockwork-electron-front.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ft]))

(reg-sub
 :active-day
 (fn [db _]
   (:active-day db)))

(reg-sub
 :timeslips
 (fn [db _]
   (:timeslips db)))

(reg-sub
 :active-day-timeslips
 (fn [query-v _]
   [(subscribe [:timeslips])
    (subscribe [:active-day])])

 (fn [[timeslips active-day] _]
   (let [start-time (time/at-midnight (ft/to-date-time active-day))
         end-time (time/plus start-time (time/days 1))
         timeslips (vals timeslips)]
     (filter #(time/within? start-time
                            end-time
                            (ft/from-long (* 1000 (:updated-at %))))
             timeslips))))
