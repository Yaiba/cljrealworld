(ns realworld.db.tags
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defn list-all [ds]
  (->> (jdbc/execute! ds (sql/format {:select [:name]
                                      :from   [:tags]
                                      :order-by [:name]}))
       (map :tags/name)))
