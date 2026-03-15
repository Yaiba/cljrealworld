(ns realworld.db.connection
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as jdbc.conn])
  (:import [com.zaxxer.hikari HikariDataSource]))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   "realworld"
   :host     "localhost"
   :port     5432
   :username "realworld" ; for HikariCP
   :user     "realworld" ; for next.jdbc
   :password "password"})

(def migration-config
  {:store         :database
   :migration-dir "migrations"
   :db            db-spec})

;; (defonce datasource
;;   (jdbc.conn/->pool HikariDataSource db-spec))

(comment
  #_(migratus.core/migrate migration-config)
  )
