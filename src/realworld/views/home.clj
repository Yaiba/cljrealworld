(ns realworld.views.home
  (:require [realworld.db.articles :as db.a]
            [hiccup2.core :as hi]))

(defn home-page
  [articles]
  [:ul
   (for [article articles]
     [:li
      [:div
       [:h2 (:title article)]
       [:h3 (:description article)]
       [:p (:body article)]]])])