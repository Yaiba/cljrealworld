(ns realworld.sse
  (:require [hiccup2.core :as hi]))


(defn merge-fragment
  [html]
  (str "event: datastar-patch-elements\n"
       "data: elements " html "\n\n"))

(defn redirect [url]
  (str "event: datastar-patch-elements\n"
       "data: selector body\n"
       "data: mode append\n"
       "data: elements <script>window.location = '" url "'</script>\n\n"))
