(ns twittercloud.core
  "An example application for demonstrating lazy data processing in Clojure."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer :all])
  (:import (com.twitter.hbc.httpclient.auth OAuth1)
           (java.io InputStream BufferedReader)
           (java.nio.charset StandardCharsets)
           (java.nio.file Files Path Paths)
           (org.apache.http HttpRequest HttpResponse)
           (org.apache.http.impl.client DefaultHttpClient)
           (org.apache.http.client.methods HttpGet HttpPost))
  (:gen-class))


(extend-type Path
  io/IOFactory
  (make-reader [^Path p opts]
    (Files/newBufferedReader p StandardCharsets/UTF_8)))


(defn resolve-path
  "Resolve a file system path."
  [base & more]
  (Paths/get base (into-array String more)))


(defn get-auth
  "Load twitter application secrets from an EDN secret file stored in the user's home directory.
  See https://apps.twitter.com for generating secrets."
  []
  (-> (System/getenv "HOME")
      (resolve-path ".twitter" "auth.clj")
      (slurp)
      (edn/read-string)))


(defn sign
  "Given a twitter authentication map and a HttpRequest, signs and returns the request."
  {:tag HttpRequest}
  [^HttpRequest request auth]
  (let [{:keys [consumer-key consumer-secret access-token access-token-secret]} auth
        oauth (new OAuth1 consumer-key consumer-secret access-token access-token-secret)]
    (.signRequest oauth request "")
    request))


(defn execute
  "Performs an HttpRequest. Returns an InputStream on 200 response, otherwise throws."
  [^HttpRequest request]
  (let [client   (DefaultHttpClient.)
        response ^HttpResponse (.execute client request)
        status   (.getStatusLine response)]
    (when (not= 200 (.getStatusCode status))
      (throw (IllegalStateException. (.getReasonPhrase status))))
    (.getContent (.getEntity response))))


(defn sample-tweets
  "Read tweets from the twitter sample stream, returns an InputStream that must be consumed."
  {:tag InputStream}
  [auth]
  (-> (HttpGet. "https://stream.twitter.com/1.1/statuses/sample.json")
      (sign auth)
      (execute)))


(defn filter-tweets
  "Read tweets from the twitter sample stream, but use a filter to match tweets that include
  our search term."
  {:tag InputStream}
  [auth filter]
  (-> (HttpPost. (str "https://stream.twitter.com/1.1/statuses/filter.json?track=" filter))
      (sign auth)
      (execute)))


(defn create-example
  "Reads the specified number of lines from the input stream into an example. Returns the name of the generated file."
  [^InputStream is times]
  (let [fname (str "example-" times ".json")]
    (with-open [br ^BufferedReader (io/reader is)
                ow (io/writer fname)]
      (binding [*out* ow]
        (dotimes [i times]
          (println (.readLine br)))))
    fname))


(defn lines
  "Returns a lazy sequence of lines from the given reader."
  [^BufferedReader source]
  (lazy-seq
    (when-let [line (.readLine source)]
      (cons line (lines source)))))


(defn count-tag
  "Accumulate tag frequencies by incrementing the current count for the given tag."
  [frequencies tag]
  (update frequencies tag (fnil inc 0)))


(defn tweet-frequencies
  "Returns a map of twitter hash-tag frequencies for a given stream of tweets (as lines of text)."
  [tweets]
  (let [tweets (->> tweets
                    (map json/read-json)                    ;; parse tweets from strings
                    (filter (comp #{"en"} :lang))           ;; keep english tweets
                    (keep :text)                            ;; get text from the tweets
                    )
        tags   (->> tweets
                    ;; (map #(do (println %) %))            ;; debugging: print tweets as they are processed
                    (mapcat #(str/split % #"\s+"))          ;; tokenize words
                    (filter #(re-find #"^#" %))             ;; only keep tags
                    )]
    (reduce count-tag {} tags)))


(defn -main [& {:as args}]
  (let [source (if-let [file (get args "file")]
                 file
                 (sample-tweets (get-auth)))]
    (with-open [in ^BufferedReader (io/reader source)]
      (let [tweets (lines in)
            tweets (if (contains? args "max")
                     (take (Long/parseLong (get args "max")) tweets)
                     tweets)]
        (tweet-frequencies tweets)
        ))))
