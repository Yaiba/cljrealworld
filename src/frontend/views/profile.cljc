(ns frontend.views.profile
  (:require [frontend.views.articles :as articles-view]))

(defn render-ui [profile active-tab articles]
  (let [{username :user/username 
         image :user/image 
         bio :user/bio 
         following? :user/following?} profile]
    [:div.min-h-screen.w-full.flex.items-center.justify-center
     [:div.profile-page
      [:div.user-info
       [:div.container
        [:div.row
         [:div.col-xs-12.col-md-10.offset-md-1
          [:img.user-img {:src image}]
          [:h4 username]
          (when-not (nil? bio)
            [:p bio])
          (when-not (nil? following?)
            [:button.btn.btn-sm.btn-outline-secondary.action-btn
             {:on {:click [(if following?
                             [:profile/unfollow username]
                             [:profile/follow username])]}}
             (if following? "Unfollow" "Follow")])]]]]]
     [:div.tabs.tabs-bordered.mb-4
      [:a.tab {:class (when (= active-tab :my-articles) "tab-active")
               :on {:click [[:profile/set-tab :my-articles]
                            [:profile/fetch-articles username :my-articles]]}} "Articles"]
      [:a.tab {:class (when (= active-tab :favorited) "tab-active")
               :on {:click [[:profile/set-tab :favorited]
                            [:profile/fetch-articles username :favorited]]}} "Favorited Articles"]]
     
     [:div.container.mx-auto.px-4.py-6.flex.gap-6
      [:div.flex-1
       (articles-view/render-ui articles)]]]))