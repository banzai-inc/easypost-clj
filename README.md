# easypost-clj

A simple Clojure library for interacting with [Easypost's](https://www.easypost.com/getting-started) shipping API.

## Install

Add `easypost-clj` to `project.clj` in your Leiningen project. [Click here to get the newest version.](https://clojars.org/easypost-clj)

## Usage

Easypost-clj is an early-stage library for interfacing with Easypost's elegant shipping API. The library currently supports:

1. Creating records representing Easypost objects, and POST'ing them to Easypost's API.
2. Buying shipping labels and batch shipping labels.

### `easypost-clj.core`

Easypost-clj's functionality roughly corresponds to the tutorials found in [Easypost's Getting Started Guide](https://www.easypost.com/getting-started).

Require the core library, and make sure you have an Easypost API token handy:

```clojure
(require '[easypost-clj.core :as ep])

(def token ...)

=> "abcd1234"
```

#### Addresses

Create addresses.

```clojure
(def from (core/address {:company "Banzai, Inc."
                         :street1 "2545 N. Canyon Rd."
                         :street2 "Ste. 210"
                         :city "Provo"
                         :state "UT"
                         :zip "84604"
                         :phone "888.822.6924"}))
(core/create! from token)
```

Verify addresses. Easypost lets you verify addresses you've already created with an ID. Easypost-clj, however, only supports the Easypost's stateless verification:

```clojure
(def token "abcd123")
(core/verify (core/address {:street1 "159 W 100 S"
                            :city "Springville"
                            :state "Utah"
                            :zip "84663"}) token)
```

#### Parcels

```clojure
(def parcel (core/parcel {:length 9
                          :width 6
                          :height 2
                          :weight 10}))
(core/create! parcel token)
```

#### Shipments

Three API methods for shipments: `show`, `create!`, and `buy!`. `buy!` takes three arguments: a Shipment, a Rate, and your token.

```clojure
(def shipment (-> (core/shipment {:to_address to
                                  :from_address from
                                  :parcel parcel})
                  (core/create! token))

(core/fetch shipment token)

(def rates (:rates shipment))

(core/buy! shipment (first rates) token))
```

#### Batches

```clojure
(def batch (core/batch {:shipments [shipment]}))
(core/create! batch token)

;; Wait, wait, wait for webhook...

(core/buy! batch token)

;; Wait, wait, wait for webhook...

(core/labels! batch token "zpl") ; The file format is optional. Omitting the argument defaults to "pdf".
```

### `easypost-clj.webhooks`

The `easypost-clj.webhooks` namespace provides a few convenience methods for handling Easypost Webhook requests.

```clojure
;; Make sure your request has been converted from string based keys to keywords.
;; If you're using Ring, you should be able to transparently pass the full
;; request into the `event` constructor function.

(def req {:id ...
          :result {:object "Tracker"
                   ...}})

(event req)

=> (easypost_clj.webhooks.Event {...})
```

## License

Copyright © 2014 Banzai, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
