(ns realworld.frontend.views-test
  (:require [clojure.test :refer [deftest is testing]]
            [lookup.core :as l]
            [frontend.views.home :as home]
            [frontend.views.login :as login]
            [frontend.views.settings :as settings]
            [frontend.views.editor :as editor]
            [frontend.views.profile :as profile]))

(def alice {:user/username "alice" :user/email "alice@test.com" :user/image nil})

(def sample-article {:article/slug "hello"
                     :article/title "Hello World"
                     :article/description "A post"
                     :article/tags #{"clojure"}
                     :article/author {:user/username "alice"}})

;; ---- home ----

(deftest home-view-test
  (testing "renders without crashing"
    (let [result (home/render-ui [sample-article] ["clojure"] :global alice 0 1 false)]
      (is (vector? result))))
  (testing "shows global feed tab"
    (let [result (home/render-ui [] [] :global nil 0 0 false)]
      (is (seq (l/select '[:* [:contains "Global Feed"]] result)))))
  (testing "hides personal feed when logged out"
    (let [result (home/render-ui [] [] :global nil 0 0 false)]
      (is (empty? (l/select '[:* [:contains "Your Feed"]] result)))))
  (testing "shows personal feed when logged in"
    (let [result (home/render-ui [] [] :global alice 0 0 false)]
      (is (seq (l/select '[:* [:contains "Your Feed"]] result))))))

;; ---- login ----

(deftest login-view-test
  (testing "renders without crashing"
    (is (vector? (login/render-ui "a@b.com" "secret" nil))))
  (testing "shows error banner when errors present"
    (let [result (login/render-ui "" "" {:email ["can't be blank"]})]
      (is (seq (l/select '[:li] result))))))

;; ---- settings ----

(deftest settings-view-test
  (testing "renders without crashing"
    (let [ui {:settings/username "alice" :settings/email "alice@test.com"
              :settings/bio "hi" :settings/image nil}]
      (is (vector? (settings/render-ui ui nil))))))

;; ---- editor ----

(deftest editor-view-test
  (testing "renders without crashing for new article"
    (let [ui {:editor/title "" :editor/description "" :editor/body "" :editor/tags #{}}]
      (is (vector? (editor/render-ui ui nil alice)))))
  (testing "renders without crashing for edit"
    (let [ui {:editor/title "Existing" :editor/description "desc"
              :editor/body "body" :editor/tags #{"clojure"} :editor/slug "existing"}]
      (is (vector? (editor/render-ui ui nil alice))))))

;; ---- profile ----

(deftest profile-view-test
  (testing "renders without crashing"
    (let [profile {:user/username "alice" :user/bio "hi" :user/image nil :user/following? false}]
      (is (vector? (profile/render-ui profile :my-articles [sample-article]))))))
