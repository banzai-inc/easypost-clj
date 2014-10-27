(ns easypost-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def config {:url "https://api.easypost.com/v2"})

(defprotocol Easypostable
  (root [obj])
  (endpoint [obj])
  (vec-to-params [obj] "Returns a vector of vectors, each element representing a path to a key within the record.")
  (create! [obj token]))

(defrecord Address [])
(defrecord Parcel [])
(defrecord Shipment [])
(defrecord Rate [])
(defrecord Label [])
(defrecord Batch [])

(defn address [& [v]]
  (map->Address v))

(defn parcel [& [v]]
  (map->Parcel v))

(defn shipment [& [v]]
  (map->Shipment v))

(defn rate [& [v]]
  (map->Rate v))

(defn label [& [v]]
  (map->Label v))

(defn batch [& [v]]
  (map->Batch v))

(defmulti process-response (fn [_ method] method))

(defmethod process-response :post [resp _]
  (-> (:body resp)
      (json/parse-string true)))

(defmethod process-response :default [resp _]
  resp)

(defn- make-request* [method endpoint & [opts]]
  (-> (http/request
        (merge {:method method
                :url (str (:url config) "/" endpoint)}
               opts))
      (process-response method)))

(defn- merge-auth [opts token]
  (merge opts {:basic-auth [token ""]}))

(defn- vec-to-params*
  "Takes a vector, returns each element as a key pair
  for form-parms in clj-http.
  [{:id :a} {:id :b}] =>
  {0 {:id :a}
   2 {:id :b}}"
  [v]
  (reduce (fn [memo n] (assoc memo (keyword (str (count (keys memo)))) n)) {} v))

(defn- process-record
  "Prepare record to be converted to form-params"
  [record]
  (let [paths (vec-to-params record)]
    (reduce
      (fn [memo path]
        (update-in memo path vec-to-params*))
      record
      paths)))

(defn- create*!
  "POST request to API"
  [obj token]
  (->> (make-request* :post (endpoint obj)
                      (-> {:form-params {(root obj) (process-record obj)}}
                          (merge-auth token)))
       (merge obj)))

(defn buy!
  "Purchase either a shipment or a batch shipment."
  ([shipment rate token]
   (-> (make-request* :post (str (endpoint shipment) "/" (:id shipment) "/buy")
                      (-> {:form-params {:rate {:id (:id rate)}}}
                          (merge-auth token)))
       (label)))
  ([batch token]
   (-> (make-request* :post (str (endpoint batch) "/" (:id batch) "/buy")
                      (-> {} (merge-auth token))))))

(extend-protocol Easypostable
  Address
  (root [_] :address)
  (endpoint [_] "addresses")
  (vec-to-params [_] [])
  (create! [address token]
    (create*! address token))
  
  Parcel
  (root [_] :parcel)
  (endpoint [_] "parcels")
  (vec-to-params [_] [])
  (create! [parcel token]
    (create*! parcel token))
  
  Shipment
  (root [_] :shipment)
  (endpoint [_] "shipments")
  (vec-to-params [_] [])
  (create! [shipment token]
    (-> (create*! shipment token)
        (update-in [:rates] (partial map rate))))
  
  Batch
  (root [_] :batch)
  (endpoint [_] "batches")
  (vec-to-params [_] [[:shipments]])
  (create! [batch token]
    (create*! batch token)))
