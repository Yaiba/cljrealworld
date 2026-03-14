(ns user
  (:require [portal.api :as p]
            [realworld.server :as server]))

(def portal (p/open))
(add-tap #'p/submit)

(defn start
  [] 
  (server/start-server!))

(defn stop
  [] 
  (server/stop-server!))

(defn restart
  []
  (stop)
  (start))