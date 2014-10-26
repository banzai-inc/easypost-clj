(ns easypost-clj.core-test
  (:require [clojure.test :refer :all]
            [easypost-clj.core :as core]
            [environ.core :as e]))

;; Before running tests, make sure you've set 
;; the :easypost-api-key using environ (https://github.com/weavejester/environ)

(def token (e/env :easypost-api-key))

(def from (core/address {:company "Banzai, Inc."
                         :street1 "2545 N. Canyon Rd."
                         :street2 "Ste. 210"
                         :city "Provo"
                         :state "UT"
                         :zip "84604"
                         :phone "888.822.6924"}))

(def to (core/address {:name "Johnny Depp"
                       :street1 "189 S Chestnut St."
                       :city "Philadelphia"
                       :state "PA"
                       :zip "15003"
                       :phone "888.822.6924"}))

(def parcel (core/parcel {:length 9
                          :width 6
                          :height 2
                          :weight 10}))

(def shipment (core/shipment {:to_address to
                              :from_address from
                              :parcel parcel}))

(def batch (core/batch {:shipment shipment}))

(deftest address-test
  (let [address (-> from
                    (core/create! token))]
    (is (contains? address :created_at))))

(deftest parcel-test
  (let [parcel (-> parcel
                   (core/create! token))]
    (is (contains? parcel :created_at))))

(deftest shipment-test
  (let [shipment (-> shipment
                     (core/create! token))]
    (is (contains? shipment :created_at))
    (is (= easypost_clj.core.Rate (type (first (:rates shipment)))))))

(deftest label-test
  (let [shipment (-> shipment
                     (core/create! token))
        rate (first (:rates shipment))
        label (core/buy! shipment rate token)]
    (is (contains? label :postage_label))))

(deftest batch-test
  (let [batch (-> batch
                  (core/create! token))]
    (testing "create"
      (is (contains? batch :created_at)))
    
    (testing "buy"
      (let [batch (-> batch
                      (core/create! token)
                      (core/buy! token))]
        (is (= "created" (:state #spy/p batch)))))))
