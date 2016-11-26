(ns clockwork-electron-front.core
  (:require  [reagent.core :as reagent]
             [re-frame.core :refer [dispatch dispatch-sync]]
             [clockwork-electron-front.db]
             [clockwork-electron-front.events]
             [clockwork-electron-front.subs]
             [clockwork-electron-front.navigation :as navigation]
             [clockwork-electron-front.views :as views]
             ))

(defonce timeslips-updater
  (js/setInterval #(dispatch [:update-clock]) 1000))

(defonce timeslips-saver
  (js/setInterval #(dispatch [:save-timeslips]) 10000))

(defn root-component []
  [:div {:class "page"}
   [navigation/main]
   [views/today]])

(defn mount-root [setting]
  (reagent/render [root-component]
            (.getElementById js/document "app")))

(defn init! [setting]
  (dispatch-sync [:initialise-db])
  (mount-root setting))
