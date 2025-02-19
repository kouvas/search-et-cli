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

(defn save-to-json-file
  "Saves a sequence of Clojure maps to a JSON file using data.json"
  [filepath data]
  (let [file (io/file filepath)]
    (io/make-parents file)
    (spit file (json/write-str data :key-fn #(subs (str %) 1)))))

(defn- response->doc-details
  [response]
  (try
    (->> response
         (json/read-str,,,)
         (last,,,)
         (second,,,)
         (json/read-str,,,))
    (catch Exception e
      (log/errorf "Error while parsing response body: %s" e)
      nil)))

(defn- generate-idempotency-key
  [request-body]
  (let [hasher (MessageDigest/getInstance "SHA-256")
        hash-bytes (.digest hasher (.getBytes (str request-body)))]
    (.encodeToString (Base64/getEncoder) hash-bytes)))


(defn- calculate-backoff
  [attempt]
  (let [backoff (min (* (:initial-delay-ms retry-config)
                        (Math/pow 2 attempt))
                     (:max-delay-ms retry-config))
        jitter (* (rand) backoff)]
    (^[float] Math/round jitter)))

(defn- with-retry
  [f & {:keys [max-attempts initial-delay]
        :or   {max-attempts 3}}]
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
            (^[long] Thread/sleep delay)
            (recur (inc attempt)
                   (calculate-backoff attempt)))
          (throw e))
        (:result result)))))

(defn- send-request!
  "Given a date-range string, POSTs it to the search endpoint and returns a seq of document maps."
  [date-range]
  (let [body (json/write-str {:datePublished date-range})
        opts {:body    body
              :headers headers}
        request-id (generate-idempotency-key body)
        start-ts (System/currentTimeMillis)]
    (with-retry
      (fn callback []
        (let [{:keys [status body error]} @(http/post search-api opts)
              duration (- (System/currentTimeMillis) start-ts)]

          (log/infof "request with id %s completed in %d ms" request-id duration)
          (when error
            (throw (ex-info "HTTP POST request error"
                            {:error      error
                             :date-range date-range
                             :request-id request-id})))
          (when-not (= status 200)
            (throw (ex-info "Unexpected HTTP status"
                            {:status     status
                             :date-range date-range
                             :request-id request-id})))

          {:request-id request-id
           :docs       (response->doc-details body)})))))

(defn- download-pdf!
  [target-dir doc-url]
  (with-retry
    (fn []
      (let [filename (last (str/split doc-url #"/"))
            target-file (io/file target-dir filename)]
        (with-open [in (io/input-stream doc-url)]
          (io/copy in target-file))
        (log/infof "Downloaded: %s" (.getAbsolutePath target-file))))
    {:result :ok}))

(defn run-query-and-download-files!
  [{:keys [dates]}]
  (log/info "Running query and downloading files")
  (let [split-to-weeks (d/split-date-range-into-weeks dates)
        start-ts (System/currentTimeMillis)]
    (->> split-to-weeks
         (map send-request!)
         (mapcat h/transform-collection)
         ((fn [in]
            (log/infof "Found %d documents" (count in))
            (save-to-json-file (str "data/" dates ".json") in)
            in))
         (map :doc-url)
         (map #(download-pdf! storage-dir %))
         doall)
    (log/infof "Finished in %d ms" (- (System/currentTimeMillis) start-ts))))


(comment

  (run-query-and-download-files! ["2025-1-07 2025-01-07" "2025-1-08 2025-01-08"])

  (send-request! "2025-1-07 2025-01-07")
  (h/transform-collection (send-request! "2025-1-07 2025-01-07"))
  (download-pdf! "data/docs" "https://ia37rg02wpsa01.blob.core.windows.net/fek/03/2025/20250300002.pdf")
  (->> ["2025-1-07 2025-01-07"]
       (map send-request!)
       (mapcat h/transform-collection)

       ;;(map :doc-url)
       ;;(map #(download-pdf! "data/docs" %))
       )
  )

