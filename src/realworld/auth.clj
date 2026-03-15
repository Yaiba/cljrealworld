(ns realworld.auth
  (:require [buddy.sign.jwt :as jwt]))

(defn sign-token
  [user secret]
  (jwt/sign {:user-id (:users/id user)
             :username (:users/username user)}
            secret))

(defn verify-token
  [token secret]
  (try
    (jwt/unsign token secret)
    (catch Exception _ nil)))

(defn extract-token
  [req]
  (some-> (get-in req [:headers "authorization"])
          (clojure.string/replace #"^Token " "")))

(defn wrap-auth
  [handler secret]
  (fn [req]
    (let [_identity (some-> (extract-token req)
                           (verify-token secret))]
      (handler (assoc req :identity _identity)))))