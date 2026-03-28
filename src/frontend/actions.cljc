(ns frontend.actions
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]))

(def actions
  {:app/navigate
   (fn [_state page]
     [[:route/push-state page]])

   :app/set-email
   (fn [_state email]
     [[:state/transact [{:db/id UI-ENTITY :app/email email}]]])

   :app/set-password
   (fn [_state password]
     [[:state/transact [{:db/id UI-ENTITY :app/password password}]]])

   :app/fetch-articles
   (fn [_state]
     [[:http/request {:method "GET"
                      :url "/api/articles"
                      :on-success :app/fetch-articles-success}]])

   :app/login
   (fn [state]
     (let [ui-state (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "POST"
                        :url "/api/users/login"
                        :body {:user
                               {:email (:app/email ui-state)
                                :password (:app/password ui-state)}}
                        :on-success :app/login-success}]]))})