(ns realworld.views.layout
  (:require [hiccup2.core :as h]
            [hiccup.page :as hp]))

(def ^:private datastar-cdn
  "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.8/bundles/datastar.js")

(defn page
  [title & body]
  (hp/html5
   [:head
    [:title title]
    [:script {:type "module" :src datastar-cdn}]] 
   (into [:body] body)))