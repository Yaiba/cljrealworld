(ns realworld.db.favorites
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defn favorited? [ds user-id article-id]
  (some? (jdbc/execute-one! ds
                            (sql/format {:select [:*]
                                         :from   [:favorites]
                                         :where  [:and
                                                  [:= :user_id user-id]
                                                  [:= :article_id article-id]]}))))

(defn favorites-count [ds article-id]
  (:count (jdbc/execute-one! ds
                             (sql/format {:select [[[:count :*] :count]]
                                          :from   [:favorites]
                                          :where  [:= :article_id article-id]}))))

(defn favorite! [ds user-id article-id]
  (jdbc/execute-one! ds
                     (sql/format {:insert-into :favorites
                                  :values      [{:user_id user-id :article_id article-id}]
                                  :on-conflict [:user_id :article_id]
                                  :do-nothing  true})))

(defn unfavorite! [ds user-id article-id]
  (jdbc/execute-one! ds
                     (sql/format {:delete-from :favorites
                                  :where       [:and
                                                [:= :user_id user-id]
                                                [:= :article_id article-id]]})))
