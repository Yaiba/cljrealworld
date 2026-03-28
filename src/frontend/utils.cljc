(ns frontend.utils)

(defn remove-nil-values
  [m]
  (into {} (filter (fn [[_ v]] (some? v)) m)))