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
