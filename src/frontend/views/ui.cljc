(ns frontend.views.ui
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]
            [frontend.views.home :as home]
            [frontend.views.login :as login]))

(defn render-ui [state]
  (let [ui-state (qs/get-entity state UI-ENTITY)
        page (:app/page ui-state)]
    (case page
      :page/home (#'home/render-ui (qs/query-articles state))
      :page/login (#'login/render-ui (:app/email ui-state)
                                     (:app/password ui-state))
      [:div "Loading..."])))