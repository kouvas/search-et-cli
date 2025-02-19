A cli client to download pdf documents from https://search.et.gr/en/advanced-search. 
It's meant for mass download of documents, based on a given date range, for now.


```shell
# if not on apple silicon you need to install clojure and build it with:
❯ clj -T:build ci

#if on apple silicon and java installed:
❯ chmod +x ./run.sh

❯ ./run.sh --dates "2025-1-07 2025-01-07"
...
...
00:33:03,322 |-INFO in ch.qos.logback.classic.util.ContextInitializer@47be0f9b - ch.qos.logback.classic.util.DefaultJoranConfigurator.configure() call lasted 58 milliseconds. ExecutionStatus=DO_NOT_INVOKE_NEXT_IF_ANY

==========================================================================================================================================

00:33:03.355 [main] INFO  com.themis.et.cli.http - Running query and downloading files
00:33:04.276 [main] INFO  com.themis.et.cli.http - request with id rI6g9HzSLkJZsB0CLjhB57B/XmRpoTXDe34hs74O2XE= completed in 917 ms
00:33:04.863 [main] INFO  com.themis.et.cli.http - request with id HcwCHH+UmAoGI4G1UhKBd2d8zGMzUR9XLse0P8FkXgk= completed in 585 ms
00:33:05.393 [main] INFO  com.themis.et.cli.http - request with id /zU8OZm/8C32ITn/YXtDD8sl2aBfh0X7JUGzkSIb/08= completed in 518 ms
00:33:05.707 [main] INFO  com.themis.et.cli.http - request with id 7RdfatBeKHV0XzaJQHzCBX0YqZeugIzBbEY4eZBbw7k= completed in 293 ms
00:33:06.011 [main] INFO  com.themis.et.cli.http - request with id leZdZKb1BiD9D5eU5fFBj3vm+gmLD5izSt26dimyWEk= completed in 295 ms
00:33:06.051 [main] INFO  com.themis.et.cli.http - Found 675 documents
00:33:06.929 [main] INFO  com.themis.et.cli.http - Downloaded: /Users/kou/workspaces/themis/src/search-et-cli/data/docs/20250200001.pdf
00:33:07.136 [main] INFO  com.themis.et.cli.http - Downloaded: /Users/kou/workspaces/themis/src/search-et-cli/data/docs/20250200002.pdf
...
...
00:35:28.134 [main] INFO  com.themis.et.cli.http - Downloaded: /Users/kou/workspaces/themis/src/search-et-cli/data/docs/20250400030.pdf
00:35:28.435 [main] INFO  com.themis.et.cli.http - Downloaded: /Users/kou/workspaces/themis/src/search-et-cli/data/docs/20251400043.pdf
00:35:28.747 [main] INFO  com.themis.et.cli.http - Downloaded: /Users/kou/workspaces/themis/src/search-et-cli/data/docs/20251400044.pdf
00:35:28.747 [main] INFO  com.themis.et.cli.http - Finished in 145390 ms

themis/src/search-et-cli via ☕ v21.0.6 took 2m26s 
❯ ls ./data/docs  | wc -l 
     675

themis/src/search-et-cli via ☕ v21.0.6 
❯ 
```

```shell
❯ ./run.sh --dates "2025-13-07 2025-01-07"             
Found JAR file: ./target/com.themis/et-cli-0.1.0-SNAPSHOT.jar
Starting application...
....
==========================================================================================================================================

01:04:48.619 [main] ERROR com.themis.et.cli.utils.dates - Failed to parse date: Text '2025-13-07' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13
** ERROR: **
Option error: Spec failure for option 'dates': with value '2025-13-07 2025-01-07' got java.time.format.DateTimeParseException: Text '2025-13-07' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13


NAME:
 fetcher cli - A tool to search and download documents from search.et.gr

USAGE:
  fetcher cli [command options] [arguments...]

VERSION:
 0.0.1

OPTIONS:
   -d, --dates S  Date range to download documents for, yyyy/mm/dd or yyyy/m/d
                  Download for single day: "2025-1-07 2025-01-07"
                  Download for long period: "2024-01-01 2025-01-01"
   -?, --help

```
```shell
❯ ./run.sh --dates 2025-13-07             
Found JAR file: ./target/com.themis/et-cli-0.1.0-SNAPSHOT.jar
Starting application...
....
==========================================================================================================================================

** ERROR: **
Option error: Spec failure for option 'dates'
-- Spec failed --------------------

  "2025-13-07"

This should be string of 2 dates, either yyyy/mm/dd or yyyy/m/d.
Valid examples:

                                 "2025-01-07 2025-01-07"
                                 "2025-1-07 2025-01-13"
                                 "2025-01-07 2025-01-9"
Invalid examples:

                                 "2025-13-07 2025-01-13"
                                 "2025-11-06 2025-12-32"

-- Relevant specs -------

:com.themis.et.cli.utils.dates/date-range:
  (clojure.spec.alpha/and
   clojure.core/string?
   com.themis.et.cli.utils.dates/valid-date-range-regex
   (clojure.core/fn
    [date-str]
    (clojure.core/let
     [[date1 date2] (clojure.string/split date-str #" ")]
     (clojure.core/and
      (com.themis.et.cli.utils.dates/valid-date? date1)
      (com.themis.et.cli.utils.dates/valid-date? date2)))))

-------------------------
Detected 1 error



NAME:
 fetcher cli - A tool to search and download documents from search.et.gr
 ```