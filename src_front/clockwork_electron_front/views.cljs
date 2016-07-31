(ns clockwork-electron-front.views
  (:require [reagent.core :as r]
            [cljs.nodejs :as nodejs]
            [clojure.string :as s]
            [clockwork-electron-front.timeslips :as t]
            [clockwork-electron-front.db :refer [subscribe]]
            [cljs-time.core :as time]
            [clockwork-electron-front.utils :as u]))

(def Electron (nodejs/require "electron"))
(def remote (.-remote Electron))
(def dialog (.-dialog remote))

(defn new-timeslip-form []
  (let [val (r/atom "")
        new-timeslip (fn [] {:description @val :task "" :client ""
                             :active true :edit? false
                             :duration 0 :updated-at (t/read-clock)})
        change #(do
                  (println (str "setting value to: " (u/event-val %)))
                  (reset! val (u/event-val %)))
        save #(let [timeslip (new-timeslip)]
                (println (str "Calling add-timeslip with " timeslip))
                (.preventDefault %)
                (reset! val "")
                (t/add-timeslip timeslip))]
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
  (let [date (subscribe [:active-day]); (t/get-active-day) ; (subscribe [:active-day]); 
        weekday (nth u/weekdays (- (time/day-of-week @date) 1))
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
         :on-click #(t/goto-previous-day)}
        [:span.glyphicon.glyphicon-backward {:aria-hidden true}]]
       [:button.btn.btn-default
        {:type "button"
         :on-click #(t/set-active-day (time/today))}
        [:span "Today"]]
       [:button.btn.btn-default
        {:type "button"
         :on-click #(t/goto-next-day)}
        [:span.glyphicon.glyphicon-forward {:aria-hidden true}]]]]
     [:div.col-sm-8
      [new-timeslip-form]]]))

(defn toggle-button [id active?]
  (let [icon-label (if active? "Stop" "Start")
        icon-class (if active? "glyphicon-pause" "glyphicon-play")]
    [:button.btn.btn-default
     {:type "button"
      :on-click #(t/set-timeslip-state id (not active?))}
     [:span {:class (str "glyphicon " icon-class)
             :aria-hidden "true"}]
     [:span {:class "sr-only"} icon-label]] ))

(defn delete-warning-dialog [id]
  (let [options {:type "question"
                 :buttons ["Cancel" "Remove"]
                 :defaultId 0
                 :title "Confirm Timeslip Deletion"
                 :message "Delete this timeslip?"
                 :detail "Are you sure? This action cannot be undone"
                 }
        action (.showMessageBox dialog (clj->js options))]
    (if (= action 1)
      (t/remove-timeslip id))))

(defn remove-timeslip-button [id]
  [:button.btn.btn-danger.col-sm-12
   {:on-click #(delete-warning-dialog id)}
   [:span.glyphicon.glyphicon-remove
    {:aria-hidden "true"}]
   [:span {:class "sr-only"} "Remove Timeslip"]])

(defn edit-timeslip-button [id]
  [:button.btn.btn-default.col-sm-12
   {:on-click #(t/edit-timeslip id)}
   [:span.glyphicon.glyphicon-pencil
    {:aria-hidden "true"}]
   [:span {:class "sr-only"} "Edit Timeslip"]])

(defn unedit-timeslip-button [id]
  [:button.btn.btn-default.col-sm-12
   {:on-click #(t/unedit-timeslip id)}
   [:span.glyphicon.glyphicon-save
    {:aria-hidden "true"}]
   [:span {:class "sr-only"} "Save Timeslip"]])

(defn timeslip-description [timeslip]
  (let [{:keys [id project client task description]} timeslip
        project-html (if-not (s/blank? project) [:strong project])
        client-html (if-not (s/blank? client) (str " (" client ")"))
        task-html (if-not (s/blank? task) (str task " - "))]
    [:div.timeslip
     [:p.lead
      project-html
      client-html]
     [:p
      task-html [:em description]]]))

(defn text-input [id attribute val]
  [:div.form-group
   [:label.col-sm-2.control-label
    (s/capitalize (name attribute))]
   [:div.col-sm-10
    [:input.form-control
     {:type "text"
      :value val
      :on-change #(t/update-timeslip id attribute (u/event-val %))}]]])

(defn text-area [id attribute val]
  [:textarea.form-control
   {:value val
    :on-change #(t/update-timeslip id attribute (u/event-val %))}])

(defn timeslip-form [timeslip]
  (let [{:keys [id project client task description]} timeslip]
    [:form.form-horizontal
     [text-input id :client client]
     [text-input id :project project]
     [text-input id :task task]
     [text-area id :description description]]))

(defn timeslip-row [timeslip]
  (let [{:keys [id active edit?]} timeslip
        duration (t/get-timeslip-duration id)
        formatted-duration (u/format-time duration true)]
    [:tr
     [:td {:id (str "timeslip-" id)}
      (if edit?
        [timeslip-form timeslip]
        [timeslip-description timeslip]
        )]
     [:td
      [:div.col-md-6.col-lg-8.col-sm-12
       [:div.col-sm-8
        (if (and edit? (not active))
          [:input {:type "text"
                   :placeholder (u/format-time duration)
                   :on-change #(t/set-timeslip-clock-input id (u/event-val %))}]
          [:p.lead
           [:strong formatted-duration]] )]
       [:div.col-sm-4
          [toggle-button id active]]]
      [:div.col-md-6.col-lg-4.col-sm-12
       (if edit?
         [:div
           [unedit-timeslip-button id]
           [remove-timeslip-button id]]
         [edit-timeslip-button id])]]]))

(defn timeslips-table []
  (let [timeslips (r/track t/active-day-timeslips)
        label-task ""
        label-time ""]
    [:div.row
     [:table.table.table-hover
      [:thead
       [:tr
        [:th.col-sm-8 label-task]
        [:th.col-sm-4 label-time]]]
      [:tbody
       (for [timeslip @timeslips]
         ^{:key (str "timeslip-row-" (:id timeslip))}
         [timeslip-row timeslip]
         )]]]))

(defn today []
  [:div.container
   [head-row]
   [timeslips-table]])
