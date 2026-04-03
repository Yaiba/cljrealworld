(ns frontend.queries
  (:require [datascript.core :as d]
            [frontend.constants :refer [UI-ENTITY]]))

(defn get-entity
  [state id]
  (d/pull state '[*] id))

(defn query-articles
  [state]
  (d/q '[:find [(pull ?a [:article/slug :article/description :article/title :article/tags {:article/author [:user/username]}]) ...]
         :where [?a :article/slug _]] state))

(defn get-token [state]
  (get-in (d/pull state '[{:app/current-user [:user/token]}] UI-ENTITY)
          [:app/current-user :user/token]))

(defn query-tags
  [state]
  (:app/tags (d/pull state '[:app/tags] UI-ENTITY)))

(defn query-current-user
  [state]
  (get-in (d/pull
           state
           '[{:app/current-user [:user/username :user/image :user/bio :user/email]}] UI-ENTITY)
          [:app/current-user]))

(defn query-current-profile
  [state]
  (get-in (d/pull
           state
           '[{:app/current-profile [:user/username :user/image :user/bio :user/following?]}] UI-ENTITY)
          [:app/current-profile]))

(defn query-article-by-slug [state slug]
  (d/q '[:find (pull ?a [:article/slug :article/title :article/description
                         :article/body :article/body-html :article/tags 
                         :article/favorited? :article/favorites-count
                         {:article/author [:user/username :user/image]}]) .
         :in $ ?slug
         :where [?a :article/slug ?slug]]
       state slug))

(defn query-current-article [state]
  (let [slug (:app/current-article-slug (get-entity state UI-ENTITY))]
    (query-article-by-slug state slug)))

(defn query-article-comments [state slug]
  (get-in (d/q '[:find (pull ?a [{:article/comments 
                                  [:comment/id :comment/body
                                   {:comment/author [:user/username :user/image]}]}]) .
                 :in $ ?slug
                 :where [?a :article/slug ?slug]]
               state slug)
          [:article/comments]))
