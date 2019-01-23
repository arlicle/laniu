(ns laniu.db
  (:require [laniu.core :refer :all]))



(defn field-to-column
  [model [k v]]
  (str
    "`"
    (case (:type v)
      :auto-field
      (get-field-db-column model k)

      :char-field
      (get-field-db-column model k)

      ; others
      (get-field-db-column model k))
    "`"))


(defn get-field-type
  [[k v]]
  (str
    (case (:type v)
      (:auto-field :int-field :foreignkey)
      "int(11)"

      :char-field
      (str "varchar(" (get v :max-length) ")")

      :else)))


(defn null-field
  [[k v]]
  (if (not (:nil? v))
    "NOT NULL"))



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
             (field-to-column model item)
             (get-field-type item)
             (null-field item)
             (auto-increment item)]))
        ) model)
    (conj (primary-key-sql model))))



(defn create-table
  [model]
  (let [model-db-name (get-model-db-name model)]
    (str "CREATE TABLE `" model-db-name "` (\n"
         (clojure.string/join ",\n" (fields-to-db-info model))
         "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8")))

(create-table Publisher)
(create-table Category)
(create-table article)
