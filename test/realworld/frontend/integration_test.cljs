(ns realworld.frontend.integration-test
  (:require [cljs.test :refer [deftest is testing]]
            [datascript.core :as d]
            [nexus.core :as nexus]
            [frontend.schema :refer [schema]]
            [frontend.constants :refer [UI-ENTITY]]
            [frontend.actions :refer [actions]]
            [frontend.effects :refer [effects callbacks]]))

;; Stub HTTP: bypasses js/fetch entirely.
;; Looks up the URL in `responses`, calls the success callback directly.
(defn make-stub-http [responses]
  (fn [_ctx system {:keys [url on-success loading-key]}]
    (when loading-key
      (d/transact! system [{:db/id UI-ENTITY loading-key true}]))
    (let [[on-success-key & success-args] on-success
          handler (get callbacks on-success-key)
          resp    (get responses url)]
      (when loading-key
        (d/transact! system [{:db/id UI-ENTITY loading-key false}]))
      (when handler
        (apply handler system resp success-args)))))

;; Fixture data
(def login-resp
  {:user {:username "alice"
          :email    "alice@example.com"
          :token    "jwt-token-xyz"
          :bio      nil
          :image    nil}})

(def articles-resp
  {:articlesCount 1
   :articles [{:slug           "hello-world"
               :title          "Hello World"
               :description    "A test article"
               :body           "Body text"
               :tagList        ["clojure" "test"]
               :favoritesCount 3
               :favorited      false
               :author         {:username "alice" :bio nil :image nil}}]})

(deftest full-loop-login-then-fetch-articles-test
  (testing "set credentials → login → fetch articles → DataScript has user + articles"
    (let [conn (d/create-conn schema)
          _    (d/transact! conn [{:db/id UI-ENTITY :app/loaded-at 1}])
          test-nexus {:nexus/system->state (fn [c] (d/db c))
                      :nexus/placeholders  {}
                      :nexus/actions       actions
                      :nexus/effects       (assoc effects
                                                  :http/request
                                                  (make-stub-http
                                                   {"/api/users/login"              login-resp
                                                    "/api/articles?limit=10&offset=0" articles-resp}))}]

      ;; ── step 1: set email and password ───────────────────────────────────
      (nexus/dispatch test-nexus conn {} [[:app/set-email "alice@example.com"]])
      (nexus/dispatch test-nexus conn {} [[:app/set-password "password"]])

      (let [ui (d/pull (d/db conn) '[:app/email :app/password] UI-ENTITY)]
        (is (= "alice@example.com" (:app/email ui)))
        (is (= "password" (:app/password ui))))

      ;; ── step 2: login ────────────────────────────────────────────────────
      ;; app/login-success calls localStorage.setItem (needs stub) then
      ;; rfe/push-state (throws in Node — but d/transact! already ran, so catch and continue)
      (with-redefs [js/localStorage (clj->js {:setItem (fn [& _] nil)})]
        (try
          (nexus/dispatch test-nexus conn {} [[:app/login]])
          (catch :default _)))

      (let [user (get-in (d/pull (d/db conn)
                                 '[{:app/current-user [:user/username :user/token]}]
                                 UI-ENTITY)
                         [:app/current-user])]
        (is (= "alice"         (:user/username user)))
        (is (= "jwt-token-xyz" (:user/token user))))

      ;; ── step 3: fetch articles ───────────────────────────────────────────
      ;; app/fetch-articles-success is pure DataScript — no side-effectful calls
      (nexus/dispatch test-nexus conn {} [[:app/fetch-articles]])

      (let [articles (d/q '[:find [(pull ?a [:article/slug :article/title
                                             {:article/author [:user/username]}]) ...]
                            :where [?a :article/slug _]]
                          (d/db conn))]
        (is (= 1 (count articles)))
        (is (= "hello-world"  (:article/slug (first articles))))
        (is (= "Hello World"  (:article/title (first articles))))
        (is (= "alice"        (get-in (first articles) [:article/author :user/username])))))))
