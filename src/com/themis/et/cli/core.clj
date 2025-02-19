(ns com.themis.et.cli.core
  "API client for search.et.gr

  Its purpose is:
    - to safely download all document details of the site
    - from document details, it builds the pdf download link for each doc
    - save document details with their pdf download link to json
    - download document pdf files"
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.tools.logging :as log]
            [com.themis.et.cli.http :as http]
            [com.themis.et.cli.utils.dates :as d])
  (:gen-class))

;; https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error ex "Uncaught exception on " (.getName thread)))))

;; ---------------------------------------------------------------------------------



(def CONFIGURATION
  {:command     "fetcher cli"
   :description "A tool to search and download documents from search.et.gr"
   :version     "0.0.1"
   :opts        [{:as     ["Date range to download documents for, yyyy/mm/dd or yyyy/m/d"
                           "Download for single day: \"2025-1-07 2025-01-07\""
                           "Download for long period: \"2024-01-01 2025-01-01\""]
                  :option "dates"
                  :short  "d"
                  :spec ::d/date-range
                  :type   :string}]
   :runs        http/run-query-and-download-files!})

(defn -main [& args]
  (println "==========================================================================================================================================\n")
  (run-cmd args CONFIGURATION))


(comment
  (-main "-d" "2025-1-07 2025-01-07")
  )