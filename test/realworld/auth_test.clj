(ns realworld.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [realworld.auth :as auth]))

(def ^:private secret "test-secret")

(deftest sign-and-verify-token
  (testing "a signed token can be verified"
    (let [fake-user {:users/id 42 :users/username "alice"}
          token     (auth/sign-token fake-user secret)
          claims    (auth/verify-token token secret)]
      (is (string? token))
      (is (= 42 (:user-id claims)))
      (is (= "alice" (:username claims))))))

(deftest invalid-token-returns-nil
  (testing "garbage token returns nil"
    (is (nil? (auth/verify-token "not-a-real-token" secret)))))

(deftest wrong-secret-returns-nil
  (testing "token signed with different secret fails verification"
    (let [fake-user {:users/id 1 :users/username "bob"}
          token     (auth/sign-token fake-user "secret-a")]
      (is (nil? (auth/verify-token token "secret-b"))))))