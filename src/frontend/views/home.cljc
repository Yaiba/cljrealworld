(ns frontend.views.home)

(defn render-ui [articles]
  [:div
   [:h1 "Home"]
   [:ul
    (for [article articles]
      [:li {:key (:article/slug article)}
       [:h2 (:article/title article)]
       [:p (:article/description article)]
       [:small "by " (get-in article [:article/author :user/username])]])]
   [:button {:on {:click [:app/navigate :page/login]}} "Go to login"]])