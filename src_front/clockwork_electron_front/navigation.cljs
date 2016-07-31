(ns clockwork-electron-front.navigation)

(defn main []
  [:nav {:class "navbar navbar-default navbar-fixed-top"}
   [:div {:class "container"}
    [:div {:class "navbar-header"}
     [:button {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse"
               :data-target "#navbar" :aria-expanded "false" :aria-controls "navbar"}
      [:span {:class "sr-only"} "Toggle navigation"]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]]

     [:a {:class "navbar-brand" :href "#"} "ClockWork"]]
    [:div {:id "navbar" :class "navbar-collapse collapse"}
     [:ul {:class "nav navbar-nav"}
      [:li {:class "active"}
       [:a {:class "item" :href "#"} "My Work"]]
      [:li {:class "disabled"}
       [:a {:href "#"} "Projects"]]
      [:li {:class "disabled"}
       [:a {:href "#"} "Reports"]]
      [:li {:class "disabled"}
       [:a {:href "#"} "Manage"]]]
     [:ul {:class "nav navbar-nav navbar-right"}
      [:li {:class "disabled"}
       [:a {:href "#"} "Settings"]]]]]])
