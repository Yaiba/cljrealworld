(ns frontend.views.article 
  (:require
    [frontend.views.components :as c]))

(defn render-ui [article current-user comments new-comment]
  [:div.min-h-screen
   (c/navbar current-user)
   [:div.container.mx-auto.px-4.py-8.max-w-3xl
    [:h1.text-4xl.font-bold.mb-4 (:article/title article)
     (when (= (get-in article [:article/author :user/username])
              (:user/username current-user))
       [:a.btn.btn-outline.btn-sm
        {:on {:click [[:editor/open (:article/slug article)]]}}
        "Edit"])]
    [:div.text-sm.text-base-content.opacity-60.mb-6
     "by "
     [:span {:on
             {:click [[:app/navigate :page/profile {:username (get-in article [:article/author :user/username])}]]}}
      (get-in article [:article/author :user/username])]
     (let [favorited? (:article/favorited? article)
           slug (:article/slug article)]
       [:button.btn.btn-sm
        {:on {:click [(if favorited?
                        [:article/unfavorite slug]
                        [:article/favorite slug])]}}
        (if favorited? "Unfavorite" "Favorite")
        [:span.ml-1 "(" (:article/favorites-count article) ")"]])]
    [:div.prose.max-w-none
     {:innerHTML (:article/body-html article)}]
    [:div.mt-8
     ;; comment form (logged-in only)
     (when current-user
       [:div.mb-4
        [:textarea.textarea.textarea-bordered.w-full
         {:value new-comment  ; or however you track new comment input
          :on {:input [[:comment/set-body [:event.target/value]]]}}]
        [:button.btn.btn-primary
         {:on {:click [[:comment/post (:article/slug article) new-comment]]}}
         "Post Comment"]])
     ;; comment list
     (for [comment comments]
       (c/comment-card comment current-user (:article/slug article)))]]])
