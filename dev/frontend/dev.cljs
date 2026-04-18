(ns dev
  (:require [dataspex.core :as dataspex]
            [frontend.core :refer [conn]]
            [frontend.queries :as qs]
            [datascript.core :as d]
            [reitit.frontend.easy :as rfe]))

(defn setup []
  (dataspex/inspect "App state" conn))

(comment
  (qs/query-tags (d/db conn))
  (setup)
  (qs/query-current-user (d/db conn))
  (qs/get-entity (d/db conn) 1)
  (qs/get-entity (d/db conn) 3)
  (d/transact! conn [{:db/id 1 :app/current-page-num 2}])
  (rfe/push-state :page/home)
  (rfe/push-state :page/settings)
  (d/transact! conn [{:db/id frontend.constants/UI-ENTITY :app/loading-articles? true}])
  (d/transact! conn [{:db/id frontend.constants/UI-ENTITY :app/loading-articles? false}])
  )