(require '[babashka.http-client :as http]
         '[cheshire.core :as json])

(def failures (atom 0))

(defn check [label ok?]
  (if ok?
    (println "✓" label)
    (do (println "✗" label)
        (swap! failures inc))))

(def register-info {:user
                    {:username "smoketest"
                     :email "smoketest@test.com"
                     :password "password123"}})
(def login-info {:user
                {:email "smoketest@test.com"
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

(check "Registration/login request should succeed" (or (= 201 (:status response)) (= 200 (:status response))))

(def token (-> response
               :body
               (json/parse-string true)
               :user
               :token))
(check "Response should contain a token" token)


(let [response (http/get "http://localhost:3000/api/articles"
                         {:headers {"Authorization" (str "Token " token)}
                          :throw false})]
  (check "Should be able to access protected endpoint" (= 200 (:status response)))
  (check "User has no articles" (>= (-> response :body (json/parse-string true) :articlesCount) 0)))

(let [response (http/get "http://localhost:3000/api/tags"
                         {:throw false})]
  (check "Should be able to access public endpoint" (= 200 (:status response)))
  (check "Tags should be an array" (vector? (-> response :body (json/parse-string true) :tags))))

(when (pos? @failures)
  (System/exit 1))
