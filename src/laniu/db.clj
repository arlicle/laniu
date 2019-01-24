(ns laniu.db
  (:require [laniu.core :refer :all]
            [clojure.java.jdbc :as jdbc]))

(defn get-db-engine
  ([] (get-db-engine :write))
  ([action]
   (get-in (meta @*current-pooled-dbs) [:engine action] "InnoDB")))



(defn get-db-charset
  []
  (get (meta @*current-pooled-dbs) :charset "utf8"))


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

  (case (:type v)
    (:auto-field :int-field :foreignkey)
    "int(11)"

    :float-field
    "double"

    :tiny-int-field
    (let [max-length (get v :max-length 4)
          max-length (if (> max-length 4) 4 max-length)
          ]
      (str "tinyint(" max-length ")"))

    :char-field
    (str "varchar(" (get v :max-length) ")")

    :many-to-many-field
    nil

    :else))


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



(defn fields-to-db-info
  [model]
  (->
    (mapv
      (fn
        [item]
        (let [key* (atom []) field-type (get-field-type item)]
          (if field-type
            (reduce
              (fn [r s]
                (if s
                  (str r " " s)
                  r))
              [(field-to-column model item)
               field-type
               (null-field item)
               (auto-increment item)])))
        ) model)
    (conj (primary-key-sql model))))



(defn create-table
  "create table by model"
  [model & {:keys [debug? only-sql?]}]
  (let [model-db-name (get-model-db-name model)
        sql (str "CREATE TABLE `" model-db-name "` (\n"
                 (clojure.string/join ",\n" (filter (comp not nil?) (fields-to-db-info model)))
                 "\n) ENGINE=" (get-db-engine) " DEFAULT CHARSET=" (get-db-charset))]
    (if debug?
      (println sql))
    (if only-sql?
      sql
      (jdbc/execute! (db-connection) [sql]))))


(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(create-table Publisher :only-sql? true)

(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age  {:type :int-field}}
          :meta {:db_table "ceshi_author"})

(create-table Author :only-sql? true)

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "unrate"] [0 "0 star"] [1 "1 star"] [2 "2 star"] [3 "3 star"] [4 "4 star"] [5 "5 star"]]}
                   :authors   {:type :many-to-many-field :model Author}
                   :publisher {:type :foreignkey :model Publisher :related-name :book}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})

(create-table Book :only-sql? true)


