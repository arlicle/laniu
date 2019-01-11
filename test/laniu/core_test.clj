(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]
            [hikari-cp.core :refer :all]
            [clojure.java.jdbc :as jdbc]))


(defdb
  {:default {
             :classname   "com.mysql.jdbc.Driver"
             :subprotocol "mysql"
             :subname     "//127.0.0.1:3306/projectx2"
             :user        "root"
             :password    "123"
             :useSSL      false
             }}
  )





