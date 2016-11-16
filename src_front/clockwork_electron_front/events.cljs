(ns clockwork-electron-front.events
  (:require
   [re-frame.core :refer [reg-event-db reg-cofx reg-event-fx inject-cofx path
                          trim-v after debug]]
   [cljs-time.core :as time]
   [clockwork-electron-front.db :refer [db default-value]]
   [clockwork-electron-front.utils :as u]
   ))

;; INTERCEPTORS ------------------------

(def timeslip-interceptors
  [(path :timeslips)
   trim-v])

;; HELPERS -----------------------------


;; EVENT HANDLERS ----------------------

(reg-event-fx
 :initialise-db
 [(inject-cofx :read-clock)
  (inject-cofx :current-day)
  (inject-cofx :load-timeslips)
  ]
 (fn [cofx event]
   (let [db (:db cofx)
         current-day (:current-day cofx)
         clock (:clock cofx)
         timeslips (:timeslips cofx)]
     {:db (assoc default-value
                 :timeslips timeslips
                 :clock clock
                 :active-day current-day)})))

(reg-event-fx
 :goto-today
 [(inject-cofx :current-day)]
 (fn [{:keys [db current-day]} _]
   {:db (assoc db :active-day current-day)}))

(reg-event-db
 :goto-previous-day
 [(path :active-day)]
 (fn [active-day _]
  (time/minus active-day (time/days 1))))

(reg-event-db
 :goto-next-day
 [(path :active-day)]
 (fn [active-day _]
   (time/plus active-day (time/days 1))))
