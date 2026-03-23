(ns user
  (:require [portal.api :as p]
            [integrant.core :as ig]
            [realworld.system :refer [config]]
            [next.jdbc :as jdbc]))
;; 
;; (def portal (p/open))
;; (add-tap #'p/submit)

(defonce portal (atom nil))
(defonce system (atom nil))

(defn- start-portal
  []
  (reset! portal (p/open))
  (add-tap #'p/submit))

(defn- stop-portal
  []
  (when @portal
    (p/close @portal))
  (reset! portal nil))

(defn start
  [] 
  (reset! system (ig/init config)))

(defn stop
  []
  (when @system 
    (ig/halt! @system))
  (reset! system nil))

(defn reset
  []
  (stop)
  (start))

(defn app [] (:router/core @system))

(defn prune-db []
  (let [ds (:db/pool @system)]
    (jdbc/execute! ds ["DELETE from users"])
    (jdbc/execute! ds ["DELETE FROM tags"])))