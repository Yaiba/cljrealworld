(ns frontend.views.ui
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]
            [frontend.views.home :as home]
            [frontend.views.login :as login]
            [frontend.views.register :as register]
            [frontend.views.settings :as settings]))

(defn render-ui [state]
  (let [ui-state (qs/get-entity state UI-ENTITY)
        page (:app/page ui-state)]
    (case page
      :page/home (#'home/render-ui (qs/query-articles state)
                                   (qs/query-tags state)
                                   (:app/feed-type ui-state)
                                   (qs/query-current-user state)
                                   (:app/current-page-num ui-state)
                                   (:app/articles-count ui-state))
      :page/login (#'login/render-ui (:app/email ui-state)
                                     (:app/password ui-state)
                                     (:app/errors ui-state))
      :page/register (#'register/render-ui (:app/username ui-state)
                                           (:app/email ui-state)
                                           (:app/password ui-state)
                                           (:app/errors ui-state))
      :page/settings (#'settings/render-ui ui-state
                                           (:app/errors ui-state))
      [:div "Loading..."])))