(ns clockwork-electron-front.utils)

;; generic javascript events helpers
(defn event-val [event]
  (-> event .-target .-value))

;; time stuff
(def weekdays ["Monday" "Tuesday" "Wednesday"
               "Thursday" "Friday" "Saturday" "Sunday"])
(def months ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
             "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn format-time [total & [with-seconds]]
  (let [hours (quot total 3600)
        minutes (quot (mod total 3600) 60)
        seconds (mod total 60)
        hours-string (if (< hours 10) (str "0" hours) (str hours))
        minutes-string (if (< minutes 10) (str "0" minutes) (str minutes))
        seconds-string (if (< seconds 10) (str "0" seconds) (str seconds))]
    (str hours-string ":" minutes-string (if with-seconds (str ":" seconds-string)))))

(defn get-timestamp [clock]
  (let [now (.getTime clock)]
    (quot now 1000)))

(defn parse-duration-string [duration]
  (condp re-matches duration
    #"(\d+):(\d\d)"    :>> #(let [hours (nth % 1)
                                  minutes (nth % 2)]
                              (+ (* 3600 hours)
                                 (* 60 minutes)))
    #"(\d+)"           :>> #(js/Number. duration)
    #"(\d+)h"          :>> #(* 3600 %)
    #"(\d+)m"          :>> #(* 60 %)
    #"(\d+)h\s*(\d+)m" :>> #(let [hours (nth % 1)
                                  minutes (nth % 2)]
                              (+ (* 3600 hours)
                                 (* 60 minutes)))
    false))

(defn seconds-between [a b]
  (if a
    (- b a)
    0))
