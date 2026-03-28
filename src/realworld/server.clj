(ns realworld.server
  (:require [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.exception :as exception]
            [ring.middleware.file :as rfile]
            [realworld.db.users :as db.users]
            [realworld.db.follows :as db.follows]
            [realworld.db.articles :as db.articles]
            [realworld.db.comments :as db.comments]
            [realworld.db.favorites :as db.favorites]
            [realworld.db.tags :as db.tags]
            [realworld.auth :as auth]
            [realworld.schema :as schema]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring.coercion :as coercion]
            [malli.error :as me]
            [realworld.html :as html]
            [realworld.views.layout :as layout]))

(defn empty-string->nil
  [s]
  (when (not= s "") s))

;; ── response helpers ──────────────────────────────────────────────────────────

(defn- err [& messages]
  {:errors {:body (vec messages)}})

(defn- err-key [key & messages]
  {:errors {(keyword key) (vec messages)}})

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

(defn build-article-summary [ds article current-user-id]
  (dissoc (build-article-response ds article current-user-id) :body))

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
    (cond 
      (db.users/find-by-email ds (:email user)) {:status 409
                                                 :body   {:errors {:email ["has already been taken"]}}}
      (db.users/find-by-username ds (:username user)) {:status 409
                                                       :body   {:errors {:username ["has already been taken"]}}}    
      :else (let [created (db.users/create! ds user)
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
       :body   (err-key :credentials "invalid")})))

(defn get-user-handler
  [req]
  (let [identity (:identity req)
        user (db.users/find-by-id (:ds req) (:user-id identity))]
    {:status 200
     :body   {:user {:email    (:users/email user)
                     :username (:users/username user)
                     :token    (auth/sign-token user (:secret req))
                     :bio      (empty-string->nil (:users/bio user)) ; normalize to nil if empty
                     :image    (empty-string->nil (:users/image user))}}}))

(defn update-user-handler [req]
  (let [identity (:identity req)
        ds     (:ds req)
        fields (get-in req [:parameters :body :user])
        user   (db.users/update! ds (:user-id identity) fields)]
    {:status 200
     :body   {:user {:email    (:users/email user)
                     :username (:users/username user)
                     :bio      (empty-string->nil (:users/bio user))
                     :image    (empty-string->nil (:users/image user))
                     :token    (auth/sign-token user (:secret req))}}}))

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
      {:status 404 :body {:errors {:profile ["not found"]}}} ;; different body
      (let [identity  (:identity req)
            following (if identity
                        (db.follows/following? ds (:user-id identity) (:users/id user))
                        false)]
        {:status 200 :body {:profile (profile-response user following)}}))))

(defn follow-user-handler [req]
  (let [identity (:identity req)
        ds       (:ds req)
        username (get-in req [:path-params :username])
        user     (db.users/find-by-username ds username)]
    (if-not user
      {:status 404 :body {:errors {:profile ["not found"]}}}
      (do 
        (db.follows/follow! ds (:user-id identity) (:users/id user))
        {:status 200 :body {:profile (profile-response user true)}}))))

(defn unfollow-user-handler [req]
  (let [identity (:identity req)
        ds       (:ds req)
        username (get-in req [:path-params :username])
        user     (db.users/find-by-username ds username)]
    (if-not user
      {:status 404 :body {:errors {:profile ["not found"]}}}
      (do 
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
     :body   {:articles      (mapv #(build-article-summary ds % user-id) articles)
              :articlesCount cnt}}))

(defn get-feed-handler [req]
  (let [identity (:identity req)
        ds       (:ds req)
        user-id  (:user-id identity)
        params   (get-in req [:parameters :query])
        articles (db.articles/get-feed ds user-id params)
        cnt      (db.articles/count-feed ds user-id)]
    {:status 200
     :body   {:articles      (mapv #(build-article-summary ds % user-id) articles)
              :articlesCount cnt}}))

(defn get-article-handler [req]
  (let [ds      (:ds req)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      {:status 200
       :body   {:article (build-article-response ds article (:user-id (:identity req)))}})))

(defn create-article-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        data    (get-in req [:parameters :body :article])
        row     (db.articles/create! ds user-id data)
        article (db.articles/find-by-slug ds (:articles/slug row))]
    {:status 201
     :body   {:article (build-article-response ds article user-id)}}))

(defn update-article-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (println "update article: " article)
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (if-let [has-permission? (= user-id (:author_id article))]
        (let [data (get-in req [:parameters :body :article])
              _ (db.articles/update! ds slug user-id data)
              updated-article (db.articles/find-by-slug ds slug)]
          {:status 200
           :body   {:article (build-article-response ds updated-article user-id)}})
        {:status 403 :body (err-key :article "forbidden")}))))

(defn delete-article-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (if-let [has-permission? (= user-id (:author_id article))]
        (let [_ (db.articles/delete! ds slug user-id)]
          {:status 204 :body   {}})
        {:status 403 :body (err-key :article "forbidden")}))))

;; ── comment handlers ──────────────────────────────────────────────────────────

(defn get-comments-handler [req]
  (let [ds       (:ds req)
        slug     (get-in req [:path-params :slug])
        identity (:identity req)
        article  (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (let [comments (db.comments/find-by-article ds (:id article))]
        {:status 200
         :body   {:comments (mapv #(build-comment-response ds % (:user-id identity)) comments)}}))))

(defn add-comment-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        slug    (get-in req [:path-params :slug])
        body    (get-in req [:parameters :body :comment :body])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (let [row     (db.comments/create! ds user-id (:id article) body)
            comment (db.comments/find-by-id ds (:comments/id row))]
        {:status 201
         :body   {:comment (build-comment-response ds comment user-id)}}))))

(defn delete-comment-handler [req]
  (let [identity (:identity req)
        ds         (:ds req)
        user-id    (:user-id identity)
        slug    (get-in req [:path-params :slug])
        comment-id (parse-long (get-in req [:path-params :id]))
        article (db.articles/find-by-slug ds slug)
        comment (db.comments/find-by-id ds comment-id)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (if-not comment
        {:status 404 :body (err-key :comment "not found")}
        (if-let [has-permissions? (= user-id (:author_id comment))]
          (let [_ (db.comments/delete! ds comment-id user-id)]
            {:status 204 :body {}})
          {:status 403 :body (err-key :comment "forbidden")})))))

;; ── favorite handlers ─────────────────────────────────────────────────────────

(defn favorite-article-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (do
        (db.favorites/favorite! ds user-id (:id article))
        {:status 200
         :body   {:article (build-article-response ds article user-id)}}))))

(defn unfavorite-article-handler [req]
  (let [identity (:identity req)
        ds      (:ds req)
        user-id (:user-id identity)
        slug    (get-in req [:path-params :slug])
        article (db.articles/find-by-slug ds slug)]
    (if-not article
      {:status 404 :body {:errors {:article ["not found"]}}} ;; different body
      (do
        (db.favorites/unfavorite! ds user-id (:id article))
        {:status 200
         :body   {:article (build-article-response ds article user-id)}}))))

;; ── tags handler ──────────────────────────────────────────────────────────────

(defn get-tags-handler [req]
  {:status 200
   :body   {:tags (db.tags/list-all (:ds req))}})

;; ── router ────────────────────────────────────────────────────────────────────

(defn spa-handler
  []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "public/index.html")})

(defn default-handler
  [req]
  (if (clojure.string/includes? (get-in req [:headers "accept"] "") "text/html")
    (spa-handler)
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body "{\"errors\":{\"body\":[\"not found\"]}}"}))

(defn wrap-db
  [handler ds]
  (fn [req]
    (handler (assoc req :ds ds))))

(defn wrap-secret
  [handler secret]
  (fn [req]
    (handler (assoc req :secret secret))))

(defn wrap-auth
  [handler secret]
  (fn [req]
    (let [_identity (some-> (auth/extract-token req) (auth/verify-token secret))
          require-auth? (get-in req [:reitit.core/match :data :auth])]
      (if (and require-auth? (nil? _identity))
        {:status 401 :body {:errors {:token ["is missing"]}}}
        (handler (assoc req :identity _identity))))))

(defn make-cookie-auth-middleware
  [secret]
  {:name ::require-auth-cookie
   :wrap (fn [handler]
           (fn [req]
             (let [token (some-> (get-in req [:headers "cookie"])
                                 (clojure.string/split #"; ")
                                 (->> (filter #(clojure.string/starts-with? % "token=")))
                                 first
                                 (subs (count "token=")))
                   _identity (when token (auth/verify-token token secret))
                   match-data (get-in req [:reitit.core/match :data])
                   method (:request-method req)
                   needs-auth? (get (get match-data method) :auth (get match-data :auth))]
               (if (and needs-auth? (nil? _identity))
                 {:status 302 :headers {"Location" "/login"} :body ""}
                 (handler (assoc req :identity _identity))))))})

(defn make-auth-middleware [secret]
  {:name ::require-auth
   :wrap (fn [handler]
           (fn [req]
             (let [identity    (some-> (auth/extract-token req) (auth/verify-token secret))
                   match-data (get-in req [:reitit.core/match :data])
                   method (:request-method req)
                   needs-auth? (get (get match-data method) :auth (get match-data :auth))]
               (if (and needs-auth? (nil? identity))
                 {:status 401 :body {:errors {:token ["is missing"]}}}
                 (handler (assoc req :identity identity))))))})

(def coercion-error-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:reitit.coercion/request-coercion
     (fn [e _]
       (println "coercion error:" (type e) (ex-message e))
       (let [exdata (ex-data e)
             humanized (me/humanize {:schema (:schema exdata)
                                     :value (:value exdata)
                                     :errors (:errors exdata)})
             errors (if (and (map? humanized) (= 1 (count humanized)))
                      (first (vals humanized)) ;; unwrap {:article {...}} -> {...}
                      humanized)]
         (println "ex-data: " (ex-data e))
         (println "errors: " errors)
         {:status 422
          :body {:errors errors}
          }))
     
     :reitit.ring.middleware.exception/default
     (fn [e _]
       (println "unhandled exception:" (type e) (ex-message e))
       {:status 500
        :body (err (ex-message e))})})))

(defn create-api-router
  [secret]
  (-> (ring/router
       [["/api/users"
         {:post {:handler    #'create-user-handler
                 :parameters {:body [:map [:user schema/NewUser]]}}}]
        ["/api/users/login"
         {:post {:handler    #'user-login-handler
                 :parameters {:body [:map [:user schema/LoginCredentials]]}}}]
        ["/api/user"
         {:auth true
          :get #'get-user-handler
          :put {:handler    #'update-user-handler
                :parameters {:body [:map [:user schema/UpdateUser]]}}}]
        ["/api/profiles/:username"
         {:get #'get-profile-handler}]
        ["/api/profiles/:username/follow"
         {:auth true
          :post   #'follow-user-handler
          :delete #'unfollow-user-handler}]
        ["/api/articles"
         {:auth true}
         ["" {:get  {:auth false ; overwrite - no auth needed
                     :handler    #'list-articles-handler
                     :parameters {:query schema/ArticleFilters}}
              :post {:handler    #'create-article-handler
                     :parameters {:body [:map [:article schema/NewArticle]]}}}]
         ["/feed"
          {:get {:handler    #'get-feed-handler
                 :parameters {:query schema/FeedFilters}}}]
         ["/:slug"
          {:get    {:auth false
                    :handler #'get-article-handler}
           :put    {:handler    #'update-article-handler
                    :parameters {:body [:map [:article schema/UpdateArticle]]}}
           :delete #'delete-article-handler}]
         ["/:slug/comments"
          {:get  {:auth false
                  :handler #'get-comments-handler}
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
                            muuntaja/format-response-middleware
                            coercion-error-middleware
                            muuntaja/format-request-middleware
                            (make-auth-middleware secret)
                            coercion/coerce-request-middleware
                            coercion/coerce-response-middleware
                            ]}})))

(defn create-html-router
  [secret]
  (-> (ring/router
       [["/" {:get {:handler #'html/home-handler}}]
        ["/hello" {:auth true 
                   :get {:handler #'html/hello-handler}}]
        ["/login" {:get {:handler #'html/login-handler}
                   :post {:handler #'html/login-post-handler}}]]
       {:data {:middleware [(make-cookie-auth-middleware secret)]}})))

(defn create-app
  [ds secret]
  (-> (ring/routes
       (ring/ring-handler (create-api-router secret))
       default-handler) ;plain handler, catches everything that fell through 
      (ring.middleware.file/wrap-file "resources/public")
      (wrap-db ds)
      (wrap-secret secret)))