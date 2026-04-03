(ns frontend.schema)

(def schema ;; datascript schema
  {:article/author {:db/valueType :db.type/ref} ; points to ONE user entity
   :article/tags {:db/cardinality :db.cardinality/many} ; multiple strings(not entites - just values)
   :article/comments {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many} ; points to MULTIPLE comment entities
   :article/slug {:db/unique :db.unique/identity}
   :comment/author {:db/valueType :db.type/ref} ; points to ONE user entity
   :comment/article {:db/valueType :db.type/ref} ; points to ONE article entity
   :comment/id {:db/unique :db.unique/identity}
   :user/favorites {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many} ; points to MULTIPLE article entities
   :user/following {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many} ; points to MULTIPLE user entities
   :user/username {:db/unique :db.unique/identity}
   :app/current-user {:db/valueType :db.type/ref} ; points to ONE user entity
   :app/current-profile {:db/valueType :db.type/ref} ; points to ONE user entity
   :app/tags {:db/cardinality :db.cardinality/many}
   })
