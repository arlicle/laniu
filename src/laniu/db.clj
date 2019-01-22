(ns laniu.db
  (:require [laniu.core :refer :all]))


(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})


(defn field-to-column
  [[k v]]
  (str
    "`"
    (case (:type v)
      :auto-field
      (get-field-db-column Publisher k)

      :char-field
      (get-field-db-column Publisher k)

      :else
      )
    "`"
    ))


(defn get-field-type
  [[k v]]
  (str
    (case (:type v)
      :auto-field
      "int(11)"

      :char-field
      (str "varchar(" (get v :max-length) ")")

      :else
      )))


(defn null-field
  [[k v]]
  (if (not (:nil? v))
    "NOT NULL"
    ))



(defn auto-increment
  [[k v]]
  (if (= (:type v) :auto-field)
    "AUTO_INCREMENT"))



(defn primary-key
  [[k v]]
  (if (= (:primary-key? v) :auto-field)
    "AUTO_INCREMENT"))


(defn primary-key-sql
  [model]
  (str "PRIMARY KEY (`" (get-field-db-column model (get-model-primary-key model)) "`)"))

(primary-key-sql Publisher)

(defn fields-to-db-info
  [model]
  (->
    (mapv
      (fn
        [item]
        (let [key* (atom [])]
          (reduce
            (fn [r s]
              (if s
                (str r " " s)
                r))
            [
             (field-to-column item)
             (get-field-type item)
             (null-field item)
             (auto-increment item)]))
        ) model)
    (conj (primary-key-sql model))))

(partial add-primary-key model)

(prn (fields-to-db-info Publisher))

(defn create-table
  [model]
  (let [model-db-name (get-model-db-name model)]
    (str "CREATE TABLE `ceshi_reporter` (\n"
         (clojure.string/join ",\n" (fields-to-db-info model))
         "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8"
         )))

(create-table Publisher)
