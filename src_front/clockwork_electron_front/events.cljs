(ns clockwork-electron-front.events
  (:require
   [re-frame.core :refer [reg-event-db reg-cofx reg-event-fx inject-cofx path
                          trim-v after debug]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as ft]
   [clockwork-electron-front.db :refer [default-value]]
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
  (inject-cofx :current-day)]
 (fn [cofx event]
   (let [db (:db cofx)
         current-day (:current-day cofx)
         clock (:clock cofx)]
     {:db (assoc default-value
                 :clock clock
                 :active-day current-day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :update-clock
 [(inject-cofx :read-clock)]
 (fn [{:keys [db clock]} _]
   {:db (assoc db :clock clock)}))

(reg-event-fx
 :save-timeslips
 (fn [{:keys [db]} _]
   (let [timeslips (:timeslips db)
         active-day (:active-day db)]
     {:timeslips->file [timeslips active-day]})))

(reg-event-fx
 :load-timeslips
 [(inject-cofx :active-timeslips)]
 (fn [{:keys [db timeslips]} _]
   {:db (assoc db :timeslips timeslips)}))

(reg-event-fx
 :goto-today
 [(inject-cofx :current-day)]
 (fn [{:keys [db current-day]} _]
   {:db (assoc db :active-day current-day)
    :dispatch [:load-timeslips]}))

(reg-event-fx
 :goto-previous-day
 (fn [{:keys [db]} _]
   (let [active-day (:active-day db)
         day (time/minus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :goto-next-day
 (fn [{:keys [db]} _]
   (let [active-day (:active-day db)
         day (time/plus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

(reg-event-db
 :toggle-timeslip
 (fn [db [_ id]]
   (let [{:keys [active updated-at duration] :as timeslip} (get-in db [:timeslips id])
         elapsed-time (time/in-seconds (time/interval (ft/from-string updated-at) (time/now)))
         duration (if active (+ duration elapsed-time) duration)
         new-timeslip (merge timeslip
                               {:duration duration
                                :updated-at (ft/to-string (time/now))
                                :active (not active)}
                               )]
     (assoc-in db [:timeslips id] new-timeslip))))

(reg-event-db
 :update-timeslip
 (fn [db [_ timeslip]]
   (let [{:keys [active updated-at duration id]} timeslip
         elapsed-time (time/in-seconds (time/interval (ft/from-string updated-at) (time/now)))
         duration (if active (+ duration elapsed-time) duration)
         new-timeslip (merge timeslip {:duration duration
                                       :updated-at (ft/to-string (time/now))})]
     (assoc-in db [:timeslips id] new-timeslip))))

(reg-event-db
 :remove-timeslip
 (fn [db [_ id]]
   (update-in db [:timeslips] dissoc id)))

(reg-event-db
 :add-timeslip
 (fn [db [_ timeslip]]
   (let [id (str (time/now))
         day (str (:active-day db))
         new-timeslip (merge timeslip {:id id
                                       :day day
                                       :updated-at (ft/to-string (time/now))})]
     (assoc-in db [:timeslips id] new-timeslip))))
