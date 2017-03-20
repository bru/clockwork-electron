(ns clockwork-ui.events
  (:require
   [re-frame.core :refer [reg-event-db reg-cofx reg-event-fx inject-cofx path
                          trim-v after debug]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as ft]
   [cljs.spec :as s]
   [clockwork-ui.db :refer [default-value]]
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
 (fn [{:keys [db current-day clock]} _]
   "Initialise the app-db with the current clock and active day, then dispatch
    the load-timeslips event to load the timeslips into the app-db"
   {:db (assoc default-value
               :clock clock
               :active-day current-day)
    :dispatch [:load-timeslips]}))

(reg-event-fx
 :update-clock
 [(inject-cofx :read-clock)
  check-spec-interceptor]
 (fn [{:keys [db clock]} _]
   "Tic-tac, this event updates the clock in the app-db"
   {:db (assoc db :clock clock)}))

(reg-event-fx
 :save-timeslips
 (fn [{{:keys [timeslips active-day]} :db} _]
   "Invoke the timeslips->file side effect to save the timeslips in the app-db to file"
   {:timeslips->file [timeslips active-day]}))

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

(reg-event-db
 :update-timeslip
 [check-spec-interceptor]
 (fn [{:keys [clock] :as db}
      [_ {:keys [id started-at stopped-at duration] :as timeslip}]]
   (let [original (get-in db [:timeslips id])
         new-start (ft/to-string
                    (time/minus
                     (ft/from-string stopped-at) (time/seconds duration)))
         updated-at (ft/to-string clock)
         new-timeslip (merge original
                             (dissoc timeslip :duration)
                             {:started-at new-start
                              :updated-at updated-at})]
     (assoc-in db [:timeslips id] new-timeslip))))

(reg-event-db
 :remove-timeslip
 [check-spec-interceptor]
 (fn [db [_ id]]
   "Remove a timeslip from the app-db"
   (update-in db [:timeslips] dissoc id)))

(reg-event-db
 :stop-timeslips
 [check-spec-interceptor]
 (fn [{:keys [timeslips clock] :as db} [_ id]]
   (let [now (ft/to-string clock)
         stop? (fn [[k ts]] (not (or (= k id) (:stopped-at ts))))
         stop (fn [[k ts]] [k (assoc ts :stopped-at now :updated-at now)])
         updated-timeslips (->> timeslips
                                (filter stop?)
                                (map stop)
                                (into timeslips))]
     (assoc db :timeslips updated-timeslips))))

(reg-event-db
 :stop-timeslip
 [check-spec-interceptor]
 (fn [{:keys [clock] :as db} [_ id]]
   (assoc-in db [:timeslips id :stopped-at] (ft/to-string clock))))

(reg-event-fx
 :add-timeslip
 [check-spec-interceptor]
 (fn [{:keys [db]} [_ timeslip]]
   "Add a new timeslip to the app-db"
   (let [clock (:clock db)
         id (str clock)
         now (ft/to-string clock)
         new-timeslip (merge timeslip {:id id
                                       :started-at now
                                       :updated-at now})]
     {:db (assoc-in db [:timeslips id] new-timeslip)
      :dispatch [:stop-timeslips id]})))
