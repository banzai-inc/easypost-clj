(ns easypost-clj.webhooks
  (:require [easypost-clj.core :as ep]))

(defrecord Event [])
(defrecord Tracker [])
(defrecord TrackingDetail [])

(defn tracking-detail [& [v]]
  (map->TrackingDetail v))

(defn tracker [& [v]]
  (-> (map->Tracker v)
      (update-in [:tracking_details] (partial map tracking-detail))))

(defmulti transform-result :object)
(defmethod transform-result "Tracker" [v] (tracker v))
(defmethod transform-result "Batch" [v]
  (-> (ep/batch v)
      (update-in
        [:shipments]
        (fn [shipments] (map ep/shipment shipments)))))

(defn event [& [v]]
  (-> (map->Event v)
      (update-in [:result] transform-result)))
