(ns clockwork-electron-front.core
  (:require  [reagent.core :as reagent]
             [re-frame.core :refer [dispatch dispatch-sync]]
             [clockwork-electron-front.db]
             [clockwork-electron-front.events]
             [clockwork-electron-front.subs]
             ; [clockwork-electron-front.events]
             [clockwork-electron-front.navigation :as navigation]
             [clockwork-electron-front.timeslips :as timeslips]
             [clockwork-electron-front.views :as views]
             ))

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
