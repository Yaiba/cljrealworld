(ns realworld.system
  (:require [integrant.core :as ig]
            [next.jdbc.connection :as jdbc.conn]
            [org.httpkit.server :as http]
            [realworld.server :as server]
            [aero.core :as aero]
            [clojure.java.io :as io])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

(defn config []
  (let [{:keys [secret database-url port]} 
        (aero/read-config (io/resource "config.edn"))]
    {:app/secret secret
     :db/pool {:jdbc-url database-url}
     :router/core {:db (ig/ref :db/pool)
                   :secret (ig/ref :app/secret)}
     :server/http {:port port
                    :handler (ig/ref :router/core)}}))

(defmethod ig/init-key :app/secret [_ v] v)

(defmethod ig/init-key :db/pool
  [_ {:keys [jdbc-url]}]
  (println "Initializing database connection pool with JDBC URL:" jdbc-url)
  (let [
        hc (doto (com.zaxxer.hikari.HikariConfig.)
             (.setJdbcUrl jdbc-url))]
    (com.zaxxer.hikari.HikariDataSource. hc)))
  ;; (println "Initializing database connection pool with options:" opts)
  ;; (jdbc.conn/->pool HikariDataSource opts)


(defmethod ig/halt-key! :db/pool
  [_ datasource] 
  (println "Closing database connection pool")
  (.close datasource))

(defmethod ig/init-key :router/core
  [_ {:keys [db secret]}]
  (println "Initializing router with DB connection and secret")
  (server/create-app db secret))

(defmethod ig/init-key :server/http
  [_ {:keys [port handler]}]
  (println "Starting HTTP server on:" port)
  (http/run-server handler {:port port}))

(defmethod ig/halt-key! :server/http
  [_ server-instance]
  (println "Stopping HTTP server")
  (when server-instance
    (server-instance :timeout 100)))