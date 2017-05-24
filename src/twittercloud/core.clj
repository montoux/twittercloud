(ns twittercloud.core
  (:gen-class)
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
           (org.apache.http.client.methods HttpGet HttpPost)))


(extend-type Path
  io/IOFactory
  (make-reader [^Path p opts]
    (Files/newBufferedReader p StandardCharsets/UTF_8)))


(defn resolve-path [base & more]
  (Paths/get base (into-array String more)))


(defn get-auth []
  (-> (System/getenv "HOME")
      (resolve-path ".twitter" "auth.clj")
      (slurp)
      (edn/read-string)))


(defn sign
  "Given a twitter authentication map and a HttpRequest, signs and returns the request."
  {:tag HttpRequest}
  [^HttpRequest request auth]
  (let [{:keys [consumer-key consumer-secret access-token access-token-secret]} auth
        oauth (OAuth1. consumer-key consumer-secret access-token access-token-secret)]
    (.signRequest oauth request "")
    request))


(defn execute
  "Performs an HttpRequest. Returns an InputStream on 200 response, otherwise throws."
  [^HttpRequest request]
  (let [client (DefaultHttpClient.)
        response ^HttpResponse (.execute client request)
        status (.getStatusLine response)]
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


(defn -main [& args]
  (println "Hello World!"))
