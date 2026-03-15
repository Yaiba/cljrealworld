(ns realworld.db.users
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defn find-by-email [ds email]
  (jdbc/execute-one! ds
                     (sql/format {:select [:*]
                                  :from   [:users]
                                  :where  [:= :email email]})))

(defn find-by-username [ds username]
  (jdbc/execute-one! ds
                     (sql/format {:select [:*]
                                  :from   [:users]
                                  :where  [:= :username username]})))

(defn find-by-id [ds id]
  (jdbc/execute-one! ds
                     (sql/format {:select [:*]
                                  :from   [:users]
                                  :where  [:= :id id]})))

(defn create! [ds user]
  (jdbc/execute-one! ds
                     (sql/format {:insert-into :users
                                  :values      [user]
                                  :returning   [:*]})))

(defn update! [ds id fields]
  (jdbc/execute-one! ds
                     (sql/format {:update :users
                                  :set    fields
                                  :where  [:= :id id]
                                  :returning [:*]})))
