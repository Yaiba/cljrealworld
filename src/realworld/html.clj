(ns realworld.html
  (:require [realworld.auth :as auth]
            [realworld.db.users :as db.users]
            [realworld.views.layout :as layout]
            [realworld.views.login :as login]
            [hiccup2.core :as hi]
            [realworld.sse :as sse]
            [jsonista.core :as json]))

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

(defn hello-handler
  [_]
  {:status 200
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"}
   :body (sse/merge-fragment (str (hi/html [:div#result "hello from server!"])))})

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
        {:status 200
         :headers {"Content-Type" "text/event-stream"
                   "Cache-Control" "no-cache"
                   "Set-Cookie" (str "token=" token "; HttpOnly; Path=/")}
         :body  (sse/redirect "/")})
      {:status 200 ; 401 will kill the SSE response, datastar only process SSE for 200
       :headers {"Content-Type" "text/event-stream"
                 "Cache-Control" "no-cache"}
       :body (sse/merge-fragment (str (hi/html [:div#login-error "Invalid credentials"])))})))