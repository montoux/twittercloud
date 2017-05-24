(ns twittercloud.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.nio.file Paths Path Files)
           (java.nio.charset StandardCharsets)
           (java.io PushbackReader)
           (java.net URLEncoder URLDecoder)
           (java.util Base64)
           (com.google.common.io BaseEncoding)
           (com.twitter.hbc.httpclient.auth OAuth1)
           (org.apache.http.client.methods HttpGet HttpPost)
           (org.apache.http.impl.client DefaultHttpClient)
           (org.apache.http HttpResponse)
           (org.apache.http.client HttpClient)))


(extend-type Path
  io/IOFactory
  (make-reader [^Path p opts]
    (Files/newBufferedReader p StandardCharsets/UTF_8)))


(defn get-auth []
  (let [path (Paths/get (System/getenv "HOME") (into-array String [".twitter" "auth.clj"]))
        auth (PushbackReader. (io/reader path))]
    (read auth)))


(defn sample-tweets [{:keys [consumer-key consumer-secret access-token access-token-secret]}]
  (let [client   (DefaultHttpClient.)
        oauth    (OAuth1. consumer-key consumer-secret access-token access-token-secret)
        req      (HttpGet. "https://stream.twitter.com/1.1/statuses/sample.json")
        _        (.signRequest oauth req "")
        response ^HttpResponse (.execute client req)]
    (when (not= 200 (-> response (.getStatusLine) (.getStatusCode)))
      (throw (IllegalStateException. (.getReasonPhrase (.getStatusLine response)))))
    (.getContent (.getEntity response))))


(defn filter-tweets [{:keys [consumer-key consumer-secret access-token access-token-secret]} filter]
  (let [client   (DefaultHttpClient.)
        oauth    (OAuth1. consumer-key consumer-secret access-token access-token-secret)
        req      (HttpPost. (str "https://stream.twitter.com/1.1/statuses/filter.json?track=" filter))
        _        (.signRequest oauth req "")
        response ^HttpResponse (.execute client req)]
    (when (not= 200 (-> response (.getStatusLine) (.getStatusCode)))
      (throw (IllegalStateException. (.getReasonPhrase (.getStatusLine response)))))
    (.getContent (.getEntity response))
    ))


(defn -main [& args]
  (println "Hello World!"))


#_(with-open [is (sample-tweets (get-auth))
              ir (InputStreamReader. is)
              br (BufferedReader. ir)]
    (transduce
      (comp (filter #(re-find #"\"lang\":\"en\"" %))
            (keep (comp second #(re-find #"text\":\"([^\"]+)\"" %)))
            (mapcat #(str/split % #"\s+"))
            (filter #(re-find #"^#" %))
            (take 100))
      (fn
        ([] (transient {}))
        ([counts x] (assoc! counts x (inc (get counts x 0))))
        ([counts] (persistent! counts)))
      (repeatedly #(.readLine br))
      ))
