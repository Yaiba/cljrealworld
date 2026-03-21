(ns realworld.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [realworld.server :as server]
            [realworld.db.tags :as tags-db]))

(def test-secret "test-secret")

(deftest tags-endpoint
  (testing "GET /api/tags returns 200 with tags list"
    (with-redefs [tags-db/list-all (fn [_] ["clojure" "reagent" "re-frame"])]
      (let [app  (server/create-app nil test-secret)
            resp (app (mock/request :get "/api/tags"))]
        (is (= 200 (:status resp))))))) 

(deftest get-current-user-requires-auth
  (testing "GET /api/user returns 401 without token"
    (let [app  (server/create-app nil test-secret)
          resp (app (mock/request :get "/api/user"))]
      (is (= 401 (:status resp))))))
