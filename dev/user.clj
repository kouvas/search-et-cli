(ns user
  (:require [org.httpkit.client :as http]
            [com.themis.et.cli.http :as h]))


(h/run-query-and-download-files! {:dates "2025-1-07 2025-01-07"})



(comment
  (import ch.qos.logback.classic.Logger)
  (import ch.qos.logback.classic.Level)
  (.setLevel
    (org.slf4j.LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME)) Level/ALL)


  (def response-map
    {:status "ok"
     :message "Operation completed successfully"
     :data "[{\"search_ID\":\"775654\",\"search_DocumentNumber\":\"3\",\"search_IssueGroupID\":\"2\",\"search_IssueDate\":\"01/07/2025 00:00:00\",\"search_PublicationDate\":\"01/08/2025 00:00:00\",\"search_Pages\":\"4\",\"search_PrimaryLabel\":\"Β 3/2025\",\"search_Score\":\"0\"},{\"search_ID\":\"776157\",\"search_DocumentNumber\":\"1\",\"search_IssueGroupID\":\"11\",\"search_IssueDate\":\"01/07/2025 00:00:00\",\"search_PublicationDate\":\"01/17/2025 00:00:00\",\"search_Pages\":\"2\",\"search_PrimaryLabel\":\"ΠΡΑ.Δ.Ι.Τ. 1/2025\",\"search_Score\":\"0\"}]"})

  (def parsed-response
    (update response-map :data parse-json-string))
  )

(comment
  (json/decode
    (:body @(http/post endpoint
                       {:headers {"Content-Type" "application/json"
                                  "Accept"       "application/json"}
                        :body    (json/encode {:advancedSearch                ""
                                               :selectYear                    ["2025"]
                                               :selectIssue                   []
                                               :documentNumber                ""
                                               :legislationCatalogues         ""
                                               :legislationCataloguesNames    []
                                               :categoryIds                   []
                                               :datePublished                 "2025-01-07 2025-01-07"
                                               :dateReleased                  ""
                                               :entity                        []
                                               :selectedEntitiesSearchHistory []
                                               :tags                          []})
                        })))


  )


(comment
  ;; stuff for debugging

  (require '[portal.api :as p])
  (def p (p/open {:launcher :intellij})) ; jvm / node only
  (add-tap #'p/submit) ; Add portal as a tap> target
  (tap> :hello) ; Start tapping out values

  (require '[dev.nu.morse :as morse])
  (morse/launch-in-proc)
  (morse/inspect {:a 1 :b 2})

  (defn inspect
    "nicer output for reflecting on an object's methods"
    [obj]
    (let [reflection (reflect obj)
          members (sort-by :name (:members reflection))]
      (println "Class:" (.getClass obj))
      (println "Bases:" (:bases reflection))
      (println "---------------------\nConstructors:")
      (doseq [constructor (filter #(instance? clojure.reflect.Constructor %) members)]
        (println (:name constructor) "(" (join ", " (:parameter-types constructor)) ")"))
      (println "---------------------\nMethods:")
      (doseq [method (filter #(instance? clojure.reflect.Method %) members)]
        (println (:name method) "(" (join ", " (:parameter-types method)) ") ;=>" (:return-type method)))))

  (require '[clj-memory-meter.core :as mm])
  (defn measure-memory [obj]
    (mm/measure obj))
  )