(ns realworld.frontend.actions-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [frontend.schema :refer [schema]]
            [frontend.constants :refer [UI-ENTITY]]
            [frontend.actions :refer [actions]]))

(defn seed-db [tx-data]
  (d/db-with (d/empty-db schema) tx-data))

(defn dispatch [action-key db & args]
  (apply (get actions action-key) db args))

(deftest app-set-email-test
  (testing "transacts email onto UI entity"
    (let [result (dispatch :app/set-email (seed-db []) "bob@test.com")]
      (is (= [[:state/transact [{:db/id UI-ENTITY :app/email "bob@test.com"}]]]
             result)))))

(deftest feed-set-tag-test
  (testing "returns transact + http request"
    (let [[transact-effect http-effect] (dispatch :feed/set-tag (seed-db []) "clojure")]
      (is (= :state/transact (first transact-effect)))
      (is (= :http/request (first http-effect)))
      (is (clojure.string/includes? (get-in http-effect [1 :url]) "tag=clojure")))))

(deftest app-login-test
  (testing "posts credentials from UI entity"
    (let [db (seed-db [{:db/id UI-ENTITY :app/email "a@b.com" :app/password "secret"}])
          [[_ {:keys [method url body]}]] (dispatch :app/login db)]
      (is (= "POST" method))
      (is (= "/api/users/login" url))
      (is (= "a@b.com" (get-in body [:user :email])))
      (is (= "secret" (get-in body [:user :password]))))))

(deftest user-logout-test
  (testing "returns logout effect"
    (is (= [[:user/logout]]
           (dispatch :user/logout (seed-db []))))))

(deftest editor-add-tag-test
  (testing "adds tag to set and clears input"
    (let [db (seed-db [{:db/id UI-ENTITY :editor/tags #{"clojure"} :editor/tag-input "repl"}])
          [[_ [{:keys [editor/tags editor/tag-input]}]]] (dispatch :editor/add-tag db)]
      (is (contains? tags "repl"))
      (is (= "" tag-input))))
  (testing "ignores empty input"
    (let [db (seed-db [{:db/id UI-ENTITY :editor/tag-input "  "}])]
      (is (= [] (dispatch :editor/add-tag db))))))

(deftest editor-remove-tag-test
  (testing "removes tag from set"
    (let [db (seed-db [{:db/id UI-ENTITY :editor/tags #{"clojure" "repl"}}])
          [[_ [{:keys [editor/tags]}]]] (dispatch :editor/remove-tag db "clojure")]
      (is (not (contains? tags "clojure")))
      (is (contains? tags "repl")))))

(deftest article-fetch-test
  (testing "stores slug and fires HTTP request"
    (let [[transact-effect http-effect] (dispatch :article/fetch (seed-db []) "my-slug")]
      (is (= :state/transact (first transact-effect)))
      (is (= :http/request (first http-effect)))
      (is (clojure.string/includes? (get-in http-effect [1 :url]) "my-slug")))))
