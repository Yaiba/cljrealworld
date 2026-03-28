(ns frontend.effects
  (:require [frontend.constants :refer [UI-ENTITY]]
            [frontend.utils :refer [remove-nil-values]]
            [frontend.queries :refer [get-token]]
            [datascript.core :as d]
            [reitit.frontend.easy :as rfe]))

(def local-storage-item "token")

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
       (d/transact! system [[:db/retract UI-ENTITY :app/errors :app/email :app/password]
                            (remove-nil-values {:db/id -1
                                                :user/username (:username user)
                                                :user/email (:email user)
                                                :user/token (:token user)
                                                :user/bio (:bio user)
                                                :user/image (:image user)})
                            {:db/id UI-ENTITY
                             :app/current-user -1
                             :user/token (:token user)}])
       (js/localStorage.setItem local-storage-item (:token user))
       (rfe/push-state :page/home)))
   
   :app/login-failure
   (fn [system resp]
     (let [errors (:errors resp)]
       (d/transact! system [{:db/id UI-ENTITY :app/errors errors}])))
   
   :app/fetch-articles-success
   (fn [system resp]
     (prn "fetch article success ===" (count (:articles resp)))
     (prn "total article count ===" (:articlesCount resp))
     (let [db (d/db system)
           existing-ids (d/q '[:find [?a ...] :where [?a :article/slug _]] db)
           retractions (map (fn [eid] [:db/retractEntity eid]) existing-ids)] 
       ;; TODO: use :http/fetch-article ?
       (d/transact! system (concat retractions
                                   [{:db/id UI-ENTITY :app/articles-count (:articlesCount resp)}]
                                   (mapcat normalize-article (:articles resp))))))
   
   :app/fetch-tags-success
   (fn [system resp]
     (prn "fetch tags success ===" (count (:tags resp)))
     (d/transact! system [{:db/id UI-ENTITY
                           :app/tags (:tags resp)}]))
   
   :app/fetch-current-user-success
   (fn [system resp]
     (let [user (:user resp)]
       (d/transact! system [(remove-nil-values {:db/id -1
                                                :user/username (:username user)
                                                :user/email    (:email user)
                                                :user/token    (:token user)
                                                :user/bio      (:bio user)
                                                :user/image    (:image user)})
                            (remove-nil-values {:db/id UI-ENTITY
                                                :app/current-user -1
                                                })]))) 
                                                ; also save user info in settings namespace for easier editing))
                                                ;;  :settings/username (:username user)
                                                ;; :settings/email (:email user)
                                                ;; :settings/bio (:bio user)
                                                ;; :settings/image (:image user)
   
   :user/update-success
   (fn [system resp]
     (let [user (:user resp)]
       (d/transact! system [(remove-nil-values {:db/id -1
                                                :user/username (:username user)
                                                :user/email    (:email user)
                                                :user/token    (:token user)
                                                :user/bio      (:bio user)
                                                :user/image    (:image user)})
                            {:db/id UI-ENTITY :app/current-user -1}])))
   })

(def effects
  {:route/push-state
   (fn [_ctx _system page]
     (rfe/push-state page))
   :state/transact
   (fn [_ctx system entities]
     (d/transact! system entities))
   :http/request
   (fn [_ctx system {:keys [method url body on-success on-failure]}]
     (let [token (get-token (d/db system))
           headers (cond-> {"Content-type" "application/json"}
                     token (assoc "Authorization" (str "Token " token)))]
       (-> (js/fetch url
                     (clj->js {:method method
                               :headers headers
                               :body (when body
                                       (js/JSON.stringify (clj->js body)))}))
           (.then #(.json %))
           (.then #(let [resp (js->clj % :keywordize-keys true)
                         handler (if (:errors resp)
                                   (get callbacks on-failure)
                                   (get callbacks on-success))]
                     (when handler
                       (handler system resp)))))))

   :user/logout
   (fn [_ctx system]
     (let [db (d/db system)
           user-eid (get-in (d/pull db '[{:app/current-user [:db/id]}] UI-ENTITY)
                            [:app/current-user :db/id])]
       (when user-eid
         (d/transact! system [[:db/retractEntity user-eid]
                              [:db/retract UI-ENTITY :app/current-user]]))
       (js/localStorage.removeItem local-storage-item)
       (rfe/push-state :page/login)))
  
   })