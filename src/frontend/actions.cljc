(ns frontend.actions
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]
            [datascript.core :as d]
            [clojure.string :as str]
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
   (fn [_state page & [path-params]]
     (prn "navigate to page=" page " with path-params=" path-params)
     [[:route/push-state page path-params]])

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
                      :on-success [:app/fetch-articles-success]}]])

   :app/login
   (fn [state]
     (let [ui-state (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "POST"
                        :url "/api/users/login"
                        :body {:user
                               {:email (:app/email ui-state)
                                :password (:app/password ui-state)}}
                        :on-success [:app/login-success]
                        :on-failure [:app/login-failure]}]]))

   :app/fetch-tags
   (fn [_state]
     [[:http/request {:method "GET"
                      :url "/api/tags"
                      :on-success [:app/fetch-tags-success]}]])

   :feed/set-tag
   (fn [_state tag]
     [[:state/transact [{:db/id UI-ENTITY :app/active-tag tag}]]
      [:http/request {:method "GET"
                      :url (str "/api/articles?tag=" tag)
                      :on-success [:app/fetch-articles-success]}]])

   :feed/set-type
   (fn [state feed-type]
     (let [new-state (d/db-with state [{:db/id UI-ENTITY
                                        :app/feed-type feed-type}])]
       [[:state/transact [{:db/id UI-ENTITY :app/feed-type feed-type}]]
        [:http/request {:method "GET"
                        :url (articles-url new-state 0)
                        :on-success [:app/fetch-articles-success]}]]))

   :feed/set-page-num
   (fn [state page]
     [[:state/transact [{:db/id UI-ENTITY :app/current-page-num page}]]
      [:http/request {:method "GET"
                      :url (articles-url state page)
                      :on-success [:app/fetch-articles-success]}]])

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
                        :on-success [:app/login-success]
                        :on-failure [:app/login-failure]}]]))

   :app/fetch-current-user
   (fn [_state]
     [[:http/request {:method "GET"
                      :url "/api/user"
                      :on-success [:app/fetch-current-user-success]}]])

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
                        :on-success [:user/update-success]
                        :on-failure [:user/login-failure]}]]))

   :user/logout
   (fn [_state]
     [[:user/logout]])

   :editor/set-field
   (fn [_state field value]
     [[:state/transact [{:db/id UI-ENTITY
                         (keyword "editor" (name field)) value}]]])

   :editor/add-tag
   (fn [state]
     (let [ui (qs/get-entity state UI-ENTITY)
           tag (some-> (:editor/tag-input ui) str/trim)
           current-tags (or (:editor/tags ui) #{})]
       (if (and tag (seq tag))
         [[:state/transact [{:db/id UI-ENTITY
                             :editor/tags (conj current-tags tag)
                             :editor/tag-input ""}]]]
         [])))

   :editor/remove-tag
   (fn [state tag]
     (let [ui (qs/get-entity state UI-ENTITY)
           current-tags (or (:editor/tags ui) #{})]
       [[:state/transact [{:db/id UI-ENTITY
                           :editor/tags (disj current-tags tag)}]]]))

   :editor/open
   (fn [_state slug]
     (prn "editor/open called with slug=" slug)
     [[:route/push-state :page/editor-edit {:slug slug}]])

   :editor/init
   (fn [state slug]
     (if slug
       (let [article (d/q '[:find (pull ?a [:article/title :article/description :article/body :article/tags :article/slug]) .
                            :in $ ?slug
                            :where [?a :article/slug ?slug]]
                          state slug)]
         [[:state/transact [(remove-nil-values {:db/id UI-ENTITY
                                                :editor/slug (:article/slug article)
                                                :editor/title (:article/title article)
                                                :editor/description (:article/description article)
                                                :editor/body (:article/body article)
                                                :editor/tags (:article/tags article)
                                                :editor/tag-input ""})]]])
       (let [current-slug (:editor/slug (qs/get-entity state UI-ENTITY))
             retractions  (when current-slug
                            [[:db/retract UI-ENTITY :editor/slug current-slug]])]
         [[:state/transact (concat retractions
                                   [{:db/id UI-ENTITY
                                     :editor/title ""
                                     :editor/description ""
                                     :editor/body ""
                                     :editor/tags #{}
                                     :editor/tag-input ""}])]])))

   :article/create
   (fn [state]
     (let [ui (qs/get-entity state UI-ENTITY)]
       [[:http/request {:method "POST"
                        :url "/api/articles"
                        :body {:article {:title (:editor/title ui)
                                         :description (:editor/description ui)
                                         :body (:editor/body ui)
                                         :tagList (vec (or (:editor/tags ui) []))}}
                        :on-success [:article/create-success]
                        :on-failure [:article/create-failure]}]]))

   :article/update
   (fn [state]
     (let [ui (qs/get-entity state UI-ENTITY)
           slug (:editor/slug ui)]
       [[:http/request {:method "PUT"
                        :url (str "/api/articles/" slug)
                        :body {:article {:title (:editor/title ui)
                                         :description (:editor/description ui)
                                         :body (:editor/body ui)
                                         :tagList (vec (or (:editor/tags ui) []))}}
                        :on-success [:article/update-success]
                        :on-failure [:article/create-failure]}]]))

   :article/fetch
   (fn [_state slug]
     [[:state/transact [{:db/id UI-ENTITY :app/current-article-slug slug}]]
      [:http/request {:method "GET"
                      :url (str "/api/articles/" slug)
                      :on-success [:article/fetch-success]}]])

   :article/fetch-comments
   (fn [_state slug]
     [[:http/request {:method "GET"
                      :url (str "/api/articles/" slug "/comments")
                      :on-success [:article/fetch-comments-success slug]}]])

   :article/view
   (fn [_state slug]
     [[:route/push-state :page/article {:slug slug}]])

   :article/favorite
   (fn [_state slug]
     (prn "favorite-" slug)
     [[:http/request {:method "POST"
                      :url (str "/api/articles/" slug "/favorite")
                      :on-success [:article/fetch-success]}]])

   :article/unfavorite
   (fn [_state slug]
     (prn "unfavorite-" slug)
     [[:http/request {:method "DELETE"
                      :url (str "/api/articles/" slug "/favorite")
                      :on-success [:article/fetch-success]}]])

   :profile/fetch
   (fn [_state username]
     [[:http/request {:method "GET"
                      :url (str "/api/profiles/" username)
                      :on-success [:profile/fetch-success]}]])

   :profile/follow
   (fn [_state username] 
     [[:http/request {:method "POST"
                      :url (str "/api/profiles/" username "/follow")
                      :on-success [:profile/fetch-success]}]])

   :profile/unfollow
   (fn [_state username] 
     [[:http/request {:method "DELETE"
                      :url (str "/api/profiles/" username "/follow")
                      :on-success [:profile/fetch-success]}]])

   :profile/fetch-articles
    (fn [_state username tab]
      (let [url (if (= tab :favorited)
                  (str "/api/articles?favorited=" username)
                  (str "/api/articles?author=" username))]
        [[:state/transact [{:db/id UI-ENTITY :profile/active-tab tab}]]
         [:http/request {:method "GET"
                         :url url
                         :on-success [:app/fetch-articles-success]}]]))
   
   :profile/set-tab
    (fn [_state tab]
      [[:state/transact [{:db/id UI-ENTITY :profile/active-tab tab}]]])
   
   :comment/set-body
   (fn [_state body]
     [[:state/transact [{:db/id UI-ENTITY :comment/new-body body}]]])

   :comment/post
   (fn [_state slug new-comment-body]
     [[:http/request {:method "POST"
                      :url (str "/api/articles/" slug "/comments")
                      :body {:comment {:body new-comment-body}}
                      :on-success [:comment/post-success slug]}]])

   :comment/delete
   (fn [_state slug comment-id]
     [[:http/request {:method "DELETE"
                      :url (str "/api/articles/" slug "/comments/" comment-id)
                      :on-success [:comment/delete-success slug comment-id]}]])
   ;;
   })
