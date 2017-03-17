(ns clockwork-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [cljs-time.core :as time]))

(reg-sub
 :db
 (fn [db _]
   "this will signal ANY change in the db. Only used for debugging purposes"
   db))

(reg-sub
 :active-day
 (fn [db _]
   (:active-day db)))

(reg-sub
 :timeslips
 (fn [db _]
   (:timeslips db)))

(reg-sub
 :clock
 (fn [db _]
   (:clock db)))

(reg-sub
 :active-day-timeslips
 (fn [query-v _]
   [(subscribe [:timeslips])
    (subscribe [:active-day])])

 (fn [[timeslips active-day] _]
   (let [timeslips (if (nil? timeslips) [] (vals timeslips))]
     (filter #(time/overlaps?
               (:started-at %) (:stopped-at %)
               (start active-day) (end active-day))
             timeslips))))
