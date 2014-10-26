(ns easypost-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def config {:url "https://api.easypost.com/v2"})

(defprotocol Easypostable
  (root [obj])
  (endpoint [obj])
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

(defn- create*! [obj token]
  (->> (make-request* :post (endpoint obj)
                      (-> {:form-params {(root obj) (into {} obj)}}
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
  (create! [address token]
    (create*! address token))
  
  Parcel
  (root [_] :parcel)
  (endpoint [_] "parcels")
  (create! [parcel token]
    (create*! parcel token))
  
  Shipment
  (root [_] :shipment)
  (endpoint [_] "shipments")

  (create! [shipment token]
    (-> (create*! shipment token)
        (update-in [:rates] (partial map rate))))
  
  Batch
  (root [_] :batch)
  (endpoint [_] "batches")
  (create! [batch token]
    (create*! batch token)))
