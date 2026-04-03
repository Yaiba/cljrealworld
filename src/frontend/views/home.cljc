(ns frontend.views.home
  (:require [frontend.views.articles :as articles-view]))

(defn render-ui 
  [articles tags feed-type current-user current-page-num articles-count]  
  (let [page-num (or current-page-num 0)
        total-pages (Math/ceil (/ (or articles-count 0) 10))]
    [:div.min-h-screen
     ;; navbar
     [:div.navbar.bg-base-100.shadow
      [:div.navbar-start
       [:a.btn.btn-ghost.text-xl "Realworld"]]
      [:div.navbar-end
       (if current-user
         [:div.flex.gap-2
          [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/editor]]}} "New Article"]
          [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/settings]]}} "Settings"]
          [:span.btn.btn-ghost (:user/username current-user)]]
         [:a.btn.btn-ghost {:on {:click [[:app/navigate :page/login]]}} "Sign in"])]]

     ;; Hero banner
     [:div.hero.bg-neutral.text-neutral-content.py-8
      [:div.hero-content.text-center
       [:h1.text-4xl.font-bold "Realworld"]
       [:p "A nice place"]]]

     ;; Main content
     [:div.container.mx-auto.px-4.py-6.flex.gap-6
      ;; Article feed
      [:div.flex-1
       [:div.tabs.tabs-bordered.mb-4
        [:a.tab {:class (when (not= feed-type :personal) "tab-active")
                 :on {:click [[:feed/set-type :global]]}} "Global Feed"]
        (when current-user
          [:a.tab {:class (when (= feed-type :personal) "tab-active")
                   :on {:click [[:feed/set-type :personal]]}} "Your Feed"])]
        (articles-view/render-ui articles)]

       ;;pagination
       [:div.flex.gap-2.mt-4
        (for [n (range total-pages)]
          [:button.btn.btn-sm
           {:key n
            :class (when (= n page-num) "btn-active")
            :on {:click [[:feed/set-page n]]}}
           (inc n)])]]

      ;; Tag sidebar
      [:aside.w-64
       [:div.card.bg-base-200
        [:div.card-body
         [:h3.card-title.text-sm "Popular Tags"]
         [:div.flex.flex-wrap.gap-2
          (for [tag tags]
            [:span.badge.badge-neutral.cursor-pointer
             {:key tag
              :on {:click [[:feed/set-tag tag]]}}
             tag])]]]]]]))