(ns realworld.server
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [realworld.db :as db]
            [ring.mock.request :as mock]
            [realworld.auth :as auth]))

(defonce server-instance (atom nil))

(defn handler
  [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "Hello, World!"})

(defn create-user-handler
  [req]
  (let [user (get-in req [:body-params])
        user-existing? (db/find-user-by-email (:email user))]
    (if user-existing?
      {:status 422
       :headers {"Content-Type" "application/json"}
       :body {:error "User already exists"}}
      (do
        (println "Creating user:" user)
        (db/create-user! user)
        {:status 201
         :headers {"Content-Type" "application/json"}
         :body {:user user}}))))

(defn user-login-handler
  [req]
  (let [credentials (get-in req [:body-params])
        user (db/find-user-by-email (:email credentials))]
    (if (and user (= (:password user) (:password credentials)))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:user (dissoc user :password)}}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body {:error "Invalid email or password"}})))

(defn get-tags-handler
  [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {:tags (:tags @db/app-db)}})

(defn get-user-handler
  "Hardcoded user response for testing purposes."
  [req]
  (let [_identity (:identity req)]
    (if _identity
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:user _identity}}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body {:error "Unauthorized"}})))

(defn create-app
  [secret]
  (-> (ring/router
       [["/api/users" {:post #'create-user-handler}]
        ["/api/users/login" {:post #'user-login-handler}]
        ["/api/user" {:get #'get-user-handler}]
        ["/api/tags" {:get #'get-tags-handler}]]
       {:data {:muuntaja m/instance
               :middleware [parameters/parameters-middleware
                            muuntaja/format-negotiate-middleware
                            muuntaja/format-response-middleware
                            muuntaja/format-request-middleware]}})
      (ring/ring-handler (ring/create-default-handler))
      (auth/wrap-auth secret)))

(comment
  (def test-user {:email "test@test.com" :username "test" :password "secret"})
  (db/create-user! test-user)
  (get-tags-handler {})
  (require '[ring.mock.request :as mock])
  )