(ns dev
  (:require [dataspex.core :as dataspex]
            [frontend.core :refer [conn]]
            [frontend.queries :as qs]
            [datascript.core :as d]))

(defn setup []
  (dataspex/inspect "App state" conn))

(comment
  (qs/query-tags (d/db conn))
  (setup)
  (qs/query-current-user (d/db conn))
  (qs/get-entity (d/db conn) 1)
  (qs/get-entity (d/db conn) 3)
  (d/transact! conn [{:db/id 1 :app/current-page-num 2}])
  )