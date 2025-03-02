(ns com.themis.et.cli.http
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.themis.et.cli.utils.dates :as d]
            [com.themis.et.cli.utils.helpers :as h]
            [org.httpkit.client :as http])
  (:import (java.security MessageDigest)
           (java.util Base64)))


(def ^:private storage-dir "data/docs")
(defonce ^:private search-api "https://searchetv99.azurewebsites.net/api/search")
(def ^:private headers {"Content-Type" "application/json"
                        "Accept"       "application/json"
                        "User-Agent"   "Mozilla/5 .0 (Windows NT 10.0 ; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0"})

(def ^:private retry-config
  {:max-duration-ms  (* 10 60 1000)
   :initial-delay-ms 1000
   :max-delay-ms     (* 20 1000)
   :expected-status  200})

;; Pure functions

(defn- generate-idempotency-key
  "Generate a unique key for a request based on its body (pure function)"
  [request-body]
  (let [hasher (MessageDigest/getInstance "SHA-256")
        hash-bytes (.digest hasher (.getBytes (str request-body)))]
    (.encodeToString (Base64/getEncoder) hash-bytes)))

(defn- calculate-backoff
  "Calculate backoff time with jitter (pure function)"
  [attempt]
  (let [backoff (min (* (:initial-delay-ms retry-config)
                        (Math/pow 2 attempt))
                     (:max-delay-ms retry-config))
        jitter (* (rand) backoff)]
    (^[float] Math/round jitter)))

(defn- response->doc-details
  "Extract document details from response (pure function)"
  [response]
  (when response
    (->> response
         (json/read-str,,,)
         (last,,,)
         (second,,,)
         (json/read-str,,,))))

(defn- create-request-body
  [date-range]
  (json/write-str {:datePublished date-range}))

(defn- create-request-options
  [body]
  {:body    body
   :headers headers})

(defn- create-error-info
  [error status date-range request-id]
  (cond
    error {:error      error
           :date-range date-range
           :request-id request-id}
    (not= status 200) {:status     status
                       :date-range date-range
                       :request-id request-id}))

(defn- create-response-data
  [request-id body]
  {:request-id request-id
   :docs       (response->doc-details body)})

(defn- create-pdf-filename
  [doc-url]
  (when doc-url
    (last (str/split doc-url #"/"))))

(defn- create-target-file-path
  [target-dir doc-url]
  (when doc-url
    (let [filename (create-pdf-filename doc-url)]
      (when filename
        (io/file target-dir filename)))))

(defn- create-json-filepath
  [dates]
  (str "data/" dates ".json"))


;; Side-effect functions

(defn save-to-json-file!
  [filepath data]
  (let [file (io/file filepath)]
    (io/make-parents file)
    (spit file (json/write-str data :key-fn #(subs (str %) 1)))))

(defn- sleep!
  [duration]
  (^[long] Thread/sleep duration))

(defn- with-retry
  [f & {:keys [max-attempts initial-delay]
        :or   {max-attempts 3
               initial-delay (:initial-delay-ms retry-config)}}]
  (loop [attempt 1
         delay initial-delay]
    (let [result (try
                   {:result (f)}
                   (catch Exception e
                     {:exception e}))]
      (if-let [e (:exception result)]
        (if (< attempt max-attempts)
          (do
            (log/error (str "Attempt " attempt " failed: " (.getMessage e))
                       "- retrying in" delay "ms")
            (sleep! delay)
            (recur (inc attempt)
                   (calculate-backoff attempt)))
          (throw e))
        (:result result)))))

(defn- execute-http-request!
  "Execute HTTP request with retry (side effect)"
  [url options request-id date-range]
  (let [start-ts (System/currentTimeMillis)]
    (with-retry
      (fn callback []
        (let [{:keys [status body error]} @(http/post url options)
              duration (- (System/currentTimeMillis) start-ts)]
          
          (log/infof "request with id %s completed in %d ms" request-id duration)
          
          (when-let [error-info (create-error-info error status date-range request-id)]
            (throw (ex-info (if error "HTTP POST request error" "Unexpected HTTP status")
                            error-info)))
          
          (create-response-data request-id body))))))

(defn- download-file!
  "Download a file from URL to target directory (side effect)"
  [target-dir doc-url]
  (when doc-url
    (with-retry
      (fn []
        (let [target-file (create-target-file-path target-dir doc-url)]
          (when target-file
            (with-open [in (io/input-stream doc-url)]
              (io/copy in target-file))
            (log/infof "Downloaded: %s" (.getAbsolutePath target-file)))))
      :initial-delay (:initial-delay-ms retry-config)
      :max-attempts 3)))

;; Main API functions

(defn send-request!
  "Given a date-range string, POSTs it to the search endpoint and returns a seq of document maps."
  [date-range]
  (let [body (create-request-body date-range)
        options (create-request-options body)
        request-id (generate-idempotency-key body)]
    (execute-http-request! search-api options request-id date-range)))

(defn download-pdf!
  "Download a PDF file from the given URL"
  [target-dir doc-url]
  (download-file! target-dir doc-url))

(defn query-documents
  "Query documents for the given date range (pure function pipeline)"
  [date-range]
  (->> date-range
       (d/split-date-range-into-weeks)
       (mapv send-request!)
       (mapcat h/transform-collection)
       doall))

(defn save-documents!
  "Save documents to JSON file (side effect)"
  [documents dates]
  (let [filepath (create-json-filepath dates)]
    (log/infof "Found %d documents" (count documents))
    (save-to-json-file! filepath documents)
    documents))

(defn download-documents!
  "Download documents from their URLs (side effect)"
  [documents]
  (->> documents
       (map :doc-url)
       (filter identity)
       (map #(download-pdf! storage-dir %))
       doall))

(defn run-query-and-download-files!
  "Main function to run query and download files"
  [{:keys [dates]}]
  (log/info "Running query and downloading files")
  (let [start-ts (System/currentTimeMillis)]
    (-> dates
        (query-documents)
        (#(save-documents! % dates))
        (download-documents!))
    (log/infof "Finished in %d ms" (- (System/currentTimeMillis) start-ts))
    ))


(comment

  (run-query-and-download-files! {:dates "2025-1-07 2025-01-07"})

  (send-request! "2025-1-07 2025-01-07")
  (h/transform-collection (send-request! "2025-1-07 2025-01-07"))
  (download-pdf! "data/docs" "https://ia37rg02wpsa01.blob.core.windows.net/fek/03/2025/20250300002.pdf")
  (->> ["2025-1-07 2025-01-07"]
       (map send-request!)
       (mapcat h/transform-collection)

       ;;(map :doc-url)
       ;;(map #(download-pdf! storage-dir %))
       )
  )

