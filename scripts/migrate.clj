(require '[migratus.core :as m]
         '[realworld.db.connection :refer [migration-config]])

(m/migrate migration-config)
(println "Migrations complete!")