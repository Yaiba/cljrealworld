(ns realworld.db)

(defonce app-db
  (atom {:users {}
         :tags ["clojure" "functional" "repl"]}))

(defn create-user!
  [user]
  (swap! app-db update :users assoc (:email user) user))

(defn find-user-by-email
  [email]
  (get-in @app-db [:users email]))

(comment
  (create-user! {:email "test@test.com" :username "test" :password "secret"})
  (find-user-by-email "test@test.com")
  (@app-db) 
  )