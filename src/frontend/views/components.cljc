(ns frontend.views.components)

(defn error-banner [errors]
  (when errors
    [:div.alert.alert-error
     [:ul
      (for [[field msgs] errors
            msg msgs]
        [:li {:replicant/key (str field msg)} (str (name field) " " msg)])]]))

(defn article-card [article]
  [:div.card.bg-base-100.shadow.mb-4.cursor-pointer
   {:replicant/key (:article/slug article)
    :on {:click [[:article/view (:article/slug article)]]}}
   [:div.card-body
    [:h2.card-title (:article/title article)]
    [:p (:article/description article)]
    [:div.text-sm.text-base-content.opacity-60
     "by "
     [:span {:on {:click [[:effect/stop-event-propagation]
                          [:app/navigate :page/profile {:username (get-in article [:article/author :user/username])}]]}}
      (get-in article [:article/author :user/username])]]]])

(defn navbar
  ([] (navbar nil))
  ([current-user]
   [:div.navbar.bg-base-100.shadow
    [:div.navbar-start
     [:a.btn.btn-ghost.text-xl {:on {:click [[:app/navigate :page/home]]}} "Realworld"]]
    [:div.navbar-end
     (if current-user
       [:div.flex.gap-2
        [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/editor]]}} "New Article"]
        [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/settings]]}} "Settings"]
        [:span.btn.btn-ghost (:user/username current-user)]]
       [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/login]]}} "Sign in"])]]))

(defn comment-card [comment current-user article-slug]
  [:div.card.bg-base-200.mb-3 
   {:replicant/key (:comment/id comment)}
   [:div.card-body.p-4
    [:p (:comment/body comment)]
    [:div.flex.justify-between.items-center.mt-2
     [:span.text-sm "- " (get-in comment [:comment/author :user/username])]
     (when (= (get-in comment [:comment/author :user/username])
              (:user/username current-user))
       [:button.btn.btn-ghost.btn-xs
        {:on {:click [[:comment/delete article-slug (:comment/id comment)]]}}
        "Delete"])]]])

(defn tag-pill [tag]
  [:span.badge.badge-neutral.cursor-pointer
   {:replicant/key  tag
    :on {:click [[:feed/set-tag tag]]}}
   tag])
