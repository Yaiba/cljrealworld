(ns frontend.views.home
  (:require [frontend.views.articles :as articles-view]
            [frontend.views.components :as c]))

(defn render-ui 
  [articles tags feed-type current-user current-page-num articles-count loading?]  
  (let [page-num (or current-page-num 0)
        total-pages (Math/ceil (/ (or articles-count 0) 10))]
    [:div.min-h-screen
     ;; navbar
     (c/navbar current-user)

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
       (if loading?
         [:div.flex.justify-center.py-8 [:span.loading.loading-spinner.loading-lg]]
         (articles-view/render-ui articles))

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
            (c/tag-pill tag))]]]]]]))