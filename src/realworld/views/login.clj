(ns realworld.views.login
  (:require [hiccup2.core :as hi]
            [realworld.views.layout :as layout]))

(defn login-page
  []
  (layout/page "Login"
               [:div#login-form 
                {:data-signals "{email: \"\", password: \"\"}"}
                [:form 
                 {:data-on:submit "@post(\"/login\")"}
                 [:span "email: " [:input {:data-bind "email" :value ""}]]
                 [:span "password: " [:input {:data-bind "password" :value ""}]]
                 [:button {:type "submit"} "Sign in"]]
                [:div#login-error ""]]))