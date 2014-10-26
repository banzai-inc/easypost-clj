(ns easypost-clj.webhooks)

(defrecord Event [])
(defrecord Tracker [])
(defrecord TrackingDetail [])

(defn tracking-detail [& [v]]
  (map->TrackingDetail v))

(defn tracker [& [v]]
  (-> (map->Tracker v)
      (update-in [:tracking_details] (partial map tracking-detail))))

(defn event [& [v]]
  (-> (map->Event v)
      (update-in [:result] tracker)))
