(ns clockwork-ui.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as tc]))

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
 :midnight
 (fn [db _]
   (time/plus (time/at-midnight (:clock db)) (time/seconds 86399))))

(reg-sub
 :active-day-timeslips
 :<- [:timeslips]
 :<- [:active-day]
 :<- [:midnight]
 (fn [[timeslips active-day midnight] _]
   (let [timeslips (if (nil? timeslips) [] (vals timeslips))
         start-active (time/at-midnight (tc/from-date active-day))
         end-active (time/plus start-active (time/seconds 86399))
         day-interval (time/interval start-active end-active)]
     (filter #(time/overlaps?
               (time/interval
                (tc/from-string (:started-at %))
                (or (tc/from-string (:stopped-at %)) midnight))
               day-interval)
             timeslips))))
