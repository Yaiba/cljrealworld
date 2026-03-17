(ns realworld.db.comments
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]))

(def ^:private comment-select
  [[:c.id :id]
   [:c.body :body]
   [:c.created_at :created_at]
   [:c.updated_at :updated_at]
   [:c.author_id :author_id]
   [:u.username :author_username]
   [:u.bio :author_bio]
   [:u.image :author_image]])

(defn find-by-article [ds article-id]
  (jdbc/execute! ds
                 (sql/format {:select   comment-select
                              :from     [[:comments :c]]
                              :join     [[:users :u] [:= :c.author_id :u.id]]
                              :where    [:= :c.article_id article-id]
                              :order-by [[:c.created_at :desc]]})
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-id [ds id]
  (jdbc/execute-one! ds
                     (sql/format {:select comment-select
                                  :from   [[:comments :c]]
                                  :join   [[:users :u] [:= :c.author_id :u.id]]
                                  :where  [:= :c.id id]})
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn create! [ds author-id article-id body]
  (jdbc/execute-one! ds
                     (sql/format {:insert-into :comments
                                  :values      [{:body       body
                                                 :author_id  author-id
                                                 :article_id article-id}]
                                  :returning   [:*]})))

(defn delete! [ds comment-id author-id]
  (jdbc/execute-one! ds
                     (sql/format {:delete-from :comments
                                  :where       [:and
                                                [:= :id comment-id]
                                                [:= :author_id author-id]]})))
