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



(defn create-many-to-many-table
  "根据model和字段创建many-to-many的table"
  [model field]
  (let [table-name (get-in model [field :through-db])
        [field1 field2] (get-in model [field :through-field-columns])
        sql (str "CREATE TABLE `" table-name "` (\n"
                 "`id` int(11) NOT NULL AUTO_INCREMENT,\n"
                 "`" field1 "` int(11) NOT NULL,\n"
                 "`" field2 "` int(11) NOT NULL,\n"
                 "PRIMARY KEY (`id`)\n"
                 ") ENGINE=" (get-db-engine) " DEFAULT CHARSET=" (get-db-charset)
                 )]
    sql))



(defn get-field-type
  [model [k v] *many-to-many-tablle]

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
    (do
      (swap! *many-to-many-tablle conj (create-many-to-many-table model k))
      nil)

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
  [model *many-to-many-tablle]
  (->
    (mapv
      (fn
        [item]
        (let [field-type (get-field-type model item *many-to-many-tablle)]
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
  (let [*many-to-many-tablle (atom [])
        model-db-name (get-model-db-name model)
        sql (str "CREATE TABLE `" model-db-name "` (\n"
                 (clojure.string/join ",\n" (filter (comp not nil?) (fields-to-db-info model *many-to-many-tablle)))
                 "\n) ENGINE=" (get-db-engine) " DEFAULT CHARSET=" (get-db-charset))]
    (when debug?
      (println sql)
      (doseq [s @*many-to-many-tablle]
        (println s)))
    (if only-sql?
      (do
        (cons sql @*many-to-many-tablle))
      (let [connection (db-connection)]
        (jdbc/with-db-transaction
          [connection connection {:isolation :serializable}]
          (jdbc/execute! connection sql)
          (doseq [s @*many-to-many-tablle]
            (jdbc/execute! connection s)))
        ))))


