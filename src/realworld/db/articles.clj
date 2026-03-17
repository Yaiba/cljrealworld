(ns realworld.db.articles
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]
            [clojure.string :as str]))

;; ── helpers ──────────────────────────────────────────────────────────────────

(defn slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^\w\s-]" "")
      (str/replace #"\s+" "-")))

(def ^:private article-select
  [[:a.id :id]
   [:a.slug :slug]
   [:a.title :title]
   [:a.description :description]
   [:a.body :body]
   [:a.created_at :created_at]
   [:a.updated_at :updated_at]
   [:a.author_id :author_id]
   [:u.username :author_username]
   [:u.bio :author_bio]
   [:u.image :author_image]])

;; ── tags ─────────────────────────────────────────────────────────────────────

(defn find-tags-for-article [ds article-id]
  (->> (jdbc/execute! ds
                      (sql/format {:select   [[:t.name :name]]
                                   :from     [[:tags :t]]
                                   :join     [[:article_tags :at] [:= :at.tag_id :t.id]]
                                   :where    [:= :at.article_id article-id]
                                   :order-by [:t.name]})
                      {:builder-fn rs/as-unqualified-lower-maps})
       (map :name)))

(defn- upsert-tags! [tx tag-names]
  (jdbc/execute! tx
                 (sql/format {:insert-into :tags
                              :values      (mapv (fn [n] {:name n}) tag-names)
                              :on-conflict [:name]
                              :do-nothing  true}))
  (jdbc/execute! tx
                 (sql/format {:select [:id]
                              :from   [:tags]
                              :where  [:in :name tag-names]})))

(defn- set-article-tags! [tx article-id tag-names]
  (jdbc/execute! tx (sql/format {:delete-from :article_tags
                                 :where       [:= :article_id article-id]}))
  (when (seq tag-names)
    (let [tags    (upsert-tags! tx tag-names)
          tag-ids (map :tags/id tags)]
      (jdbc/execute! tx
                     (sql/format {:insert-into :article_tags
                                  :values      (mapv (fn [tid] {:article_id article-id
                                                                :tag_id     tid})
                                                     tag-ids)})))))

;; ── CRUD ─────────────────────────────────────────────────────────────────────

(defn create! [ds author-id {:keys [title description body tagList]}]
  (jdbc/with-transaction [tx ds]
    (let [row        (jdbc/execute-one! tx
                                        (sql/format {:insert-into :articles
                                                     :values      [{:slug        (slugify title)
                                                                    :title       title
                                                                    :description description
                                                                    :body        body
                                                                    :author_id   author-id}]
                                                     :returning   [:*]}))
          article-id (:articles/id row)]
      (set-article-tags! tx article-id tagList)
      row)))

(defn find-by-slug [ds slug]
  (jdbc/execute-one! ds
                     (sql/format {:select article-select
                                  :from   [[:articles :a]]
                                  :join   [[:users :u] [:= :a.author_id :u.id]]
                                  :where  [:= :a.slug slug]})
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn update! [ds slug author-id {:keys [title description body tagList] :as fields}]
  (jdbc/with-transaction [tx ds]
    (let [db-fields (cond-> {:updated_at [:now]}
                      title       (assoc :title title :slug (slugify title))
                      description (assoc :description description)
                      body        (assoc :body body))
          row       (jdbc/execute-one! tx
                                       (sql/format {:update   :articles
                                                    :set      db-fields
                                                    :where    [:and
                                                               [:= :slug slug]
                                                               [:= :author_id author-id]]
                                                    :returning [:*]}))]
      (when (contains? fields :tagList)
        (set-article-tags! tx (:articles/id row) tagList))
      row)))

(defn delete! [ds slug author-id]
  (jdbc/execute-one! ds
                     (sql/format {:delete-from :articles
                                  :where       [:and
                                                [:= :slug slug]
                                                [:= :author_id author-id]]})))

;; ── queries ───────────────────────────────────────────────────────────────────

(defn- build-conditions [{:keys [tag author favorited]}]
  (cond-> []
    tag
    (conj [:in :a.id {:select [:at.article_id]
                      :from   [[:article_tags :at]]
                      :join   [[:tags :t] [:= :t.id :at.tag_id]]
                      :where  [:= :t.name tag]}])
    author
    (conj [:= :u.username author])
    favorited
    (conj [:in :a.id {:select [:f.article_id]
                      :from   [[:favorites :f]]
                      :join   [[:users :fu] [:= :fu.id :f.user_id]]
                      :where  [:= :fu.username favorited]}])))

(defn list-articles [ds {:keys [limit offset] :or {limit 20 offset 0} :as filters}]
  (let [conditions (build-conditions filters)
        query      (cond-> {:select   article-select
                            :from     [[:articles :a]]
                            :join     [[:users :u] [:= :a.author_id :u.id]]
                            :order-by [[:a.created_at :desc]]
                            :limit    limit
                            :offset   offset}
                     (seq conditions) (assoc :where (into [:and] conditions)))]
    (jdbc/execute! ds (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-articles [ds filters]
  (let [conditions (build-conditions filters)
        query      (cond-> {:select [[[:count :*] :count]]
                            :from   [[:articles :a]]
                            :join   [[:users :u] [:= :a.author_id :u.id]]}
                     (seq conditions) (assoc :where (into [:and] conditions)))]
    (:count (jdbc/execute-one! ds (sql/format query)))))

(defn get-feed [ds user-id {:keys [limit offset] :or {limit 20 offset 0}}]
  (jdbc/execute! ds
                 (sql/format {:select   article-select
                              :from     [[:articles :a]]
                              :join     [[:users :u]   [:= :a.author_id :u.id]
                                         [:follows :f] [:= :f.followee_id :a.author_id]]
                              :where    [:= :f.follower_id user-id]
                              :order-by [[:a.created_at :desc]]
                              :limit    limit
                              :offset   offset})
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn count-feed [ds user-id]
  (:count (jdbc/execute-one! ds
                             (sql/format {:select [[[:count :*] :count]]
                                          :from   [[:articles :a]]
                                          :join   [[:follows :f] [:= :f.followee_id :a.author_id]]
                                          :where  [:= :f.follower_id user-id]}))))
