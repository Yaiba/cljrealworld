(ns user
  (:require [portal.api :as p]
            [integrant.core :as ig]
            [realworld.system :refer [config]]))

(def portal (p/open))
(add-tap #'p/submit)

(defonce system (atom nil))

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