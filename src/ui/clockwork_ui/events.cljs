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
  (inject-cofx :running-timeslip)
  (inject-cofx :projects)
  check-spec-interceptor]
 (fn [{:keys [db current-day clock running-timeslip projects]} _]
   "Initialise the app-db with the current clock and active day, then dispatch
    the load-timeslips event to load the timeslips into the app-db"
   {:db (-> default-value
            (assoc :clock clock
                   :active-day current-day
                   :panel "timeslips")
            (cond-> running-timeslip (assoc :running-timeslip running-timeslip))
            (cond-> projects (assoc :projects projects)))
    :dispatch [:load-timeslips]}))

(reg-event-fx
 :update-clock
 [(inject-cofx :read-clock)
  check-spec-interceptor]
 (fn [{:keys [db clock]} _]
   "Tic-tac, this event updates the clock in the app-db"
   {:db (assoc db :clock clock)}))

(reg-event-db
 :goto-timeslips
 [check-spec-interceptor]
 (fn [db _]
   "Set timeslips as the active panel"
   (assoc db :panel "timeslips")))

(reg-event-db
 :goto-projects
 [check-spec-interceptor]
 (fn [db _]
   "Set projects as the active panel"
   (assoc db :panel "projects")))

(reg-event-fx
 :save-timeslips
 (fn [{{:keys [timeslips active-day running-timeslip]} :db} _]
   "Invoke the timeslips->file and :running->file side effects to save the timeslips in the app-db to file"
   {:timeslips->file [timeslips active-day]
    :running->file running-timeslip}))

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
 (fn [{{:keys [active-day] :as db} :db} _]
   "Set the day before as the active-day and load the timeslips into the app-db"
   (let [day (time/minus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :goto-next-day
 [check-spec-interceptor]
 (fn [{{:keys [active-day] :as db} :db} _]
   "Set the day after as the active-day and load the timeslips into the app-db"
   (let [day (time/plus active-day (time/days 1))]
     {:db (assoc db :active-day day)
      :dispatch [:load-timeslips]})))

(reg-event-fx
 :update-timeslip
 [check-spec-interceptor]
 (fn [{{:keys [clock timeslips] :as db} :db}
      [_ {:keys [id started-at stopped-at duration] :as timeslip}]]
   (let [original (get timeslips id)
         new-start (if duration
                     (ft/to-string
                      (time/minus
                       (ft/from-string stopped-at) (time/seconds duration)))
                     started-at)
         updated-at (ft/to-string clock)
         new-timeslip (merge original
                             (dissoc timeslip :duration)
                             {:started-at new-start
                              :updated-at updated-at})]
     {:db (assoc-in db [:timeslips id] new-timeslip)
      :dispatch [:save-timeslips]})))

(reg-event-fx
 :remove-timeslip
 [check-spec-interceptor]
 (fn [{:keys [db]} [_ id]]
   "Remove a timeslip from the app-db"
   {:db (update-in db [:timeslips] dissoc id)
    :dispatch [:save-timeslips]}))

(reg-event-fx
 :stop-timeslip
 [check-spec-interceptor]
 (fn [{{:keys [clock running-timeslip] :as db} :db} [_ id]]
   (let [timeslip (assoc running-timeslip :stopped-at (ft/to-string clock))]
     {:db (assoc-in (dissoc db :running-timeslip)
                    [:timeslips id] timeslip)
      :dispatch [:save-timeslips]})))

(reg-event-fx
 :add-timeslip
 [check-spec-interceptor]
 (fn [{{:keys [clock] :as db} :db} [_ timeslip]]
   "Add a new running timeslip to the app-db"
   (let [id (str clock)
         now (ft/to-string clock)
         new-timeslip (merge timeslip {:id id
                                       :started-at now
                                       :updated-at now})]
     {:db (assoc db :running-timeslip new-timeslip)
      :dispatch [:save-timeslips]})))
