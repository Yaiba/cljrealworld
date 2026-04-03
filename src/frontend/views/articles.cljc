(ns frontend.views.articles)

(defn render-ui [articles]
  (for [article articles]
    [:div.card.bg-base-100.shadow.mb-4.cursor-pointer
     {:key (:article/slug article)
      :on {:click [[:article/view (:article/slug article)]]}}
     [:div.card-body
      [:h2.card-title (:article/title article)]
      [:p (:article/description article)]
      [:div.text-sm.text-base-content.opacity-60
       "by "
       (let [username (get-in article [:article/author :user/username])]
         [:span {:on
                 {:click [[:effect/stop-event-propagation]
                          [:app/navigate :page/profile {:username username}]]}}
          username])]]]))