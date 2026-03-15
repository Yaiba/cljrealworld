(ns realworld.server
  (:require [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [realworld.db.users :as db.users]
            [realworld.db.follows :as db.follows]
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

(defn dummy-handler
  [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "Hello, World!"})

(defn create-user-handler
  [req]
  (let [ds (:ds req)
        secret (:secret req)
        user (get-in req [:parameters :body :user])]
    (if (db.users/find-by-email ds (:email user))
      {:status 422
       :headers {"Content-Type" "application/json"}
       :body {:error {:email "has already been taken"}}}
      (let [created (db.users/create! ds user) ; TODO: try-catch?
            token (auth/sign-token created secret)]
        (println "Creating user:" user)
        {:status 201
         :headers {"Content-Type" "application/json"}
         :body {:user {:email (:users/email user)
                       :username (:users/username user)
                       :token token
                       :bio (:users/bio user)
                       :image (:users/image user)}}}))))

(defn user-login-handler
  [req]
  (let [ds (:ds req)
        secret (:secret req)
        credentials (get-in req [:parameters :body :user])
        user (db.users/find-by-email ds (:email credentials))]
    (if (and user (= (:users/password user) (:password credentials)))
      (let [token (auth/sign-token user secret)]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:user {:email (:users/email user)
                       :username (:users/username user)
                       :token token
                       :bio (:users/bio user)
                       :image (:users/image user)}}})
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body {:error "Invalid email or password"}})))

(defn update-user-handler [req]
  (let [_identity (:identity req)]
    (if-not _identity
      {:status 401 :body {:error "Unauthorized"}}
      (let [ds     (:ds req)
            fields (get-in req [:parameters :body :user])
            user   (db.users/update! ds (:user-id _identity) fields)]
        {:status 200
         :body {:user {:email    (:users/email user)
                       :username (:users/username user)
                       :bio      (:users/bio user)
                       :image    (:users/image user)
                       :token    (auth/sign-token user (:secret req))}}}))))

(defn get-tags-handler
  [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {:tags []}})

(defn get-user-handler
  "Hardcoded user response for testing purposes."
  [req]
  (if-let [_identity (:identity req)]
    (let [user (db.users/find-by-id (:ds req) (:user-id _identity))]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:user (dissoc user :password)}})
    {:status 401
     :headers {"Content-Type" "application/json"}
     :body {:error "Unauthorized"}}))

(defn profile-response [user following]
  {:username  (:users/username user)
   :bio       (:users/bio user)
   :image     (:users/image user)
   :following following})

(defn get-profile-handler [req]
  (let [ds       (:ds req)
        username (get-in req [:path-params :username])
        user     (db.users/find-by-username ds username)]
    (if-not user
      {:status 404 :headers {"Content-Type" "application/json"} :body {:error "User not found"}}
      (let [_identity  (:identity req)
            following (if _identity
                        (db.follows/following? ds (:user-id _identity) (:users/id user))
                        false)]
        {:status 200 :headers {"Content-Type" "application/json"} :body {:profile (profile-response user following)}}))))

(defn follow-user-handler [req]
  (let [_identity (:identity req)]
    (if-not _identity
      {:status 401 :body {:error "Unauthorized"}}
      (let [ds       (:ds req)
            username (get-in req [:path-params :username])
            user     (db.users/find-by-username ds username)]
        (db.follows/follow! ds (:user-id _identity) (:users/id user))
        {:status 200 :headers {"Content-Type" "application/json"} :body {:profile (profile-response user true)}}))))

(defn unfollow-user-handler [req]
  (let [identity (:identity req)]
    (if-not identity
      {:status 401 :body {:error "Unauthorized"}}
      (let [ds       (:ds req)
            username (get-in req [:path-params :username])
            user     (db.users/find-by-username ds username)]
        (db.follows/unfollow! ds (:user-id identity) (:users/id user))
        {:status 200 :headers {"Content-Type" "application/json"} :body {:profile (profile-response user false)}}))))

(defn create-app
  [ds secret]
  (-> (ring/router
       [["/api/users" {:post {:handler #'create-user-handler
                              :parameters {:body [:map [:user schema/NewUser]]}}}]
        ["/api/users/login" {:post {:handler #'user-login-handler
                                    :parameters {:body [:map [:user schema/LoginCredentials]]}}}]
        ["/api/user" {:get #'get-user-handler
                      :put {:handler #'update-user-handler
                            :parameters {:body [:map [:user [:map
                                                             [:email   {:optional true} :string]
                                                             [:username {:optional true} :string]
                                                             [:bio      {:optional true} :string]
                                                             [:image    {:optional true} :string]
                                                             [:password {:optional true} :string]]]]}}}]
        ["/api/tags" {:get #'get-tags-handler}]
        ["/api/profiles/:username" {:get #'get-profile-handler}]
        ["/api/profiles/:username/follow" {:post   #'follow-user-handler
                                           :delete #'unfollow-user-handler}]]
       {:data {:muuntaja m/instance
               :coercion malli-coercion/coercion
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