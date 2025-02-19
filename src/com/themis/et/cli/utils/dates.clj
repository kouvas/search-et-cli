(ns com.themis.et.cli.utils.dates
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [expound.alpha :as ex])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter DateTimeParseException)))


(def in-date-formater (DateTimeFormatter/ofPattern "yyyy-M-d"))
(def out-date-formater (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
(def valid-date-range-regex #(re-matches #"\d{4}-\d{1,2}-\d{1,2} \d{4}-\d{1,2}-\d{1,2}" %))


(defn ^:private parse-date [date-str]
  (try
    (LocalDate/parse date-str in-date-formater)
    (catch DateTimeParseException e
      (log/errorf "Failed to parse date: %s" (Exception/.getMessage e))
      (throw e))))

(defn ^:private valid-date? [s]
  (parse-date s))

(s/def ::date-range
  (s/and
    string?
    valid-date-range-regex
    (fn [date-str]
      (let [[date1 date2] (str/split date-str #" ")]
        (and (valid-date? date1)
             (valid-date? date2))))))

(ex/defmsg ::date-range (str "This should be string of 2 dates, either yyyy/mm/dd or yyyy/m/d. \n"
                             "Valid examples:\n
                             \t \"2025-01-07 2025-01-07\"
                             \t \"2025-1-07 2025-01-13\"
                             \t \"2025-01-07 2025-01-9\"\n"
                             "Invalid examples:\n
                             \t \"2025-13-07 2025-01-13\"
                             \t \"2025-11-06 2025-12-32\""))

(defn ^:private format-date
  [date]
  (LocalDate/.format date out-date-formater))

(comment
  (parse-date "2025-13-20")
  (valid-date? "2025-1-3sadf")
  (s/valid? ::date-range "2025-2-7 2025-1-07")
  (LocalDate/parse "2025-1-3" in-date-formater)
  (format-date (parse-date "2025-1-20"))
  )

(defn ^:private generate-week-range
  "generate a single week range starting from start-date, bounded by end-date"
  [^LocalDate start-date ^LocalDate end-date]
  (let [week-end (.plusDays start-date 6)                  
        adjusted-end (if (.isAfter week-end end-date) end-date week-end)]
    (str (format-date start-date) " " (format-date adjusted-end))))

(defn ^:private generate-weekly-ranges
  "Generate all weekly ranges between start and end dates"
  [^LocalDate start-date ^LocalDate end-date]
  (->> (iterate #(.plusDays % 7) start-date)
       (take-while #(not (.isAfter % end-date)))
       (mapv #(generate-week-range % end-date))))

(defn ^:private validate-dates
  [^LocalDate start-date ^LocalDate end-date]
  (when (LocalDate/.isAfter start-date end-date)
    (throw (IllegalArgumentException. (str "Start date " start-date " cannot be after end date " end-date)))))

(comment
  (LocalDate/.isAfter (parse-date "2025-01-07") (parse-date "2025-01-20"))
  (validate-dates (parse-date "2025-01-07") (parse-date "2025-01-20"))
  )

(defn split-date-range-into-weeks
  "api returns max of 1.9MB data, which based on a quick test correspond to
  roughly 3-5 months of data. Safest approach to query for very long periods of time
  is to split a given date range into weekly date ranges and do multiple queries instead"
  [date-range-str]
  (try
    (let [[start-str end-str] (str/split date-range-str #" ")
          start-date (parse-date start-str)
          end-date (parse-date end-str)]
      (validate-dates start-date end-date)
      (generate-weekly-ranges start-date end-date))
    (catch Exception e
      (log/error e)
      (throw e))))                                          ;; todo re-think: propagate error from it's private fns, or create new type of error?


(comment
  (split-date-range-into-weeks "2025-1-7 2025-1-20")
  (split-date-range-into-weeks "2025-02-28 2025-03-05")

  (split-date-range-into-weeks "invalid 2025-03-01")
  ;;=> Execution error (DateTimeParseException) at java.time.format.DateTimeFormatter/parseResolved0 (DateTimeFormatter.java:2108).
  ;;Text 'invalid' could not be parsed at index 0

  (split-date-range-into-weeks "2025-13-1 2025-1-1")
  ;; "Text '2025-13-1' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13"

  (split-date-range-into-weeks "2025-1-1 2024-1-1")
  ;;=> Execution error (IllegalArgumentException) at com.themis.fetcher.utils.dates/validate-dates (dates.clj:63).
  ;;   Start date 2025-01-01 cannot be after end date 2024-01-01

  )