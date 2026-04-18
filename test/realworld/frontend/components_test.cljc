(ns realworld.frontend.components-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [frontend.views.components :as c]))


(deftest error-banner-test
  (testing "returns nil when no errors"
    (is (nil? (c/error-banner nil))))
  (testing "shows each error message"
    (let [result (c/error-banner {:email ["can't be blank"]})]
      ;; select :li directly, not via :contains
      (is (seq (l/select '[:li] result))))))

(deftest article-card-test
  (let [article {:article/slug "hello"
                 :article/title "Hello World"
                 :article/description "A great article"
                 :article/author {:user/username "alice"}}
        result (c/article-card article)]
    (testing "renders title"
      (is (seq (l/select '[:* [:contains "Hello World"]] result))))
    (testing "renders author"
      (is (seq (l/select '[:* [:contains "alice"]] result))))
    (testing "click navigates to article"
      (is (= [[:article/view "hello"]]
             (get-in (l/attrs result) [:on :click]))))))

(deftest navbar-test
  (testing "shows sign in link when logged out"
    (let [result (c/navbar nil)]
      (is (seq (l/select '[:* [:contains "Sign in"]] result)))))
  (testing "shows username when logged in"
    (let [result (c/navbar {:user/username "alice"})]
      (is (seq (l/select '[:* [:contains "alice"]] result))))))

(deftest comment-card-test
  (let [comment {:comment/id 1 :comment/body "Great!" :comment/author {:user/username "bob"}}]
    (testing "hides delete for other user's comment"
      (let [result (c/comment-card comment {:user/username "alice"} "my-article")]
        (is (empty? (l/select '[:button] result)))))
    (testing "shows delete for own comment"
      (let [result (c/comment-card comment {:user/username "bob"} "my-article")]
        (is (seq (l/select '[:button] result)))))))

(deftest tag-pill-test
  (testing "renders tag text"
    (let [result (c/tag-pill "clojure")]
      ;; tag-pill IS the root span, use l/text instead
      (is (= "clojure" (l/text result)))))
  (testing "click dispatches set-tag action"
    (let [result (c/tag-pill "clojure")]
      (is (= [[:feed/set-tag "clojure"]]
             (get-in (l/attrs result) [:on :click]))))))
