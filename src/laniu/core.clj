(ns laniu.core
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            ))





(defn- auto-field-spec
  "
  An int-field that automatically increments according to available IDs.
  You usually won’t need to use this directly;
  a primary key field will automatically be added to your model if you don’t specify otherwise.

  By default, Laniu gives each model the following field:

  :id {:type :auto-field :primary_key? true}

  If you’d like to specify a custom primary key, just specify :primary_key? true on one of your fields.
  If Laniu sees you’ve explicitly set :primary_key? true, it won’t add the automatic id column.
  "
  [{field-type :type primary_key? :primary_key?}]
  [`int?])

(defn- foreignkey-spec
  "
  A many-to-one relationship.
  Requires two positional arguments: which the model is related and the on_delete option.
  To create a recursive relationship – an object that has a many-to-one relationship with itself
  – use {:type \"foreinkey\" :model \"self\" :on-delete  :CASCADE}
  "
  [{field-type :type model :model on-delete :on-delete blank? :blank?}]
  [`int?])

(defn- char-field-spec
  "
  A string field, for small- to large-sized strings.
  For large amounts of text, use text-field.
  "
  [opts]
  (let [
        max-length-spec (if-let [max-length (:max-length opts)]
                          `#(<= (count %) ~max-length))
        choices-spec (if-let [choices (:choices opts)]
                       (let [choices-map (into {} choices)]
                         `#(contains? ~choices-map %)))
        ]

    (filterv identity [`string? max-length-spec choices-spec])
    ))

(defn- text-field-spec
  "
  A string field, for small- to large-sized strings.
  For large amounts of text, use text-field.
  "
  [opts]
  (let [
        max-length-spec (if-let [max-length (:max-length opts)]
                          `#(<= (count %) ~max-length))
        choices-spec (if-let [choices (:choices opts)]
                       (let [choices-map (into {} choices)]
                         `#(contains? ~choices-map %)))
        ]

    (filterv identity [`string? max-length-spec choices-spec])
    ))


(defn- int-field-spec
  "
  An integer. Values from -2147483648 to 2147483647 are safe in all databases.
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value (if (and max-value (> max-value 2147483647)) 2147483647 max-value)
        min-value (if (and min-value (< min-value -2147483648)) -2147483648 min-value)

        max-value-spec (if max-value
                         `#(<= % ~max-value))
        min-value-spec (if min-value
                         `#(>= % ~min-value))

        choices-spec (if-let [choices (:choices opts)]
                       (let [choices-map (into {} choices)]
                         `#(contains? ~choices-map %)))
        ]

    (filterv identity [`int? max-value-spec min-value-spec choices-spec])
    ))


(defn- tiny-int-field-spec
  "
  Like an integer-field, but only allows values under a certain (database-dependent) point.
  Values from -128 to 127 are safe in all databases.
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value (if (and max-value (> max-value 127)) 127 max-value)
        min-value (if (and min-value (< min-value -128)) -128 min-value)

        max-value-spec (if max-value
                         `#(<= % ~max-value))
        min-value-spec (if min-value
                         `#(>= % ~min-value))

        choices-spec (if-let [choices (:choices opts)]
                       (let [choices-map (into {} choices)]
                         `#(contains? ~choices-map %)))
        ]


    (filterv identity [`int? max-value-spec min-value-spec choices-spec])
    ))




(defn optimi-model-fields
  "index more db field data from field type
  a primary key field will automatically be added to model if you don’t specify otherwise.
  "
  [fields]
  (let [*primary_key (atom nil)
        new-fields (reduce (fn [r [k v]]
                             (if (:primary_key? v)
                               (reset! *primary_key k))
                             (assoc r k
                                      (case (:type v)
                                        :foreignkey
                                        (merge {:db_column (str (name k) "_id") :to_field :id} v)
                                        (merge {:db_column (name k)} v)
                                        ))
                             ) {} fields)]
    (if (not @*primary_key)
      [(assoc new-fields :id {:type :auto-field :primary_key? true :db_column "id"}) :id]
      [new-fields @*primary_key])))



(defmacro defmodel
  "A model is the single, definitive source of information about your data.
  It contains the essential fields and behaviors of the data you’re storing.
  Generally, each model maps to a single database table.

  The basic:

  Each attribute of the model represents a database field.
  With all of this, Laniu gives you an automatically-generated database-access API

  "
  [model-name & {fields-configs :fields meta-configs :meta methods-config :methods :or {meta-configs {} methods-config {}}}]
  (let [
        ns-name (str (ns-name *ns*))
        default_db_name (str (clojure.string/join "_" (rest (clojure.string/split ns-name #"\."))) "_" model-name)
        meta-configs (merge {:db_table default_db_name} meta-configs)
        {req-fields :req opt-fields :opt opt-fields2 :opt2}
        (reduce (fn [r [k v]]
                  (if (or (= :auto-field (:type v)) (contains? v :default) (:blank? v))
                    (-> r
                        (update-in [:opt] conj (keyword (str ns-name "." (name model-name)) (name k)))
                        (update-in [:opt2] assoc k (:default v)))
                    (update-in r [:req] conj (keyword (str ns-name "." (name model-name)) (name k)))
                    )
                  ) {:req [] :opt [] :opt2 {}} fields-configs)

        [fields-configs pk] (optimi-model-fields fields-configs)
        models-fields (assoc fields-configs
                        :---fields (set (keys fields-configs))
                        :---default-value-fields opt-fields2
                        :---sys-meta {:name (name model-name) :ns-name ns-name :primary_key pk}
                        :---meta meta-configs)]

    `(do
       ~@(for [[k field-opts] fields-configs]
           `(s/def
              ~(keyword (str ns-name "." (name model-name)) (name k))
              (s/and
                ~@(case (:type field-opts)
                    :char-field
                    (char-field-spec field-opts)

                    :int-field
                    (int-field-spec field-opts)

                    :tiny-int-field
                    (tiny-int-field-spec field-opts)

                    :text-field
                    (text-field-spec field-opts)

                    :foreignkey
                    (foreignkey-spec field-opts)

                    :auto-field
                    (auto-field-spec field-opts)

                    (do
                      (println "(:type field-opts):" (:type field-opts))
                      ['string?])))))

       (s/def ~(keyword ns-name (name model-name))
         (s/keys :req-un ~req-fields
                 :opt-un ~opt-fields
                 ))

       (def ~(symbol model-name)
         ~models-fields
         ))))




(defn get-field-db-name
  [model k *join-table]
  (let [k_name (name k) model-db-table (get-model-db-name model)]
    (if-let [[_ foreingnkey-field-name link-table-field] (re-find #"(\w+)\.(\w+)" k_name)]
      ; 获取其它表的表明，替换第一个部分
      (let [foreignkey-field (keyword foreingnkey-field-name)
            _ (check-model-field model foreignkey-field)
            join-model-db-name (get-in model [foreignkey-field :model :---meta :db_table])]
        (if *join-table
          (swap! *join-table conj [join-model-db-name
                                   (str model-db-table "."
                                        (get-field-db-column model foreignkey-field)
                                        " = " join-model-db-name "."
                                        (get-foreignkey-to-field-db-column model foreignkey-field)
                                        )]))
        (str join-model-db-name "." link-table-field)
        )
      (do
        (check-model-field model k)
        (str model-db-table "." (get-field-db-column model k)))
      )))


(defn clean-insert-model-data
  [model data]
  (reduce (fn [r [k v]]
            (assoc r (get-field-db-name model k nil)
                     (if (fn? v) (v) v)))
          {}
          (select-keys (merge (:---default-value-fields model) data)
                       (:---fields model))))


(defn- get-model-db-name
  [model]
  (get-in model [:---meta :db_table])
  )

(defn get-field-db-column
  [model field]
  (get-in model [field :db_column])
  )



(defn get-foreignkey-to-field-db-column
  [model foreignkey-field]
  (get-in model [foreignkey-field :type])
  (let [to-field (get-in model [foreignkey-field :to_field])]
    (if (keyword? to-field)
      (get-in model [foreignkey-field :model to-field :db_column])
      to-field
      )
    )
  )



(defn get-model-name
  [model]
  (get-in model [:---sys-meta :name])
  )


(defn get-model&table-name
  [model]
  [(get-model-name model) (get-model-db-name model)]
  )


(defn insert!
  "insert the data into the database."
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)]
    (if (s/valid? (keyword (str (ns-name *ns*)) model-name) values)
      (let [new-data
            (if clean-data?
              (clean-insert-model-data model values)
              values
              )]
        (when debug?
          (prn "insert data to db " (keyword db-table-name) " : " new-data))
        (jdbc/insert! (db-connection) (keyword db-table-name) new-data))
      (s/explain-data (keyword (str (ns-name *ns*)) model-name) values)
      )))



(defn insert-multi!
  "一次插入多条数据"
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)
        model-key (keyword (str (ns-name *ns*)) model-name)
        new-items (if clean-data?
                    (map-indexed (fn [idx item]
                                   (if (s/valid? model-key item)
                                     (clean-insert-model-data model item)
                                     (throw (Exception. (str "error-data in row : " idx " , " item)))
                                     )
                                   ) values)
                    values
                    )
        ]

    (when debug?
      (prn "db:" (keyword db-table-name) " items:" new-items))

    (jdbc/insert-multi! (db-connection) (keyword db-table-name) new-items)
    )
  )



(defn check-model-field
  "
  check is field exists and is field type correct
  if is not correct , throw exception
  "
  ([model field] (check-model-field model field nil))
  ([model field field-type]
   (if (not (contains? (:---fields model) field))
     (throw (Exception. (str "field " field " is not in model " (get-model-name model)))))
   (if (and field-type (not= (get-in model [field :type]) field-type))
     (throw (Exception. (str "only :foreignkey field can related search. "
                             field " is not a foreignkey field. "
                             field "'s type is " (or (get-in model [field :type]) "nil")
                             ))))
   true))



(defn infix
  ([model form]
   (let [*vals (atom [])]
     [(infix model form *vals) @*vals]))
  ([model [op & elements] *vals]
   (let [elements
         (mapv (fn [x] (cond
                         (list? x)
                         (infix model x *vals)
                         (keyword? x)
                         (get-field-db-name model x nil)
                         (symbol? x)
                         (do
                           (swap! *vals conj x)
                           '?)
                         :else
                         x
                         )) elements)]
     (format "(%s)" (clojure.string/join "" (interpose op elements))))))



(defn clean-data
  "return the valid field data"
  [model data]
  (select-keys data (:---fields model))
  )


(defn get-update!-fields-query
  "
  make data map to sql query
  return:
   [\"ceshi_article.view_count=?,ceshi_article.headline=?\" 30 \"aaa\"]
  "
  [model data]
  (let [new-data (clean-data model data)
        [r-fields r-vals]
        (reduce (fn [r [k v]]
                  (if (list? v)
                    (let [[k2 v2] (infix model v)]
                      (-> r
                          (update-in [0] conj (str (get-field-db-name model k nil) "=" k2))
                          (update-in [1] into v2)
                          )
                      )
                    (-> r
                        (update-in [0] conj (str (get-field-db-name model k nil) "=?"))
                        (update-in [1] conj v)
                        )
                    )
                  )
                [[] []]
                new-data
                )]
    [(clojure.string/join "," r-fields) r-vals]))



(defn get-model
  [model-symbol]
  (var-get (resolve model-symbol))
  )


(defn check-where-func
  [op]
  (if (not (contains? #{'or 'and} op))
    (throw (Exception. (str "() must first of function 'or' or 'and', " op " is not valid.")))
    )
  true)


(defn parse-sql-func
  [[func-name v]]
  (case func-name
    > [" > ?" v]
    >= [" >= ?" v]
    < [" < ?" v]
    <= [" <= ?" v]
    (startswith :startswith) [" like ?" (str v "%")]
    nil? [(if v
            " IS NULL "
            " IS NOT NULL "
            ) nil]
    (in :in) [(str " in " "(" (clojure.string/join "," (repeat (count v) "?")) ")") v]
    [" **** " " none "]
    ))




(defn where-parse
  [model where-condition]
  (let [[op where-condition] (if (list? where-condition)
                               [(first where-condition) (rest where-condition)]
                               ['and where-condition])
        _ (check-where-func op)
        *join-table (atom [])
        [fields vals]
        (if (keyword? (first where-condition))
          (reduce (fn [r [k v]]
                    (let [[s-type new-val]                  ;查询类型
                          (if (vector? v)
                            ; 如果是vector进行单独的处理
                            (parse-sql-func v)

                            ; 否则就是普通的值, 直接等于即可
                            ["= ?" v]
                            )
                          conj-func (if (vector? new-val)
                                      #(apply conj %1 %2)
                                      conj)]
                      (-> r
                          (update-in [0] conj (str (get-field-db-name model k *join-table) s-type))
                          (update-in [1] conj-func new-val))))
                  [[] []]
                  (partition 2 where-condition)
                  )
          (reduce (fn [r where-condition2]
                    (let [[fields-str2 vals2 join-table] (where-parse model where-condition2)]
                      (swap! *join-table into join-table)
                      ;(swap! *join-table update-in [0] into (get join-table 0))
                      ;(swap! *join-table update-in [1] into (get join-table 1))
                      (-> r
                          (update-in [0] conj (str "(" fields-str2 ")"))
                          (update-in [1] #(apply conj %1 %2) vals2))
                      ))
                  [[] []]
                  where-condition))]
    (if (seq fields)
      [(clojure.string/join (str " " op " ") fields) vals @*join-table]
      )))



(defmacro update!
  [model & {values :values where-condition :where debug? :debug? clean-data? :clean-data? :or {debug? false clean-data? true}}]
  (let [model (get-model model)
        [fields-str fields-values] (get-update!-fields-query model values)
        [where-query-str values where-join-table] (where-parse model where-condition)
        where-query-str (if where-query-str (str "where " where-query-str))

        where-join-query-str (clojure.string/join " " (get-join-table-query (set where-join-table) "INNER"))

        fields-str (if fields-str (str " set " fields-str))
        sql (str "update " (get-model-db-name model) " "
                 where-join-query-str " "
                 fields-str " " where-query-str)
        query-vec (-> [sql]
                      (into fields-values)
                      (into values))]
    (when debug?
      (prn query-vec))

    `(jdbc/execute! (db-connection) ~query-vec)
    ))



(defn update-or-insert!
  "to be continue"
  [model data & {where-condition :where debug? :debug? clean-data? :clean-data? :or {clean-data? true}}]

  )


(defn get-select-fields-query
  [model fields]
  (if (seq fields)
    (let [*join-table (atom [])]
      [(clojure.string/join ", "
                            (map (fn [k]
                                   (if (= (type k) clojure.lang.Keyword)
                                     ; 一种是字段直接就是关键字，表示字段名
                                     (get-field-db-name model k *join-table)
                                     ; 一种字段是有中括号，表示有别名
                                     (let [[k0 k1] k]
                                       (str (get-field-db-name model k0 *join-table) " as " (name k1))
                                       )))
                                 fields))
       @*join-table])
    ["*" nil]))



(defn get-join-table-query
  [join-table join-type]
  (map (fn [[t s]] (str join-type " JOIN " t " ON (" s ")"))
       join-table))



(defmacro select
  [model & {fields-list :fields where-condition :where debug? :debug?}]
  (let [model (get-model model)
        [where-query-str values where-join-table] (where-parse model where-condition)
        where-query-str (if where-query-str (str "where " where-query-str))
        [fields-str field-join-table] (get-select-fields-query model fields-list)

        set-where-join-table (set where-join-table)
        diff-join-table (clojure.set/difference (set field-join-table) set-where-join-table)
        fields-join-query-strs (get-join-table-query diff-join-table "LEFT")
        where-join-query-strs (get-join-table-query set-where-join-table "INNER")
        sql (str "select " fields-str " from " (get-model-db-name model) " "
                 (clojure.string/join " " fields-join-query-strs) " "
                 (clojure.string/join " " where-join-query-strs) " "
                 where-query-str)
        query-vec (into [sql] values)]
    (when debug?
      (prn query-vec))
    `(jdbc/query (db-connection) ~query-vec)))



(defmacro delete!
  [model & {where-condition :where debug? :debug?}]
  (let [model (get-model model)
        [where-query-str values where-join-table] (where-parse model where-condition)
        where-query-str (if where-query-str (str "WHERE " where-query-str))
        where-join-query-strs (get-join-table-query (set where-join-table) "INNER")
        sql (str "DELETE " (get-model-db-name model) " FROM " (get-model-db-name model) " "
                 (clojure.string/join " " where-join-query-strs) " "
                 where-query-str)
        query-vec (into [sql] values)]
    (when debug?
      (prn query-vec))
    `(jdbc/execute! (db-connection) ~query-vec)))



(defonce *current-pooled-dbs (atom nil))



(defn connection-pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))



(defn db-connection
  [& {:keys [action db]}]
  (let [pooled-db @*current-pooled-dbs]
    (if db
      @(get pooled-db db)
      (if (= action :read)
        ; 需要把读改为随机读取
        @(get pooled-db (get-in pooled-db [:___db_by_action :read 0]))
        @(get pooled-db (get-in pooled-db [:___db_by_action :write 0]))
        ))))



(defn defdb
  [db-settings]
  (let [*db-by-action (atom {:read [] :write []})]
    (reset! *current-pooled-dbs
            (reduce (fn [r [k v]]
                      (let [p (:permission v)]
                        (if (contains? #{:read :read_and_write} p)
                          (swap! *db-by-action update-in [:read] conj k))
                        (if (not= :read p)
                          (swap! *db-by-action update-in [:write] conj k)))

                      (assoc r k (delay (connection-pool v))))
                    {} db-settings))
    (swap! *current-pooled-dbs assoc :___db_by_action @*db-by-action)
    ))

