(ns com.themis.et.cli.utils.helpers
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]))


;;---------------------------------------------
;;------------- Backoff setup -----------------

;; https://github.com/ctorrisi/franklin/blob/e4a9204d3bade39218ee4c6653308aa1e740f5ee/src/franklin/core.clj#L97
(def ^:private max-retries 5)
(def ^:private base-delay 250)
(def ^:private max-delay 20000)

(defn- backoff
  "Parameters and EqualJitterBackoffStrategy based on `aws-sdk-java`'s
  `com.amazonaws.retry.PredefinedBackoffStrategies`."
  [retries]
  (when (< retries max-retries)
    (let [retries (min retries max-retries)
          delay (min (* (bit-shift-left 1 retries) base-delay) max-delay)
          half-delay (/ delay 2)]
      (+ half-delay (rand-int (+ half-delay 1))))
    ))

;;----------------------------------------------------------------
;;-------------------- response data related ---------------------
;;
(def ^:private pdf-url-template "https://ia37rg02wpsa01.blob.core.windows.net/fek/%s/%s/%s.pdf")

(defn- ->kebab-case
  [mkey]
  (-> mkey
      (str/replace "search_" "")
      (str/replace #"([a-z])([A-Z])" "$1-$2")
      str/lower-case
      keyword))

(defn- string-n->int
  [value]
  (if (and (string? value)
           (re-matches #"\d+" value))
    (Integer/parseInt value)
    value))

(defn- pad-number
  [num width]
  (format (str "%0" width "d") num))

(defn- get-publication-year
  [s]
  (try
    (last (str/split (first (str/split s #" ")) #"/"))
    (catch Exception e
      (log/error "failed to get doc's publication year: " e)
      nil)))

(defn- get-pdf-url
  [m]
  (let [year (get-publication-year (get m "search_PublicationDate")) ;; map has not been transformed yet
        a (pad-number (string-n->int (get m "search_IssueGroupID")) 2)
        b (pad-number (string-n->int (get m "search_DocumentNumber")) 5)
        c (str year a b)]

    (when year
      (format pdf-url-template a year c))))

(defn- transform-map
  [request-id m]
  (into {:request-id request-id
         :doc-url    (get-pdf-url m)}
        (map (fn [[k v]] [(->kebab-case k) (string-n->int v)])
             m)))

(defn transform-collection [{:keys [request-id docs]}]
  (when (and request-id docs)
    (map #(transform-map request-id %) docs)))


(comment
  (transform-collection {:request-id "j87xhggVDHBBUynzKEXQU87hWllorvVIkKrD7Jce808=",
                         :docs       [{"search_ID"              "775654",
                                       "search_DocumentNumber"  "3",
                                       "search_IssueGroupID"    "2",
                                       "search_IssueDate"       "01/07/2025 00:00:00",
                                       "search_PublicationDate" "01/08/2025 00:00:00",
                                       "search_Pages"           "4",
                                       "search_PrimaryLabel"    "Β 3/2025",
                                       "search_Score"           "0"}
                                      {"search_ID"              "776157",
                                       "search_DocumentNumber"  "1",
                                       "search_IssueGroupID"    "11",
                                       "search_IssueDate"       "01/07/2025 00:00:00",
                                       "search_PublicationDate" "01/17/2025 00:00:00",
                                       "search_Pages"           "2",
                                       "search_PrimaryLabel"    "ΠΡΑ.Δ.Ι.Τ. 1/2025",
                                       "search_Score"           "0"}]})
  )