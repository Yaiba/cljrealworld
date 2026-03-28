(ns frontend.effects
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.utils :refer [remove-nil-values]]
            [frontend.queries :refer [get-token]]
            [datascript.core :as d]
            [reitit.frontend.easy :as rfe]))

(defn normalize-article
  "Normalize a article to two entity maps: author and article."
  [article]
  (let [author (:author article)]
    ;; using :db/id will cause tempid collision, because this func will be used
    ;; to generate from multiple article, thus many entities will have the same
    ;; tempid -1/-2; 
    ;; here we don't use it, since author/article both has a unique attribute
    [(remove-nil-values {;; :db/id -1 
                         :user/username (:username author) ; no :db/id needed - this is unique
                         :user/bio (:bio author)
                         :user/image (:image author)})
     (remove-nil-values {;; :db/id -2
                         :article/slug  (:slug article) ; no :db/id needed - this is unique
                         :article/title (:title article)
                         :article/description (:description article)
                         :article/body (:body article)
                         :article/tags (set (:tagList article))
                         :article/favorites-count (:favoritesCount article)
                         ;; since :user/username is unique, here we can use lookup ref - find the user with this username
                         :article/author [:user/username (:username author)]})]))

(def callbacks
  {:app/login-success
   (fn [system resp]
     (let [user (:user resp)]
       (prn "login success====" resp) ; TODO: errors
       ;; we create a new entity, and link to it in ui-entity
       (d/transact! system [(remove-nil-values {:db/id -1
                                                :user/username (:username user)
                                                :user/email (:email user)
                                                :user/token (:token user)
                                                :user/bio (:bio user)
                                                :user/image (:image user)})
                            {:db/id UI-ENTITY
                             :app/current-user -1
                             :user/token (:token user)}])
       ;; TODO: remove :user/password from the entity?
       (rfe/push-state :page/home)))
   
   :app/fetch-articles-success
   (fn [system resp]
    (prn "fetch article success ===" (count (:articles resp)))
     ;; TODO: use :http/fetch-article ?
     (d/transact! system (mapcat normalize-article (:articles resp))))
   })

(def effects
  {:route/push-state
   (fn [_ctx _system page]
     (rfe/push-state page))
   :state/transact
   (fn [_ctx system entities]
     (d/transact! system entities))
   :http/request
   (fn [_ctx system {:keys [method url body on-success]}]
     (let [token (get-token (d/db system))
           headers (cond-> {"Content-type" "application/json"}
                     token (assoc "Authorization" (str "Token " token)))
           success-handler (get callbacks on-success)]
       (-> (js/fetch url 
                     (clj->js {:method method
                               :headers headers
                               :body (when body
                                       (js/JSON.stringify (clj->js body)))}))
           (.then #(.json %))
           (.then #(success-handler system (js->clj % :keywordize-keys true))))))
      })