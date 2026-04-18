(ns frontend.portfolio
  (:require [portfolio.ui :as p]
            [replicant.dom :as r]
            [portfolio.replicant :refer-macros [defscene]]
            [frontend.views.articles :as articles]
            [frontend.views.components :as c]))

;; articles
(defscene articles-card-normal
  (articles/render-ui [{:article/slug "hello-world"
                        :article/title "Hello World"
                        :article/description "This is my first article."
                        :article/author {:user/username "Alice"}}]))
;; error banner
(defscene error-banner-with-errors
  (c/error-banner {:email ["can't be blank"] :password ["is too short"]}))

(defscene error-banner-empty
  (c/error-banner nil))

;; article 
(defscene article-card-normal
  (c/article-card {:article/slug "hello-world"
                   :article/title "Hello World"
                   :article/description "A great article"
                   :article/author {:user/username "alice"}}))

(defscene article-card-long-title
  (c/article-card {:article/slug "long"
                   :article/title "This is a very long title that might overflow or wrap in unexpected ways"
                   :article/description "Short desc"
                   :article/author {:user/username "bob"}}))

(defscene article-card-no-description
  (c/article-card {:article/slug "no-desc"
                   :article/title "No Description"
                   :article/description nil
                   :article/author {:user/username "carol"}}))

;; navbar
(defscene navbar-logged-out
  (c/navbar nil))

(defscene navbar-logged-in
  (c/navbar {:user/username "alice"}))

;; comment card
(defscene comment-card-own
  (c/comment-card {:comment/id 1
                   :comment/body "Great article!"
                   :comment/author {:user/username "alice"}}
                  {:user/username "alice"}
                  "my-article"))

(defscene comment-card-other
  (c/comment-card {:comment/id 2
                   :comment/body "Thanks for sharing"
                   :comment/author {:user/username "bob"}}
                  {:user/username "alice"}
                  "my-article"))

(defscene comment-card-logged-out
  (c/comment-card {:comment/id 3
                   :comment/body "Anonymous view"
                   :comment/author {:user/username "carol"}}
                  nil
                  "my-article"))

;; tags
(defscene tag-pill-single
  (c/tag-pill "clojure"))

(defscene tag-pill-long-name
  (c/tag-pill "this-is-a-very-long-tag-name"))


(defn main []
  (r/set-dispatch! (fn [replicant-data action-vec]
                     (js/console.log (str "REPLICANT DISPATCH!\n\nActions:\n" action-vec "\n\nData:\n" replicant-data))))
  (p/start!
   {:config 
    {:css-paths ["/styles.css"] ;; the path to compiled CSS file(s)
     :background/default-option-id :dark-mode
     :viewport/defaults {:background/background-color "#fdeddd"}}}))
