(ns realworld.server
  (:require [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [realworld.db.users :as db.users]
            [realworld.db.follows :as db.follows]
            [realworld.db.articles :as db.articles]
            [realworld.db.comments :as db.comments]
            [realworld.db.favorites :as db.favorites]
            [realworld.db.tags :as db.tags]
            [realworld.auth :as auth]
            [realworld.schema :as schema]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring.coercion :as coercion]))

(defn wrap-db
  [handler ds]
  (fn [req]
    (handler (assoc req :ds ds))))

(defn wrap-secret
  [handler secret]
  (fn [req]
    (handler (assoc req :secret secret))))

;; ── response helpers ──────────────────────────────────────────────────────────

(defn- err [& messages]
  {:errors {:body (vec messages)}})

(defn- ts->str [ts]
  (when ts (str (.toInstant ts))))

(defn build-article-response [ds article current-user-id]
  (let [article-id  (:id article)
        author-id   (:author_id article)
        tags        (db.articles/find-tags-for-article ds article-id)
        fav-count   (db.favorites/favorites-count ds article-id)
        favorited   (if current-user-id
                      (db.favorites/favorited? ds current-user-id article-id)
                      false)
        following   (if current-user-id
                      (db.follows/following? ds current-user-id author-id)
                      false)]
    {:slug           (:slug article)
     :title          (:title article)
     :description    (:description article)
     :body           (:body article)
     :tagList        (vec tags)
     :createdAt      (ts->str (:created_at article))
     :updatedAt      (ts->str (:updated_at article))
     :favorited      favorited
     :favoritesCount fav-count
     :author         {:username  (:author_username article)
                      :bio       (:author_bio article)
                      :image     (:author_image article)
                      :following following}}))

(defn build-comment-response [ds comment current-user-id]
  (let [author-id (:author_id comment)
        following (if current-user-id
                    (db.follows/following? ds current-user-id author-id)
                    false)]
    {:id        (:id comment)
     :body      (:body comment)
     :createdAt (ts->str (:created_at comment))
     :updatedAt (ts->str (:updated_at comment))
     :author    {:username  (:author_username comment)
                 :bio       (:author_bio comment)
                 :image     (:author_image comment)
                 :following following}}))

;; ── user handlers ─────────────────────────────────────────────────────────────

(defn create-user-handler
  [req]
  (let [ds     (:ds req)
        secret (:secret req)
        user   (get-in req [:parameters :body :user])]
    (if (db.users/find-by-email ds (:email user))
      {:status 422
       :body   (err "email has already been taken")}
      (let [created (db.users/create! ds user)
            token   (auth/sign-token created secret)]
        {:status 201
         :body   {:user {:email    (:users/email created)
                         :username (:users/username created)
                         :token    token
                         :bio      (:users/bio created)
                         :image    (:users/image created)}}}))))

(defn user-login-handler
  [req]
  (let [ds          (:ds req)
        secret      (:secret req)
        credentials (get-in req [:parameters :body :user])
        user        (db.users/find-by-email ds (:email credentials))]
    (if (and user (= (:users/password user) (:password credentials)))
      (let [token (auth/sign-token user secret)]
        {:status 200
         :body   {:user {:email    (:users/email user)
                         :username (:users/username user)
                         :token    token
                         :bio      (:users/bio user)
                         :image    (:users/image user)}}})
      {:status 401
       :body   (err "Invalid email or password")})))

(defn get-user-handler
  [req]
  (if-let [identity (:identity req)]
    (let [user (db.users/find-by-id (:ds req) (:user-id identity))]
      {:status 200
       :body   {:user {:email    (:users/email user)
                       :username (:users/username user)
                       :token    (auth/sign-token user (:secret req))
                       :bio      (:users/bio user)
                       :image    (:users/image user)}}})
    {:status 401
     :body   (err "Unauthorized")}))

(defn update-user-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds     (:ds req)
            fields (get-in req [:parameters :body :user])
            user   (db.users/update! ds (:user-id identity) fields)]
        {:status 200
         :body   {:user {:email    (:users/email user)
                         :username (:users/username user)
                         :bio      (:users/bio user)
                         :image    (:users/image user)
                         :token    (auth/sign-token user (:secret req))}}}))))

;; ── profile handlers ──────────────────────────────────────────────────────────

(defn- profile-response [user following]
  {:username  (:users/username user)
   :bio       (:users/bio user)
   :image     (:users/image user)
   :following following})

(defn get-profile-handler [req]
  (let [ds       (:ds req)
        username (get-in req [:path-params :username])
        user     (db.users/find-by-username ds username)]
    (if-not user
      {:status 404 :body (err "User not found")}
      (let [identity  (:identity req)
            following (if identity
                        (db.follows/following? ds (:user-id identity) (:users/id user))
                        false)]
        {:status 200 :body {:profile (profile-response user following)}}))))

(defn follow-user-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds       (:ds req)
            username (get-in req [:path-params :username])
            user     (db.users/find-by-username ds username)]
        (db.follows/follow! ds (:user-id identity) (:users/id user))
        {:status 200 :body {:profile (profile-response user true)}}))))

(defn unfollow-user-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds       (:ds req)
            username (get-in req [:path-params :username])
            user     (db.users/find-by-username ds username)]
        (db.follows/unfollow! ds (:user-id identity) (:users/id user))
        {:status 200 :body {:profile (profile-response user false)}}))))

;; ── article handlers ──────────────────────────────────────────────────────────

(defn list-articles-handler [req]
  (let [ds       (:ds req)
        identity (:identity req)
        user-id  (:user-id identity)
        params   (get-in req [:parameters :query])
        articles (db.articles/list-articles ds params)
        cnt      (db.articles/count-articles ds params)]
    {:status 200
     :body   {:articles      (mapv #(build-article-response ds % user-id) articles)
              :articlesCount cnt}}))

(defn get-feed-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds       (:ds req)
            user-id  (:user-id identity)
            params   (get-in req [:parameters :query])
            articles (db.articles/get-feed ds user-id params)
            cnt      (db.articles/count-feed ds user-id)]
        {:status 200
         :body   {:articles      (mapv #(build-article-response ds % user-id) articles)
                  :articlesCount cnt}}))))

(defn get-article-handler [req]
  (let [ds      (:ds req)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body (err "Article not found")}
      {:status 200
       :body   {:article (build-article-response ds article (:user-id (:identity req)))}})))

(defn create-article-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            data    (get-in req [:parameters :body :article])
            row     (db.articles/create! ds user-id data)
            article (db.articles/find-by-slug ds (:articles/slug row))]
        {:status 201
         :body   {:article (build-article-response ds article user-id)}}))))

(defn update-article-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            slug    (get-in req [:path-params :slug])
            data    (get-in req [:parameters :body :article])
            row     (db.articles/update! ds slug user-id data)]
        (if-not row
          {:status 404 :body (err "Article not found")}
          (let [article (db.articles/find-by-slug ds (:articles/slug row))]
            {:status 200
             :body   {:article (build-article-response ds article user-id)}}))))))

(defn delete-article-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            slug    (get-in req [:path-params :slug])]
        (db.articles/delete! ds slug user-id)
        {:status 200 :body {}}))))

;; ── comment handlers ──────────────────────────────────────────────────────────

(defn get-comments-handler [req]
  (let [ds       (:ds req)
        slug     (get-in req [:path-params :slug])
        identity (:identity req)
        article  (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body (err "Article not found")}
      (let [comments (db.comments/find-by-article ds (:id article))]
        {:status 200
         :body   {:comments (mapv #(build-comment-response ds % (:user-id identity)) comments)}}))))

(defn add-comment-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            slug    (get-in req [:path-params :slug])
            body    (get-in req [:parameters :body :comment :body])
            article (db.articles/find-by-slug ds slug)]
        (if-not article
          {:status 404 :body (err "Article not found")}
          (let [row     (db.comments/create! ds user-id (:id article) body)
                comment (db.comments/find-by-id ds (:comments/id row))]
            {:status 201
             :body   {:comment (build-comment-response ds comment user-id)}}))))))

(defn delete-comment-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds         (:ds req)
            user-id    (:user-id identity)
            comment-id (parse-long (get-in req [:path-params :id]))]
        (db.comments/delete! ds comment-id user-id)
        {:status 200 :body {}}))))

;; ── favorite handlers ─────────────────────────────────────────────────────────

(defn favorite-article-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            slug    (get-in req [:path-params :slug])
            article (db.articles/find-by-slug ds slug)]
        (if-not article
          {:status 404 :body (err "Article not found")}
          (do
            (db.favorites/favorite! ds user-id (:id article))
            {:status 200
             :body   {:article (build-article-response ds article user-id)}}))))))

(defn unfavorite-article-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body (err "Unauthorized")}
      (let [ds      (:ds req)
            user-id (:user-id identity)
            slug    (get-in req [:path-params :slug])
            article (db.articles/find-by-slug ds slug)]
        (if-not article
          {:status 404 :body (err "Article not found")}
          (do
            (db.favorites/unfavorite! ds user-id (:id article))
            {:status 200
             :body   {:article (build-article-response ds article user-id)}}))))))

;; ── tags handler ──────────────────────────────────────────────────────────────

(defn get-tags-handler [req]
  {:status 200
   :body   {:tags (db.tags/list-all (:ds req))}})

;; ── router ────────────────────────────────────────────────────────────────────

(defn create-app
  [ds secret]
  (-> (ring/router
       [["/api/users"
         {:post {:handler    #'create-user-handler
                 :parameters {:body [:map [:user schema/NewUser]]}}}]
        ["/api/users/login"
         {:post {:handler    #'user-login-handler
                 :parameters {:body [:map [:user schema/LoginCredentials]]}}}]
        ["/api/user"
         {:get #'get-user-handler
          :put {:handler    #'update-user-handler
                :parameters {:body [:map [:user [:map
                                                 [:email    {:optional true} :string]
                                                 [:username {:optional true} :string]
                                                 [:bio      {:optional true} :string]
                                                 [:image    {:optional true} :string]
                                                 [:password {:optional true} :string]]]]}}}]
        ["/api/profiles/:username"
         {:get #'get-profile-handler}]
        ["/api/profiles/:username/follow"
         {:post   #'follow-user-handler
          :delete #'unfollow-user-handler}]
        ["/api/articles"
         ["" {:get  {:handler    #'list-articles-handler
                     :parameters {:query schema/ArticleFilters}}
              :post {:handler    #'create-article-handler
                     :parameters {:body [:map [:article schema/NewArticle]]}}}]
         ["/feed"
          {:get {:handler    #'get-feed-handler
                 :parameters {:query schema/FeedFilters}}}]
         ["/:slug"
          {:get    #'get-article-handler
           :put    {:handler    #'update-article-handler
                    :parameters {:body [:map [:article schema/UpdateArticle]]}}
           :delete #'delete-article-handler}]
         ["/:slug/comments"
          {:get  #'get-comments-handler
           :post {:handler    #'add-comment-handler
                  :parameters {:body [:map [:comment schema/NewComment]]}}}]
         ["/:slug/comments/:id"
          {:delete #'delete-comment-handler}]
         ["/:slug/favorite"
          {:post   #'favorite-article-handler
           :delete #'unfavorite-article-handler}]]
        ["/api/tags"
         {:get #'get-tags-handler}]]
       {:conflicts nil
        :data {:muuntaja   m/instance
               :coercion   malli-coercion/coercion
               :middleware [parameters/parameters-middleware
                            muuntaja/format-negotiate-middleware
                            coercion/coerce-exceptions-middleware
                            muuntaja/format-response-middleware
                            muuntaja/format-request-middleware
                            coercion/coerce-request-middleware
                            coercion/coerce-response-middleware]}})
      (ring/ring-handler (ring/create-default-handler))
      (wrap-db ds)
      (wrap-secret secret)
      (auth/wrap-auth secret)))
