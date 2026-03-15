(ns realworld.schema)

(def NewUser
  [:map
   [:email :string]
   [:username :string]
   [:password :string]])

(def LoginCredentials
  [:map
   [:email :string]
   [:password :string]])

