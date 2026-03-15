(ns realworld.system
  (:require [integrant.core :as ig]
            [next.jdbc.connection :as jdbc.conn]
            [org.httpkit.server :as http]
            [realworld.server :as server]
            [realworld.db.connection :refer [db-spec]])
  (:import [com.zaxxer.hikari HikariDataSource]))

(def config 
  {:app/secret "my-secret-key" ; later read from env
   :db/pool db-spec
   :router/core {:db (ig/ref :db/pool)
                 :secret (ig/ref :app/secret)}
   :server/http {:port 3000
                 :handler (ig/ref :router/core)}})

(defmethod ig/init-key :app/secret [_ v] v)

(defmethod ig/init-key :db/pool
  [_ opts]
  (println "Initializing database connection pool with options:" opts)
  (jdbc.conn/->pool HikariDataSource opts))

(defmethod ig/halt-key! :db/pool
  [_ datasource] 
  (println "Closing database connection pool")
  (.close datasource))

(defmethod ig/init-key :router/core
  [_ {:keys [db secret]}]
  (println "Initializing router with DB connection and secret")
  (server/create-app secret))

(defmethod ig/init-key :server/http
  [_ {:keys [port handler]}]
  (println "Starting HTTP server on:" port)
  (http/run-server handler {:port port}))

(defmethod ig/halt-key! :server/http
  [_ server-instance]
  (println "Stopping HTTP server")
  (when server-instance
    (server-instance :timeout 100)))