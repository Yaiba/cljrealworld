(ns dev
  (:require [dataspex.core :as dataspex]
            [frontend.core :refer [conn]]))

(defn setup []
  (dataspex/inspect "App state" conn))