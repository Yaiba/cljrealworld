(require '[babashka.http-client :as http]
         '[cheshire.core :as json])

(def register-info {:user
                    {:username "seed"
                     :email "seed@test.com"
                     :password "password123"}})
(def login-info {:user
                 {:email "seed@test.com"
                  :password "password123"}})

(def response
  (let [register-resp (http/post "http://localhost:3000/api/users"
                                 {:headers {"Content-Type" "application/json"}
                                  :body (json/generate-string register-info)
                                  :throw false})]
    (cond
      (= 201 (:status register-resp)) (do (println "User registered successfully")
                                           register-resp)
      (= 409 (:status register-resp)) (do (println "User already exists, proceeding with seeding")
                                           (http/post "http://localhost:3000/api/users/login"
                                                      {:headers {"Content-Type" "application/json"}
                                                       :body (json/generate-string login-info)
                                                       :throw false}))
      :else (do (println "Unexpected error: " (:status register-resp))
                nil))))

(def token (-> response
               :body
               (json/parse-string true)
               :user
               :token))

(defn post-article [{:keys [title description body tags]}]
  (http/post "http://localhost:3000/api/articles"
             {:headers {"Content-Type" "application/json"
                        "Authorization" (str "Token " token)}
              :throw false
              :body (json/generate-string
                     {:article
                      {:title title
                       :description description
                       :body body
                       :tagList tags}})}))

(def slug1
  (let [resp (post-article {:title "Test Article"
                            :description "This is a test article"
                            :body "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
                            :tags ["test" "clojure"]})]
    (-> resp
        :body
        (json/parse-string true)
        :article
        :slug)))

(post-article {:title "Another Test Article"
               :description "This is another test article"
               :body "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
               :tags ["test" "clojure"]})

(post-article {:title "Third Test Article"
               :description "This is the third test article"
               :body "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
               :tags ["test" "clojure"]})


(http/post (str "http://localhost:3000/api/articles/" slug1 "/comments")
           {:headers {"Content-Type" "application/json"
                      "Authorization" (str "Token " token)}
            :throw false
            :body (json/generate-string
                   {:comment
                    {:body "This is a test comment"}})})