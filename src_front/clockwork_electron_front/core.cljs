(ns clockwork-electron-front.core
  (:require  [reagent.core :as r]
             [clockwork-electron-front.navigation :as navigation]
             [clockwork-electron-front.views :as views]
             [clockwork-electron-front.timeslips :as timeslips]))

(defn root-component []
  [:div {:class "page"}
   [navigation/main]
   [views/today]])

(defn mount-root [setting]
  (r/render [root-component]
            (.getElementById js/document "app")))

(defn init! [setting]
  (mount-root setting))
