(ns realworld.html
  (:require [realworld.auth :as auth]
            [realworld.db.users :as db.users]
            [realworld.db.articles :as db.articles]
            [realworld.views.layout :as layout]
            [realworld.views.login :as login]
            [realworld.views.home :as home]
            [hiccup2.core :as hi]
            [realworld.sse :as sse]
            [jsonista.core :as json]
            [starfederation.datastar.clojure.api :as d*]
            [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]))
            

;; ── html handler ───────────────────────────────────────────────────────────────

(defn index-handler
  [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (layout/page 
          "Realworld" 
          [:div {:data-signals "{count: 0}"}
           [:p#result "Click the button"]
           [:button {:data-on:click "@get(\"/hello\")"} "Say hello"]])})

(defn home-handler
  [req]
  (let [ds       (:ds req)
        identity (:identity req)
        user-id  (:user-id identity)
        articles (db.articles/list-articles ds nil)
        cnt      (db.articles/count-articles ds nil)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (layout/page
            "Realworld"
            (home/home-page articles))}))

(defn hello-handler [req]
  (hk-gen/->sse-response 
   req
   {hk-gen/on-open
    (fn [sse]
      (d*/patch-elements! sse (str (hi/html [:div#result "Hello from datastar!"])))
      (d*/close-sse! sse))}))

(defn login-handler
  [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (login/login-page)})

(defn login-post-handler
  [req]
  (let [ds          (:ds req)
        secret      (:secret req)
        credentials (json/read-value (slurp (:body req)) json/keyword-keys-object-mapper)
        user        (db.users/find-by-email ds (:email credentials))]
    (tap> credentials)
    (if (and user (= (:users/password user) (:password credentials)))
      (let [token (auth/sign-token user secret)]
        (tap> "credentials correct")
        (hk-gen/->sse-response
         req
         {hk-gen/on-open
          (fn [sse]
            (d*/execute-script! sse (str "window.location = '/'"))
            (d*/close-sse! sse))
          :headers {"Set-Cookie" (str "token=" token "; HttpOnly; Path=/")}}))
      (hk-gen/->sse-response
       req
       {hk-gen/on-open
        (fn [sse]
          (d*/patch-elements! sse (str (hi/html [:div#login-error "Invalid credentials"])))
          (d*/close-sse! sse))}))))