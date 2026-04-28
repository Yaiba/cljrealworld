(ns realworld.main
  (:require [integrant.core :as ig]
            [realworld.system :refer [config]])
  (:gen-class))

(defn -main
  "The main entry point for the application."
  [& args]
  (println "Starting RealWorld application...")
  (ig/init (config)))