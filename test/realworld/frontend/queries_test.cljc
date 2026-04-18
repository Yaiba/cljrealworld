(ns realworld.frontend.queries-test
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [frontend.schema :refer [schema]]
            [frontend.constants :refer [UI-ENTITY]]
            [frontend.queries :as qs]))

(defn seed-db
  "Create a minimal in-memory DataScript db with test data."
  [tx-data]
  (d/db-with (d/empty-db schema) tx-data))

;; ---- fixtures ----

(def alice {:db/id -1 :user/username "alice" :user/email "alice@test.com" :user/token "tok123"})
(def article {:db/id -2 :article/slug "hello" :article/title "Hello World"
              :article/description "A post" :article/tags #{"clojure" "repl"}
              :article/author [:user/username "alice"]})

(deftest get-entity-test
  (testing "returns all attributes for an entity"
    (let [db (seed-db [{:db/id UI-ENTITY :app/page :page/home}])]
      (is (= :page/home (:app/page (qs/get-entity db UI-ENTITY)))))))

(deftest get-token-test
  (testing "returns nil when no user"
    (is (nil? (qs/get-token (seed-db [])))))
  (testing "returns token when user is linked"
    (let [db (seed-db [alice {:db/id UI-ENTITY :app/current-user -1}])]
      (is (= "tok123" (qs/get-token db))))))

(deftest query-tags-test
  (testing "returns nil when no tags"
    (is (nil? (qs/query-tags (seed-db [])))))
  (testing "returns tags set"
    (let [db (seed-db [{:db/id UI-ENTITY :app/tags #{"clojure" "reagent"}}])]
      (is (= #{"clojure" "reagent"} (set (qs/query-tags db)))))))

(deftest query-articles-test
  (testing "returns empty when no articles"
    (is (empty? (qs/query-articles (seed-db [])))))
  (testing "returns articles with author"
    (let [db (seed-db [alice article])
          result (qs/query-articles db)]
      (is (= 1 (count result)))
      (is (= "hello" (:article/slug (first result))))
      (is (= "alice" (get-in (first result) [:article/author :user/username]))))))

(deftest query-current-user-test
  (testing "returns nil when no user"
    (is (nil? (qs/query-current-user (seed-db [])))))
  (testing "returns user map when logged in"
    (let [db (seed-db [alice {:db/id UI-ENTITY :app/current-user -1}])]
      (is (= "alice" (:user/username (qs/query-current-user db)))))))

(deftest query-article-by-slug-test
  (testing "returns nil for unknown slug"
    (is (nil? (qs/query-article-by-slug (seed-db []) "nope"))))
  (testing "returns article with author for known slug"
    (let [db (seed-db [alice article])
          result (qs/query-article-by-slug db "hello")]
      (is (= "Hello World" (:article/title result)))
      (is (= "alice" (get-in result [:article/author :user/username]))))))
