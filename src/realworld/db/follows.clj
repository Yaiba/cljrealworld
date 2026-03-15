(ns realworld.db.follows
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defn following? [ds follower-id followee-id]
  (some? (jdbc/execute-one! ds
                            (sql/format {:select [:*]
                                         :from   [:follows]
                                         :where  [:and
                                                  [:= :follower_id follower-id]
                                                  [:= :followee_id followee-id]]}))))

(defn follow! [ds follower-id followee-id]
  (jdbc/execute-one! ds
                     (sql/format {:insert-into :follows
                                  :values      [{:follower_id follower-id
                                                 :followee_id followee-id}]})))

(defn unfollow! [ds follower-id followee-id]
  (jdbc/execute-one! ds
                     (sql/format {:delete-from :follows
                                  :where       [:and
                                                [:= :follower_id follower-id]
                                                [:= :followee_id followee-id]]})))
