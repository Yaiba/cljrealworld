(ns frontend.core
  (:require [replicant.dom :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [nexus.core :as nexus]
            [datascript.core :as d]
            [frontend.views.ui :as ui]
            [frontend.schema :refer [schema]]
            [frontend.constants :refer [UI-ENTITY]]
            [frontend.actions :refer [actions]]
            [frontend.effects :refer [effects local-storage-item]]
            [frontend.queries :as qs]
            ))

(def app-nexus
  {:nexus/system->state (fn [_conn] (d/db _conn))
   :nexus/placeholders
   {:event.target/value
    (fn [{:replicant/keys [dom-event]}]
      (some-> dom-event .-target .-value))}

   :nexus/actions actions
   :nexus/effects effects
})

(def routes
  [["/" :page/home]
   ["/login" :page/login]
   ["/register" :page/register]
   ["/settings" :page/settings]
   ])

(defn render [state]
  (r/render (js/document.getElementById "app")
            (ui/render-ui state)))

;; let's define the terms
;; a 'state' is the snapshot of a 'store'(atom or datascript conn)

(defn init [store]
  ;; watch state, for datascript
  (d/listen! store ::render
             (fn [{:keys [db-after]}]
               (render db-after)))

  ;; init state
  (d/transact! store [{:app/loaded-at (.getTime (js/Date.))}])

  ;; wire up nexus
  (r/set-dispatch!
   (fn [replicant-data action-vec]
     (nexus/dispatch app-nexus store replicant-data [action-vec])))
  
  ;; restore user
  (when-let [token (js/localStorage.getItem local-storage-item)]
    (d/transact! store [{:db/id -1 :user/token token}
                        {:db/id UI-ENTITY :app/current-user -1}])
    (nexus/dispatch app-nexus store {} [[:app/fetch-current-user]]))

  ;; router
  (rfe/start!
   (rf/router routes)
   (fn [match _history]
     (when match
       (let [page (-> match :data :name)]
         (prn "router match - " page)
         (d/transact! store [{:db/id UI-ENTITY :app/page page}])
         (condp = page
           :page/home
           (nexus/dispatch app-nexus store {} [[:app/fetch-articles]
                                               [:app/fetch-tags]])
           :page/login
           (when (qs/get-token (d/db store))
             (rfe/push-state :page/home))
           :page/settings
           (nexus/dispatch app-nexus store {} [[:settings/init]])
           ))))
   {:use-fragment false}))

;; -------- setup ---------
;; If we are to have a dev.cljs, below could be the file
(defonce conn (d/create-conn schema))

(defn main []
  (init conn)
  (println "Loaded!"))

(defn ^:frontend.user/after-load re-render
  "Re render callback after shadow-cljs hot reload, this only re-render without 
   re-initialing stuff."
  []
  (render (d/db conn))
  (println "Reloaded!"))


(comment
  (qs/get-entity (d/db conn) UI-ENTITY)
  )