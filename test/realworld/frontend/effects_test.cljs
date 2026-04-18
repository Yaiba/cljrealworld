(ns realworld.frontend.effects-test
  (:require [cljs.test :refer [deftest is testing]]
            [datascript.core :as d]
            [frontend.schema :refer [schema]]
            [frontend.constants :refer [UI-ENTITY]]
            [frontend.effects :refer [effects]]
            [reitit.frontend.easy :as rfe]))

(defn seed-conn [tx-data]
  (let [conn (d/create-conn schema)]
    (d/transact! conn [{:app/loaded-at (.getTime (js/Date.))}]);make sure ui-entity exists
    (d/transact! conn tx-data)
    conn))

(deftest http-request-fires-fetch-test
  (testing "fires fetch with correct method and url"
    (let [captured (atom nil)
          stub (fn [url opts]
                 (reset! captured {:url url :opts opts})
                 (js/Promise.resolve #js {:text (fn [] (js/Promise.resolve ""))}))
          http-effect (get effects :http/request)
          conn (seed-conn [])]
      (with-redefs [js/fetch stub] ;; js/fetch will be our stub.
        (http-effect {} conn {:method "GET"
                              :url "/api/tags"
                              :on-success [:app/fetch-tags-success]}))
      ;; fetch is called synchronously — no need to wait
      (is (= "/api/tags" (:url @captured)))
      (is (= "GET" (.-method (clj->js (:opts @captured))))))))


(deftest http-request-injects-auth-header-test
  (testing "adds Authorization header when token present"
    (let [captured (atom nil)
          stub (fn [_url opts]
                 (reset! captured opts)
                 (-> (js/Promise.resolve "")
                     (.then (fn [_] #js {:text (fn [] (js/Promise.resolve ""))}))))
          http-effect (get effects :http/request)
          conn (seed-conn [{:db/id -1 :user/token "mytoken"}
                           {:db/id UI-ENTITY :app/current-user -1}])]
      (with-redefs [js/fetch stub]
        (http-effect {} conn {:method "GET" :url "/api/user" :on-success [:app/fetch-current-user-success]}))
    
      (let [headers (js->clj (.-headers (clj->js @captured)) :keywordize-keys false)]
        (is (= "Token mytoken" (get headers "Authorization"))))
      )))

(deftest state-transact
  (testing "transacts given data onto db"
    (let [conn (seed-conn [])]
      ((get effects :state/transact) {} conn [{:db/id -1 :app/test "value"}])
      (is (= "value" (:app/test (d/entity (d/db conn) 2))))))) ;;db/id 2 because of the initial seed transaction

(deftest user-logout
  (testing "clears user token and current user"
    (let [conn (seed-conn [{:db/id -1 :user/token "mytoken"}
                             {:db/id UI-ENTITY :app/current-user -1}])]
      (with-redefs [rfe/push-state (fn [& _] nil)
                    js/localStorage (clj->js {:removeItem (fn [& _] nil)})]
        (try
          ((get effects :user/logout) {} conn [])
          (catch :default _))
        ;; with-redefs doesn't intercept rfe/push-state in compiled ClojureScript — direct function references bypass var lookup. 
        ;; And localStorage doesn't exist in Node
        ;; transact! already ran before the error — assert on conn state here
)
      (is (nil? (:user/token (d/entity (d/db conn) 1))))
      (is (nil? (:app/current-user (d/entity (d/db conn) UI-ENTITY)))))))