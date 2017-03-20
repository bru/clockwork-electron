(ns clockwork-ui.views
  (:require [reagent.core :as r]
            [cljs.nodejs :as nodejs]
            [clojure.string :as s]
            [cljs-time.core :as time]
            [cljs-time.coerce :as ft]
            [clockwork-ui.utils :as u]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.pprint :refer [pprint]]
            ))

(def Electron (nodejs/require "electron"))
(def remote (.-remote Electron))
(def dialog (.-dialog remote))

(defn new-timeslip-form []
  (let [val (r/atom "")
        new-timeslip (fn [] {:description @val})
        change #(do
                  (reset! val (u/event-val %)))
        save #(let [timeslip (new-timeslip)]
                (.preventDefault %)
                (reset! val "")
                (dispatch [:add-timeslip timeslip]))]
    (fn []
        [:form
         [:div.col-xs-12.col-sm-12.col-md-8.col-lg-8
          [:input.form-control.input-lg
           {:id "new-timeslip-description"
            :type "text"
            :placeholder "What are you working on?"
            ;; :autofocus true
            :value @val
            :on-change change
            :on-key-down #(case (.-which %)
                            13 (save %)
                            nil)}]]
         [:div.col-xs-12.col-sm-12.col-md-4.col-lg-4
          [:button.btn.btn-success.btn-lg.col-xs-12.col-sm-12.col-md-12.col-lg-12
           {:type "button"
            :on-click save} "Start"]]])))

(defn head-row []
  (let [date (subscribe [:active-day])]
    (fn []
      (let [weekday (nth u/weekdays (- (time/day-of-week @date) 1))
            month-number (- (time/month @date) 1)
            month (nth u/months month-number)
            day (time/day @date)]
        [:div.row
         [:div.col-sm-4
          [:h2 (str weekday " ")
           [:small (str day " " month)]]
          [:div.btn-group {:role "group"}
           [:button.btn.btn-default
            {:type "button"
             :on-click #(dispatch [:goto-previous-day])}
            [:span.glyphicon.glyphicon-backward {:aria-hidden true}]]
           [:button.btn.btn-default
            {:type "button"
             :on-click #(dispatch [:goto-today])}
            [:span "Today"]]
           [:button.btn.btn-default
            {:type "button"
             :on-click #(dispatch [:goto-next-day])}
            [:span.glyphicon.glyphicon-forward {:aria-hidden true}]]]]
         [:div.col-sm-8
          [new-timeslip-form]]]))))

(defn start-button [timeslip]
  [:button.btn.btn-default
   {:type "button"
    :on-click #(dispatch [:add-timeslip (dissoc timeslip :stopped-at)])}
   [:span {:class (str "glyphicon glyphicon-play")
           :aria-hidden "true"}]
   [:span {:class "sr-only"} "Start"]])

(defn stop-button [{:keys [id]}]
  [:button.btn.btn-default
   {:type "button"
    :on-click #(dispatch [:stop-timeslip id])}
   [:span {:class (str "glyphicon glyphicon-pause")
           :aria-hidden "true"}]
   [:span {:class "sr-only"} "Stop"]])

(defn remove-warning-dialog [id]
  (let [options {:type "question"
                 :buttons ["Cancel" "Remove"]
                 :defaultId 0
                 :title "Confirm Timeslip Deletion"
                 :message "Delete this timeslip?"
                 :detail "Are you sure? This action cannot be undone"
                 }
        action (.showMessageBox dialog (clj->js options))]
    (if (= action 1)
      (dispatch [:remove-timeslip id]))))

(defn timeslip-duration []
    (fn [timeslip clock]
      (let [duration (time/in-seconds (u/timeslip-interval timeslip clock))]
        [:p.lead
         [:strong
          (u/format-time duration true)]])))

(defn text-input [form attribute]
  [:div.form-group
   [:label.col-sm-2.control-label
    (s/capitalize (name attribute))]
   [:div.col-sm-10
    [:input.form-control
     {:type "text"
      :value (get @form attribute)
      :on-change #(swap! form assoc attribute (u/event-val %))}]]])

(defn text-area [form attribute]
  [:textarea.form-control
   {:value (get @form attribute)
    :on-change #(swap! form assoc attribute (u/event-val %))}])

(defn time-input [form attribute]
  (let [val (u/format-time (get @form attribute))
        on-change #(swap! form assoc attribute
                          (u/parse-duration-string (u/event-val %)))]
    [:input.form-control
     {:type "text"
      :placeholder val
      :on-change on-change}]))

(defn timeslip-row []
  (let [editing (r/atom false)]
    (fn [timeslip clock]
       (if @editing
         ;; timeslip edit
         (let [{:keys [id stopped-at]} timeslip
               active (not stopped-at)
               duration (u/timeslip-interval timeslip clock)
               new-timeslip (r/atom (assoc timeslip :duration duration))]
           [:tr {:id (str "timeslip-" id)}
            [:td
             [:form.form-horizontal
              [text-input new-timeslip :client]
              [text-input new-timeslip :project]
              [text-input new-timeslip :task]
              [text-area new-timeslip :description]]]
            [:td
             [:div.col-md-6.col-lg-8.col-sm-12
              [:div.col-sm-8
               (if (not active)
                 [time-input new-timeslip :duration]
                 [timeslip-duration timeslip clock])]
              [:div.col-sm-4
               [toggle-button timeslip]]]
             [:div.col-md-6.col-lg-4.col-sm-12
              [:div
               [:button.btn.btn-default.col-sm-12
                {:on-click #(do
                              (dispatch [:update-timeslip @new-timeslip])
                              (reset! editing false))}
                [:span.glyphicon.glyphicon-save
                 {:aria-hidden "true"}]
                [:span {:class "sr-only"} "Save Timeslip"]]
               [:button.btn.btn-danger.col-sm-12
                {:on-click #(remove-warning-dialog id)}
                [:span.glyphicon.glyphicon-remove
                 {:aria-hidden "true"}]
                [:span {:class "sr-only"} "Remove Timeslip"]]]]]])

         ;; timeslip show
         (let [{:keys [id project client task description]} timeslip
               project-html (if-not (s/blank? project) [:strong project])
               client-html (if-not (s/blank? client) (str " (" client ")"))
               task-html (if-not (s/blank? task) (str task " - ")) ]
           [:tr {:id (str "timeslip-" (:id timeslip))}
            [:td
             [:div.timeslip
              [:p.lead
               project-html
               client-html]
              [:p task-html [:em description]]]]
            [:td
             [:div.col-md-6.col-lg-8.col-sm-12
              [:div.col-sm-8
                [timeslip-duration timeslip clock]]
              [:div.col-sm-4
               [toggle-button timeslip]]]
             [:div.col-md-6.col-lg-4.col-sm-12
              [:button.btn.btn-default.col-sm-12
               {:on-click #(reset! editing true)}
               [:span.glyphicon.glyphicon-pencil
                {:aria-hidden "true"}]
               [:span {:class "sr-only"} "Edit Timeslip"]]]]])))))

(defn timeslips-table []
  (let [timeslips (subscribe [:active-day-timeslips])
        clock (subscribe [:clock])]
    (fn []
      (let [label-task ""
            label-time ""
            clock @clock]
        [:div.row
         [:table.table.table-hover
          [:thead
           [:tr
            [:th.col-sm-8 label-task]
            [:th.col-sm-4 label-time]]]
          [:tbody
           (for [timeslip @timeslips]
             ^{:key (str "timeslip-row-" (:id timeslip))}
             [timeslip-row timeslip clock]
             )]]]))))

(defn debug-db []
  (let [db (subscribe [:db])]
    (fn []
      [:div.row.debug-db
       [:pre
        [:code.clojure
         (with-out-str (pprint @db))]]])))

(defn today [env]
  [:div.container
   [head-row]
   [timeslips-table]
   (when (= "dev" env)
     [debug-db])])
