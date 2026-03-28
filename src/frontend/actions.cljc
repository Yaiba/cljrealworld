(ns frontend.actions
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]
            [datascript.core :as d]
            [frontend.utils :refer [remove-nil-values]]))

(defn articles-url [state page]
  (let [ui (qs/get-entity state UI-ENTITY)
        feed (:app/feed-type ui)
        tag (:app/feed-type ui)
        offset (* (or page 0) 10)]
    (cond
      (= feed :personal) (str "/api/articles/feed?limit=10&offset=" offset)
      tag (str "/api/articles?tag=" tag "&limit=10&offset=" offset)
      :else (str "/api/articles?limit=10&offset=" offset))))

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
   (fn [state]
     [[:http/request {:method "GET"
                      :url (articles-url state 0)
                      :on-success :app/fetch-articles-success}]])

   :app/login
   (fn [state]
     (let [ui-state (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "POST"
                        :url "/api/users/login"
                        :body {:user
                               {:email (:app/email ui-state)
                                :password (:app/password ui-state)}}
                        :on-success :app/login-success
                        :on-failure :app/login-failure}]]))

   :app/fetch-tags
   (fn [_state]
     [[:http/request {:method "GET"
                      :url "/api/tags"
                      :on-success :app/fetch-tags-success}]])

   :feed/set-tag
   (fn [_state tag]
     [[:state/transact [{:db/id UI-ENTITY :app/active-tag tag}]]
      [:http/request {:method "GET"
                      :url (str "/api/articles?tag=" tag)
                      ;; re-fetch articles filtered by that tag
                      :on-success :app/fetch-articles-success}]])

   :feed/set-type
   (fn [state feed-type]
     (let [new-state (d/db-with state [{:db/id UI-ENTITY
                                        :app/feed-type feed-type
                                        :app/active-tag nil
                                        :app/current-page-num 0}])]
       [[:state/transact [{:db/id UI-ENTITY :app/feed-type feed-type :app/current-page-num 0}]]
        [:http/request {:method "GET"
                        :url (articles-url new-state 0)
                        :on-success :app/fetch-articles-success}]]))

   :feed/set-page-num
   (fn [state page]
     [[:state/transact [{:db/id UI-ENTITY :app/current-page-num page}]]
      [:http/request {:method "GET"
                      :url (articles-url state page)
                      :on-success :app/fetch-articles-success}]])

   :app/set-username
   (fn [_state username]
     [[:state/transact [{:db/id UI-ENTITY :app/username username}]]])

   :app/register
   (fn [state]
     (let [ui (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "POST"
                        :url "/api/users"
                        :body {:user {:username (:app/username ui)
                                      :email (:app/email ui)
                                      :password (:app/password ui)}}
                        :on-success :app/login-success
                        :on-failure :app/login-failure}]]))

   :app/fetch-current-user
   (fn [_state]
     [[:http/request {:method "GET"
                      :url "/api/user"
                      :on-success :app/fetch-current-user-success}]])

   :settings/init
   (fn [state]
     (let [user (qs/query-current-user state)]
       (prn "settings/init user=" user)
       [[:state/transact [(remove-nil-values {:db/id UI-ENTITY
                                              :settings/username (:user/username user)
                                              :settings/email (:user/email user)
                                              :settings/bio (:user/bio user)
                                              :settings/image (:user/image user)})]]]))

   :settings/set-field
   (fn [_state field value]
     [[:state/transact [{:db/id UI-ENTITY
                         (keyword "settings" (name field)) value}]]])
   
   :user/update
   (fn [state]
     (let [ui (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "PUT"
                        :url "/api/user"
                        :body {:user (cond-> {:username (:settings/username ui)
                                              :email (:settings/email ui)
                                              :bio (:settings/bio ui)
                                              :image (:settings/image ui)}
                                       (seq (:settings/password ui))
                                       (assoc :password (:settings/password ui)))}
                        :on-success :user/update-success
                        :on-failure :user/login-failure}]]))
   
   :user/logout
   (fn [_state]
     [[:user/logout]])
   ;;
   })