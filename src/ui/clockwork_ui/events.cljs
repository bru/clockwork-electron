(ns clockwork-ui.events
  (:require
   [re-frame.core :refer [reg-event-db reg-cofx reg-event-fx inject-cofx path
                          trim-v after debug]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as ft]
   [cljs.spec :as s]
   [clockwork-ui.db :refer [default-value]]
   [clockwork-ui.utils :as u]
   ))

;; INTERCEPTORS ------------------------

(defn check-and-throw
  "throw an exception if db doesn't match the spec"
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :clockwork-ui.db/db)))

;; HELPERS -----------------------------


;; EVENT HANDLERS ----------------------

(reg-event-fx
 :initialise-db
 [(inject-cofx :read-clock)
  (inject-cofx :current-day)
  check-spec-interceptor]
 (fn [cofx event]
   "Initialise the app-db with the current clock and active day, then dispatch
    the load-timeslips event to load the timeslips into the app-db"
   (let [db (:db cofx)
         current-day (:current-day cofx)
         clock (:clock cofx)]
     {:db (assoc default-value
                 :clock clock
                 :active-day current-day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :update-clock
 [(inject-cofx :read-clock)
  check-spec-interceptor]
 (fn [{:keys [db clock]} _]
   "Tic-tac, this event updates the clock in the app-db"
   {:db (assoc db :clock clock)}))

(reg-event-fx
 :save-timeslips
 (fn [{:keys [db]} _]
   "Invoke the timeslips->file side effect to save the timeslips in the app-db to file"
   (let [timeslips (:timeslips db)
         active-day (:active-day db)]
     {:timeslips->file [timeslips active-day]})))

(reg-event-fx
 :load-timeslips
 [(inject-cofx :active-timeslips)
  check-spec-interceptor]
 (fn [{:keys [db timeslips]} _]
   "Load the timeslips for the active day into the app-db"
   {:db (assoc db :timeslips timeslips)}))

(reg-event-fx
 :goto-today
 [(inject-cofx :current-day)
  check-spec-interceptor]
 (fn [{:keys [db current-day]} _]
   "Set today as the active-day and load the timeslips into the app-db"
   {:db (assoc db :active-day current-day)
    :dispatch [:load-timeslips]}))

(reg-event-fx
 :goto-previous-day
 [check-spec-interceptor]
 (fn [{:keys [db]} _]
   "Set the day before as the active-day and load the timeslips into the app-db"
   (let [active-day (:active-day db)
         day (time/minus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :goto-next-day
 [check-spec-interceptor]
 (fn [{:keys [db]} _]
   "Set the day after as the active-day and load the timeslips into the app-db"
   (let [active-day (:active-day db)
         day (time/plus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

;; (reg-event-db
;;  :toggle-timeslip
;;  [check-spec-interceptor]
;;  (fn [db [_ id]]
;;    (let [{:keys [active updated-at duration] :as timeslip} (get-in db [:timeslips id])
;;          elapsed-time (time/in-seconds (time/interval
;;                                         (ft/from-string updated-at) (time/now)))
;;          duration (if active (+ duration elapsed-time) duration)
;;          new-timeslip (merge timeslip
;;                                {:duration duration
;;                                 :updated-at (ft/to-string (time/now))
;;                                 :active (not active)}
;;                                )]
;;      (assoc-in db [:timeslips id] new-timeslip))))

(reg-event-db
 :update-timeslip
 [check-spec-interceptor]
 (fn [db [_ timeslip]]
   (let [{:keys [id started-at stopped-at duration]} timeslip
         original (get-in db [:timeslips id])
         original-duration (time/interval
                            (ft/from-string (:started-at original))
                            (time/now))
         new-started-at (if stopped-at
                          started-at
                          (ft/to-string
                           (time/minus
                            (time/now) duration)))
         new-stopped-at (if stopped-at
                          (ft/to-string
                           (time/plus
                            (time/now) duration)))
         new-timeslip (merge original (dissoc timeslip :duration))]
     (assoc-in db [:timeslips id] new-timeslip))))

(reg-event-db
 :remove-timeslip
 [check-spec-interceptor]
 (fn [db [_ id]]
   "Remove a timeslip from the app-db"
   (update-in db [:timeslips] dissoc id)))

(reg-event-db
 :add-timeslip
 [check-spec-interceptor]
 (fn [db [_ timeslip]]
   "Add a new timeslip to the app-db"
   (let [id (str (time/now))
         day (str (:active-day db))
         new-timeslip (merge timeslip {:id id
                                       :day day
                                       :updated-at (ft/to-string (time/now))})]
     (assoc-in db [:timeslips id] new-timeslip))))
