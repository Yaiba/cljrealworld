(ns frontend.core
  (:require [replicant.dom :as d]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [nexus.core :as nexus]))

(def state (atom {:page nil
                  :current-user nil
                  :articles []
                  :errors nil
                  :email ""
                  :password ""}))

(def app-nexus
  {:nexus/system->state deref
   :nexus/placeholders
   {:event.target/value
    (fn [{:replicant/keys [dom-event]}]
      (some-> dom-event .-target .-value))}

   :nexus/actions
   {:app/navigate (fn [_state page]
                    [[:route/push-state page]])
    :app/set-email (fn [_state email]
                     [[:state/assoc :email email]])
    :app/set-password (fn [_state password]
                        [[:state/assoc :password password]])
    :app/login (fn [state]
                 [[:http/post-login (:email state) (:password state)]])}
   
   :nexus/effects
   {:route/push-state (fn [_ctx _system page]
                        (rfe/push-state page))
    :state/assoc (fn [_ctx system k v]
                   (swap! system assoc k v))
    :http/post-login (fn [_ctx system email password]
                       (-> (js/fetch "/api/users/login"
                                     (clj->js {:method "POST"
                                               :headers {"Content-Type" "application/json"}
                                               :body (js/JSON.stringify
                                                      (clj->js {:user {:email email
                                                                       :password password}}))}))
                           (.then #(.json %))
                           (.then #(let [resp (js->clj % :keywordize-keys true)]
                                     (swap! system assoc :current-user (:user resp))
                                     (rfe/push-state :page/home)))))}})

(defn fetch-articles
  []
  (-> (js/fetch "/api/articles")
      (.then #(.json %))
      (.then #(let [resp (js->clj % :keywordize-keys true)]
                ;; TODO: use :http/fetch-article ?
                (swap! state assoc :articles (:articles resp))))))

(comment
  (fetch-articles)
  (count (:articles @state)))

(defn home-page [app-state]
  [:div
   [:h1 "Home"]
   [:ul
    (for [article (:articles app-state)]
      [:li {:key (:slug article)}
       [:h2 (:title article)]
       [:p (:description article)]
       [:small "by " (get-in article [:author :username])]])]
   [:button {:on {:click [:app/navigate :page/login]}} "Go to login"]])

(defn login-page [app-state]
  [:div#login
   [:h1 "Sign in"]
   [:span "Email: "
    [:input {:type "text"
             :value (:email app-state)
             :placeholder "email"
             :on {:input [:app/set-email [:event.target/value]]}}]]
   [:span "Password: "
    [:input {:type "password"
             :value (:password app-state)
             :on {:input [:app/set-password [:event.target/value]]}}]]
   [:button {:on {:click [:app/login]}} "Sign in"]])

(def routes
  [["/" :page/home]
   ["/login" :page/login]])

(defn ui [state]
  (case (:page state)
    :page/home (#'home-page state)
    :page/login (#'login-page state)
    [:div "Loading..."]))

(defn init []
  ;; watch state
  (add-watch state :render
             (fn [_ _ _ new-state]
               (d/render (js/document.getElementById "app")
                         (ui new-state))))
  ;; wire up nexus
  (d/set-dispatch!
   (fn [replicant-data action-vec]
     (nexus/dispatch app-nexus state replicant-data [action-vec])))
  ;; router
  (rfe/start!
   (rf/router routes)
   (fn [match _history]
     (when match
       (let [page (-> match :data :name)]
         (swap! state assoc :page page)
         (when (= page :page/home)
           (fetch-articles)))))
   {:use-fragment false}))