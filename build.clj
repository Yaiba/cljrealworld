(ns build
  (:require [clojure.tools.build.api :as b]))

(def basis (delay (b/create-basis {:aliases [:backend]})))

(defn uber [_]
  (b/copy-dir {:src-dirs ["src/realworld" "resources"]
               :target-dir "target/classes"})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src/realworld"]
                  :class-dir "target/classes"})
  (b/uber {:class-dir "target/classes"
           :uber-file "target/app.jar"
           :basis @basis
           :main 'realworld.main}))