(ns clockwork.core
    (:require [cljs.nodejs :as nodejs]))

(def path (nodejs/require "path"))
(def Electron (nodejs/require "electron"))
(def app (.-app Electron))
(def BrowserWindow (.-BrowserWindow Electron))
#_ (def crash-reporter (.-crashReporter Electron))

(def *win* (atom nil))
(def darwin? (= (.-platform nodejs/process) "darwin"))

(defn create-window []
  (reset! *win* (BrowserWindow. (clj->js {:width 800 :height 600})))
  (.loadURL @*win* (str "file://" (.resolve path (js* "__dirname") "../index.html")))
  (.openDevTools (.-webContents @*win*))
  (.on app "closed" (fn [] (reset! *win* nil))))

(defn -main []
  ;; TODO setup crash reports server
  #_(.start crash-reporter (clj->js {:companyName "100Starlings"
                                   :submitURL   "http://100starlings.com/"}))

  ;; error listener
  (.on nodejs/process "error"
       (fn [err] (.log js/console err)))

  ;; window all closed listener
  (.on app "window-all-closed"
       (fn [] (when-not darwin? (.quit app))))

  ;; activate listener
  (.on app "activate"
       (fn [] (when darwin? (create-window))))

  ;; ready listener
  (.on app "ready" (fn [] (create-window)))
  #_ (.on app "ready"
       (fn []
         (reset! *win* (BrowserWindow. (clj->js {:width 800 :height 800
                                                 :minWidth 400 :minHeight 400 })))

         ;; when no optimize comment out
         (.loadURL @*win* (str "file://" (.resolve path (js* "__dirname") "../index.html")))
         ;; when no optimize uncomment
         ;; (.loadURL @*win* (str "file://" (.resolve path (js* "__dirname") "../../../index.html")))

         (.on @*win* "closed" (fn [] (reset! *win* nil))))))

(nodejs/enable-util-print!)
(.log js/console "Clockwork has started!")

(set! *main-cli-fn* -main)
