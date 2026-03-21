(ns realworld.schema)

(def NewUser
  [:map
   [:email [:string {:min 1 :error/message "can't be blank"}]]
   [:username [:string {:min 1 :error/message "can't be blank"}]]
   [:password [:string {:min 1 :error/message "can't be blank"}]]])

(def UpdateUser
  [:map
   [:email    {:optional true} [:string {:min 1 :error/message "can't be blank"}]]
   [:username {:optional true} [:string {:min 1 :error/message "can't be blank"}]]
   [:bio      {:optional true} [:maybe :string]]
   [:image    {:optional true} [:maybe :string]]
   [:password {:optional true} [:string {:min 1 :error/message "can't be blank"}]]])

(def LoginCredentials
  [:map
   [:email [:string {:min 1 :error/message "can't be blank"}]]
   [:password [:string {:min 1 :error/message "can't be blank"}]]])

(def NewArticle
  [:map
   [:title [:string {:min 1 :error/message "can't be blank"}]]
   [:description [:string {:min 1 :error/message "can't be blank"}]]
   [:body [:string {:min 1 :error/message "can't be blank"}]]
   [:tagList {:optional true} [:vector :string]]])

(def UpdateArticle
  [:map
   [:title       {:optional true} [:string {:min 1 :error/message "can't be blank"}]]
   [:description {:optional true} [:string {:min 1 :error/message "can't be blank"}]]
   [:body        {:optional true} [:string {:min 1 :error/message "can't be blank"}]]
   [:tagList     {:optional true} [:vector :string]]])

(def NewComment
  [:map
   [:body [:string {:min 1 :error/message "can't be blank"}]]])

(def ArticleFilters
  [:map {:closed false}
   [:tag       {:optional true} :string]
   [:author    {:optional true} :string]
   [:favorited {:optional true} :string]
   [:limit     {:optional true} [:maybe int?]]
   [:offset    {:optional true} [:maybe int?]]])

(def FeedFilters
  [:map {:closed false}
   [:limit  {:optional true} [:maybe int?]]
   [:offset {:optional true} [:maybe int?]]])
