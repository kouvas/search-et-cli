(ns com.themis.fetcher-cli-test
  (:require [clojure.test :refer :all]
            [com.themis.et.cli.utils.dates :as d]
            [com.themis.et.cli.utils.helpers :as h])
  (:import (java.time.format DateTimeParseException)))


(def transform-collection-input {:request-id "j87xhggVDHBBUynzKEXQU87hWllorvVIkKrD7Jce808=",
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

(def transform-collection-output '({:request-id       "j87xhggVDHBBUynzKEXQU87hWllorvVIkKrD7Jce808=",
                                    :issue-group-id   2,
                                    :issue-date       "01/07/2025 00:00:00",
                                    :doc-url          "https://ia37rg02wpsa01.blob.core.windows.net/fek/02/2025/20250200003.pdf",
                                    :pages            4,
                                    :publication-date "01/08/2025 00:00:00",
                                    :document-number  3,
                                    :id               775654,
                                    :score            0,
                                    :primary-label    "Β 3/2025"}
                                   {:request-id       "j87xhggVDHBBUynzKEXQU87hWllorvVIkKrD7Jce808=",
                                    :issue-group-id   11,
                                    :issue-date       "01/07/2025 00:00:00",
                                    :doc-url          "https://ia37rg02wpsa01.blob.core.windows.net/fek/11/2025/20251100001.pdf",
                                    :pages            2,
                                    :publication-date "01/17/2025 00:00:00",
                                    :document-number  1,
                                    :id               776157,
                                    :score            0,
                                    :primary-label    "ΠΡΑ.Δ.Ι.Τ. 1/2025"}))

(deftest test-date-range->weekly
  (testing "Testing a given date range, is properly split into weekly date ranges")
  (is (= (d/split-date-range-into-weeks "2025-1-7 2025-1-20") ["2025-01-07 2025-01-13" "2025-01-14 2025-01-20"]))
  (is (= (d/split-date-range-into-weeks "2025-02-28 2025-03-05") ["2025-02-28 2025-03-05"])))

(deftest test-date-range->weekly_ReturnsExpectedError
  (testing "Testing a given an invalid input as date string is returning expected error")
  (is (thrown-with-msg?
        DateTimeParseException
        #"Text 'invalid' could not be parsed at index 0"
        (d/split-date-range-into-weeks "invalid 2025-03-01")))
  (is (thrown-with-msg?
        DateTimeParseException
        #"Text '2025-13-1' could not be parsed: Invalid value for MonthOfYear \(valid values 1 - 12\): 13" ;; esc ( & )
        (d/split-date-range-into-weeks "2025-13-1 2025-1-1")))
  (is (thrown-with-msg?
        IllegalArgumentException
        #"Start date 2025-01-01 cannot be after end date 2024-01-01"
        (d/split-date-range-into-weeks "2025-1-1 2024-1-1"))))

(deftest test-transform-collection
  (testing "Testing api response is transformed as expected")
  (is (= (h/transform-collection transform-collection-input) transform-collection-output)))