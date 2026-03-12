(ns realworld.core)

(defn top-authors
  ([articles] (top-authors articles 3))
  ([articles n]
   (->> articles
        (group-by :author)
        (#(update-vals % count))
        (sort-by val >)
        (take n)
        (map first))))