(ns frontend.views.notfound)

(defn render-ui []
  [:div.flex.justify-center.items-center.min-h-screen
   [:div.text-center
    [:h1.text-6xl.font-bold "404"]
    [:p.text-gray-500.mt-2 "Page not found"]
    [:a.btn.btn-primary.mt-6 {:href "/"} "Go home"]]])