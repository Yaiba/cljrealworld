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

(def NewArticle
  [:map
   [:title :string]
   [:description :string]
   [:body :string]
   [:tagList {:optional true} [:vector :string]]])

(def UpdateArticle
  [:map
   [:title       {:optional true} :string]
   [:description {:optional true} :string]
   [:body        {:optional true} :string]
   [:tagList     {:optional true} [:vector :string]]])

(def NewComment
  [:map
   [:body :string]])

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
