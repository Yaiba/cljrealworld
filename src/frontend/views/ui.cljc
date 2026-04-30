(ns frontend.views.ui
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]
            [frontend.views.home :as home]
            [frontend.views.login :as login]
            [frontend.views.register :as register]
            [frontend.views.settings :as settings]
            [frontend.views.editor :as editor]
            [frontend.views.article :as article]
            [frontend.views.profile :as profile]
            [frontend.views.notfound :as notfound]))

(defn render-ui [state]
  (let [ui-state (qs/get-entity state UI-ENTITY)]
    (case (:app/page ui-state)
      :page/home (home/render-ui (qs/query-articles state)
                                   (qs/query-tags state)
                                   (:app/feed-type ui-state)
                                   (qs/query-current-user state)
                                   (:app/current-page-num ui-state)
                                   (:app/articles-count ui-state)
                                   (:app/loading-articles? ui-state))
      :page/login (login/render-ui (:app/email ui-state)
                                     (:app/password ui-state)
                                     (:app/errors ui-state))
      :page/register (register/render-ui (:app/username ui-state)
                                           (:app/email ui-state)
                                           (:app/password ui-state)
                                           (:app/errors ui-state))
      :page/settings (settings/render-ui ui-state
                                           (:app/errors ui-state))
      :page/editor (editor/render-ui                     ui-state
                                                           (:app/errors ui-state)
                                                           (qs/query-current-user state))
      :page/editor-edit (editor/render-ui
                         ui-state
                         (:app/errors ui-state)
                         (qs/query-current-user state))
      :page/article (article/render-ui (qs/query-current-article state)
                                         (qs/query-current-user state)
                                         (qs/query-article-comments state (:app/current-article-slug ui-state))
                                         (:comment/new-body ui-state))
      :page/profile (profile/render-ui (qs/query-current-profile state)
                                         (:profile/active-tab ui-state)
                                         (qs/query-articles state))
      :page/not-found (notfound/render-ui)


      [:div "Loading..."])))