(ns easypost-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def config {:url "https://api.easypost.com/v2"})

(defprotocol Easypostable
  (root [obj])
  (endpoint [obj])
  (vec-to-params [obj] "Returns a vector of vectors, each element representing a path to a key within the record.")
  (create! [obj token] "Create object.")
  (fetch [obj token] "Fetch object by id."))

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

;; Not exactly sure why I made this a multimethod
(defmethod process-response :default [resp _]
  (-> (:body resp)
      (json/parse-string true)))

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

(defn- fetch*
  [obj token]
  "GET request for single resource"
  (-> (make-request* :get (str (endpoint obj) "/" (:id obj))
                     (-> {} (merge-auth token)))
      (merge obj)))

(defn- create*!
  "POST request to API"
  [obj token]
  (->> (make-request* :post (endpoint obj)
                      (-> {:form-params {(root obj) (process-record obj)}}
                          (merge-auth token)))
       (merge obj)))

(defn buy!
  "Purchase either a shipment or a batch shipment."
  ([^Shipment shipment rate token]
   (-> (make-request* :post (str (endpoint shipment) "/" (:id shipment) "/buy")
                      (-> {:form-params {:rate {:id (:id rate)}}}
                          (merge-auth token)))
       (label)))
  ([^Batch batch token]
   (make-request* :post (str (endpoint batch) "/" (:id batch) "/buy")
                  (-> {} (merge-auth token)))))

(defn labels!
  "Create labels for a batch."
  [^Batch batch token & [file-format]]
  (make-request* :post (str (endpoint batch) "/" (:id batch) "/label")
                 (-> {:form-params {:file_format (or file-format "pdf")}}
                     (merge-auth token))))

(defn verify
  "Verify address. Requires an Address record."
  [^Address address token]
  (->> (make-request* :post (str (endpoint address) "/verify")
                      (-> {:form-params {(root address) address}}
                          (merge-auth token)))
       (:address)
       (merge address)))

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
  (fetch [shipment token]
    (fetch* shipment token))
  
  Batch
  (root [_] :batch)
  (endpoint [_] "batches")
  (vec-to-params [_] [[:shipments]])
  (create! [batch token]
    (create*! batch token)))
