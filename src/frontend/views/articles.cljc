(ns frontend.views.articles
  (:require [frontend.views.components :as c]))

(defn render-ui [articles]
  (for [article articles]
    (c/article-card article)))