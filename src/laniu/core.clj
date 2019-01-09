(ns laniu.core
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:import java.util.Date)
  (:require [clojure.spec.alpha :as $s]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            ))


(defonce *current-pooled-dbs (atom nil))

(defn db-connection
  [& {:keys [operation db]}]
  (let [pooled-db @*current-pooled-dbs]
    (if (empty? pooled-db)
      (throw (Exception. "Error: No database connection."))
      (if db
        @(get pooled-db db)
        (if (= operation :read)
          ; 需要把读改为随机读取
          @(get pooled-db (get-in (meta pooled-db) [:db_for_operation :read 0]))
          @(get pooled-db (get-in (meta pooled-db) [:db_for_operation :write 0]))
          )))))


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



(defn defdb
  [db-settings]
  (let [*db-by-action (atom {:read [] :write []})]
    (reset! *current-pooled-dbs
            (with-meta
              (reduce (fn [r [k v]]
                        (let [p (:operation v)]
                          (if (or (nil? p) (contains? #{:read :read_and_write} p))
                            (swap! *db-by-action update-in [:read] conj k))
                          (if (not= :read p)
                            (swap! *db-by-action update-in [:write] conj k)))

                        (assoc r k (delay (connection-pool v))))
                      {} db-settings)
              {:db_for_operation @*db-by-action}))
    (if (empty? (:read @*db-by-action))
      (log/warn "Warning: No read database config."))
    (if (empty? (:write @*db-by-action))
      (log/warn "Warning: No write database config."))))



(defn- auto-field-spec
  "
  An int-field that automatically increments according to available IDs.
  You usually won’t need to use this directly;
  a primary key field will automatically be added to your model if you don’t specify otherwise.

  By default, Laniu gives each model the following field:

  :id {:type :auto-field ::primary-key? true}

  If you’d like to specify a custom primary key, just specify :primary-key? true on one of your fields.
  If Laniu sees you’ve explicitly set :primary-key? true, it won’t add the automatic id column.
  "
  [{field-type :type primary-key? :primary-key?}]
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


(defn- many-to-many-field-spec
  "
  A many-to-one relationship.
  Requires two positional arguments: which the model is related and the on_delete option.
  To create a recursive relationship – an object that has a many-to-one relationship with itself
  – use {:type \"foreinkey\" :model \"self\" :on-delete  :CASCADE}
  "
  [{field-type :type model :model on-delete :on-delete blank? :blank?}]
  [])



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



(defn- big-int-field-spec
  "
  An integer. Values from -9223372036854775808 to 9223372036854775807 are safe in all databases.
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value (if (and max-value (> max-value 9223372036854775807)) 2147483647 max-value)
        min-value (if (and min-value (< min-value -9223372036854775808)) -9223372036854775808 min-value)

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


(defn- small-int-field-spec
  "
  Like an integer-field, but only allows values under a certain (database-dependent) point.
  Values from  -32768 to 32767 are safe in all databases.
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value (if (and max-value (> max-value 32767)) 32767 max-value)
        min-value (if (and min-value (< min-value -32768)) -32768 min-value)

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



(defn- pos-tiny-int-field-spec
  "
  Like an integer-field, but only allows values under a certain (database-dependent) point.
  Values from 0 to 255 are safe in all databases.
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value (if (and max-value (> max-value 255)) 255 max-value)
        min-value (if (and min-value (< min-value 0)) 0 min-value)

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



(defn- float-field-spec
  "
  A floating-point number
  "
  [opts]
  (let [
        max-value (:max-value opts)
        min-value (:min-value opts)

        max-value-spec (if max-value
                         `#(<= % ~max-value))
        min-value-spec (if min-value
                         `#(>= % ~min-value))

        choices-spec (if-let [choices (:choices opts)]
                       (let [choices-map (into {} choices)]
                         `#(contains? ~choices-map %)))
        ]


    (filterv identity [`float? max-value-spec min-value-spec choices-spec])
    ))



(defn- boolean-field-spec
  "
   A true/false field.
  "
  [{field-type :type model :model blank? :blank?}]
  [`boolean?])



(defn- date-field-spec
  "
   A date, represented in clojure by a java.util.Date instance.
  "
  [{field-type :type model :model blank? :blank?}]
  (let [date-spec `#(instance? Date %)]
    [date-spec]))



(defn optimi-model-fields
  "index more db field data from field type
  a primary key field will automatically be added to model if you don’t specify otherwise.
  "
  [fields]
  (let [*primary_key (atom nil)
        default {}
        new-fields (reduce (fn [r [k v]]
                             (if (:primary-key? v)
                               (reset! *primary_key k))
                             (assoc r k
                                      (case (:type v)
                                        :foreignkey
                                        (merge default {:db_column (str (name k) "_id") :to_field :id} v)
                                        (merge default {:db_column (name k)} v)
                                        ))
                             ) {} fields)]
    (if (not @*primary_key)
      [(assoc new-fields :id {:type :auto-field :primary-key? true :db_column "id"}) :id]
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
        [fields-configs pk] (optimi-model-fields fields-configs)
        {req-fields :req opt-fields :opt opt-fields2 :opt2}
        (reduce (fn [r [k v]]
                  (if (or (= :auto-field (:type v)) (contains? v :default) (:blank? v))
                    (-> r
                        (update-in [:opt] conj (keyword (str ns-name "." (name model-name)) (name k)))
                        (update-in [:opt2] assoc k (:default v)))
                    (update-in r [:req] conj (keyword (str ns-name "." (name model-name)) (name k)))
                    )
                  ) {:req [] :opt [] :opt2 {}} fields-configs)


        models-fields (with-meta fields-configs
                                 {
                                  :fields               (set (keys fields-configs))
                                  :default-value-fields opt-fields2
                                  :name                 (name model-name)
                                  :ns-name              ns-name
                                  :primary-key          pk
                                  :meta                 (merge {:db_table default_db_name} meta-configs)})]

    `(do
       ~@(for [[k field-opts] fields-configs]
           `($s/def
              ~(keyword (str ns-name "." (name model-name)) (name k))
              ($s/and
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

                    (:many-to-many-field :m2m-field)
                    (many-to-many-field-spec field-opts)

                    :auto-field
                    (auto-field-spec field-opts)

                    (do
                      (println "(:type field-opts):" (:type field-opts))
                      ['string?])))))

       ($s/def ~(keyword ns-name (name model-name))
         ($s/keys :req-un ~req-fields
                  :opt-un ~opt-fields
                  ))

       (def ~(symbol model-name)
         ~models-fields
         ))))



(defn- get-model-db-name
  ([model1 model2]
   (if (= model1 :self)
     (get-model-db-name model2)
     (if (keyword? model1)
       (throw (Exception. (model1 " is not valid model. do you want use :self ?")))
       (get-model-db-name model1))))
  ([model]
   (get-in (meta model) [:meta :db_table])))



(defn get-field-db-column
  [model field]
  (get-in model [field :db_column]))


(defn get-model-fields
  [model]
  (:fields (meta model)))


(defn get-model-default-fields
  [model]
  (:default-value-fields (meta model)))



(defn get-model-primary-key
  [model]
  (:primary-key (meta model)))



(defn get-model-name
  [model]
  (:name (meta model)))


(defn get-model&table-name
  [model]
  [(get-model-name model) (get-model-db-name model)])



(defn check-model-field
  "
  check is field exists and is field type correct
  if is not correct , throw exception
  "
  ([model field] (check-model-field model field nil))
  ([model field field-type]
   (if (not (contains? (get-model-fields model) field))
     (throw (Exception. (str "field " field " is not in model " (get-model-name model)))))
   (if (and field-type (not= (get-in model [field :type]) field-type))
     (throw (Exception. (str "only :foreignkey field can related search. "
                             field " is not a foreignkey field. "
                             field "'s type is " (or (get-in model [field :type]) "nil")
                             ))))
   true))



(defn get-foreignkey-to-field-db-column
  [model foreignkey-field]
  (let [to-field (get-in model [foreignkey-field :to_field])]
    (if (keyword? to-field)
      (if (= :self (get-in model [foreignkey-field :model]))
        (get-in model [to-field :db_column])
        (get-in model [foreignkey-field :model to-field :db_column])
        )
      to-field)))



(defn get-foreignkey-field-db-column
  [model foreignkey-field field]
  (get-in model [foreignkey-field :model field :db_column]))



(defn get-foreignkey-table
  [model *tables from foreignkey-field join-model-db-name]
  (if *tables
    (let [tables @*tables a (get-in tables [:tables join-model-db-name]) c (inc (:count tables)) null? (get-in model [foreignkey-field :null?])]
      (cond
        (nil? a)
        (do
          (swap! *tables update-in [:tables join-model-db-name] assoc foreignkey-field nil)
          (swap! *tables assoc :count c)
          [from join-model-db-name foreignkey-field nil null?])
        (not (contains? a foreignkey-field))
        (do
          (swap! *tables update-in [:tables join-model-db-name] assoc foreignkey-field (str "T" c))
          (swap! *tables assoc :count c)
          [from join-model-db-name foreignkey-field (str "T" c) null?])
        :else
        [nil join-model-db-name foreignkey-field (get-in tables [:tables join-model-db-name foreignkey-field]) null?]))))


(defn get-field-db-name
  [model k & {:keys [*join-table *tables from]}]
  (let [k_name (name k) model-db-table (get-model-db-name model)]
    (if-let [[_ foreingnkey-field-name link-table-field] (re-find #"(\w+)\.(\w+)" k_name)]
      (let [foreignkey-field (keyword foreingnkey-field-name)
            _ (check-model-field model foreignkey-field)
            join-model-db-name (get-model-db-name (get-in model [foreignkey-field :model]) model)
            join-table (get-foreignkey-table model *tables from foreignkey-field join-model-db-name)

            ; 处理数据库别名问题
            join-model-db-name (if-let [table-alias (get join-table 3)] table-alias (get join-table 1))]
        (if *join-table
          (swap! *join-table conj [join-table
                                   (str model-db-table "."
                                        (get-field-db-column model foreignkey-field)
                                        " = " join-model-db-name "."
                                        (get-foreignkey-to-field-db-column model foreignkey-field)
                                        )]))
        (str join-model-db-name "." (get-foreignkey-field-db-column model foreignkey-field (keyword link-table-field))))
      (do
        (check-model-field model k)
        (str model-db-table "." (get-field-db-column model k))))))



(defn clean-insert-model-data
  [model data]
  (reduce (fn [r [k v]]
            (assoc r (get-field-db-name model k)
                     (if (fn? v) (v) v)))
          {}
          (select-keys (merge (get-model-default-fields model) data)
                       (get-model-fields model))))



(defn get-join-table-query
  [join-table]
  (reduce (fn [r [[from table _ alias null?] s]]
            (if from
              (let [join-type (case from :fields (if null? "LEFT" "INNER") :where "INNER")
                    table (if alias (str table " " alias) table)]
                (conj r (str join-type " JOIN " table " ON (" s ")")))
              r
              )
            )
          []
          join-table))



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
                         (get-field-db-name model x)
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
  (select-keys data (get-model-fields model)))



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
                          (update-in [0] conj (str (get-field-db-name model k) "=" k2))
                          (update-in [1] into v2)
                          )
                      )
                    (-> r
                        (update-in [0] conj (str (get-field-db-name model k) "=?"))
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
  (var-get (resolve model-symbol)))



(defn check-where-func
  [op]
  (if (not (contains? #{'or 'and 'not} op))
    (throw (Exception. (str "() must first of function 'or'/'and'/'not', " op " is not valid.")))
    )
  true)



(defn parse-sql-func
  [[func-name v args]]
  (case func-name
    > [" > ?" v]
    >= [" >= ?" v]
    < [" < ?" v]
    <= [" <= ?" v]
    not= [" <> ?" v]
    rawsql [(str " " v) args]
    startswith [" like ?" (str v "%")]
    nil? [(if v
            " IS NULL "
            " IS NOT NULL "
            ) nil]
    (in :in) [(str " in " "(" (clojure.string/join "," (repeat (count v) "?")) ")") v]
    [" **** " " none "]))



(defn get-where-query
  [model where-condition *tables]
  (let [[op where-condition] (if (list? where-condition)
                               [(first where-condition) (rest where-condition)]
                               ['and where-condition])
        _ (check-where-func op)
        *join-table (atom [])
        [fields vals]
        (if (keyword? (first where-condition))
          (reduce (fn [r [k v]]
                    (let [[s-type new-val]                  ;查询类型
                          (if (or (vector? v) (list? v))
                            ; 如果是vector或list进行单独的处理
                            (parse-sql-func v)

                            ; 否则就是普通的值, 直接等于即可
                            ["= ?" v]
                            )
                          conj-func (if (or (vector? new-val) (list? new-val))
                                      #(apply conj %1 %2)
                                      conj)]
                      (-> r
                          (update-in [0] conj (str (get-field-db-name model k :*join-table *join-table :*tables *tables :from :where) s-type
                                                   ))
                          (update-in [1] conj-func new-val))))
                  [[] []]
                  (partition 2 where-condition)
                  )
          (reduce (fn [r where-condition2]
                    (let [[fields-str2 vals2 join-table] (get-where-query model where-condition2 *tables)]
                      (swap! *join-table into join-table)
                      (-> r
                          (update-in [0] conj (str "(" fields-str2 ")"))
                          (update-in [1] #(apply conj %1 %2) vals2))
                      ))
                  [[] []]
                  where-condition))]
    (if (seq fields)
      (if (= 'not op)
        (do
          (if (> (count fields) 1)
            (throw (Exception. (str "not operation only can contains one collection. (not [:id 1 :headline \"xxx\"]) "
                                    fields " has " (count fields) " ."))))
          [(str "not (" (clojure.string/join " " fields) ")") vals @*join-table]
          )
        [(clojure.string/join (str " " op " ") fields) vals @*join-table]
        ))))



(defn get-select-fields-query
  [model fields *tables]
  (if (seq fields)
    (let [*join-table (atom [])]
      [
       (mapv (fn [k]
               (if (= (type k) clojure.lang.Keyword)
                 ; 一种是字段直接就是关键字，表示字段名
                 (get-field-db-name model k :*join-table *join-table :*tables *tables :from :fields)
                 ; 一种字段是有中括号，表示有别名
                 (let [[k0 k1] k]
                   (str (get-field-db-name model k0 :*join-table *join-table :*tables *tables :from :fields) " as " (name k1))
                   )))
             fields)
       @*join-table])
    ["*" nil]))




(defn get-aggregate-fields-query
  [model fields]
  (map (fn a-func [[op k] & x]
         (let [[k2 as-k] (if (keyword? k)
                           [(get-field-db-name model k) (str " as " op "__" (name k))]
                           (a-func k true))]
           (if (nil? x)
             (str op "(" k2 ")" as-k)
             [(str op "(" k2 ")") as-k])))
       fields))



(defn insert!
  "insert the data into the database."
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)]
    (if ($s/valid? (keyword (str (ns-name *ns*)) model-name) values)
      (let [new-data
            (if clean-data?
              (clean-insert-model-data model values)
              values
              )]
        (when debug?
          (prn "insert data to db " (keyword db-table-name) " : " new-data))
        (jdbc/insert! (db-connection) (keyword db-table-name) new-data))
      ($s/explain-data (keyword (str (ns-name *ns*)) model-name) values))))



(defn insert-multi!
  "一次插入多条数据"
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)
        model-key (keyword (str (ns-name *ns*)) model-name)
        new-items (if clean-data?
                    (map-indexed (fn [idx item]
                                   (if ($s/valid? model-key item)
                                     (clean-insert-model-data model item)
                                     (throw (Exception. (str "error-data in row : " idx " , " item)))
                                     )
                                   ) values)
                    values)]
    (when debug?
      (prn "db:" (keyword db-table-name) " items:" new-items))
    (jdbc/insert-multi! (db-connection) (keyword db-table-name) new-items)))



(defmacro update!
  [model & {values :values where-condition :where debug? :debug? clean-data? :clean-data? :or {debug? false clean-data? true}}]
  (let [model (get-model model)
        model-db-name (get-model-db-name model)
        *tables (atom {:tables {model-db-name {}} :count 1})
        [fields-str fields-values] (get-update!-fields-query model values)
        [where-query-str values where-join-table] (get-where-query model where-condition *tables)
        where-query-str (if where-query-str (str "where " where-query-str))

        where-join-query-str (clojure.string/join " " (get-join-table-query where-join-table))

        fields-str (if fields-str (str " set " fields-str))
        sql (str "update " model-db-name " "
                 where-join-query-str " "
                 fields-str " " where-query-str)
        query-vec (-> [sql]
                      (into fields-values)
                      (into (filter #(not (nil? %)) values)))]
    (when debug?
      (prn query-vec))

    `(jdbc/execute! (db-connection) ~query-vec)))



(comment
  (defmacro update-or-insert!
    "to be continue"
    [model & {values :values where-condition :where debug? :debug? clean-data? :clean-data? :or {debug? false clean-data? true}}]
    (let [t-con (db-connection)
          pk-key (get-model-primary-key model)
          pk (pk-key values)
          where-condition (if (empty? where-condition)
                            (if (not pk)
                              (throw (Exception. "values must have pk or where condition can't empty"))
                              [pk-key pk])
                            where-condition)]

      (jdbc/with-db-transaction [t-con db-spec]
                                (let [result (update! model :values values :where where-condition :debug? debug)]
                                  (if (zero? (first result))
                                    (insert! model :values values :debug? debug)
                                    result))))))



(defmacro select
  [model & {fields-list :fields where-condition :where debug? :debug?}]
  (let [model (get-model model)
        model-db-name (get-model-db-name model)
        *tables (atom {:tables {model-db-name {}} :count 1})
        [where-query-str values where-join-table] (get-where-query model where-condition *tables)
        [fields-str field-join-table] (get-select-fields-query model fields-list *tables)
        fields-join-query-strs (get-join-table-query field-join-table)
        where-join-query-strs (get-join-table-query where-join-table)
        sql (str "select " (clojure.string/join ", " fields-str) " from " model-db-name
                 (when (seq fields-join-query-strs)
                   (str " " (clojure.string/join " " fields-join-query-strs)))
                 (when (seq where-join-query-strs)
                   (str " " (clojure.string/join " " where-join-query-strs)))
                 (when (and where-query-str (not= "" where-query-str))
                   (str " " where-query-str)))
        query-vec (into [sql] (filter #(not (nil? %)) values))
        ]
    (when debug?
      (prn query-vec))
    `(jdbc/query (db-connection) ~query-vec)))



(defmacro delete!
  [model & {where-condition :where debug? :debug?}]
  (let [model (get-model model)
        model-db-name (get-model-db-name model)
        *tables (atom {:tables {model-db-name {}} :count 1})
        [where-query-str values where-join-table] (get-where-query model where-condition *tables)
        where-query-str (if where-query-str (str "WHERE " where-query-str))
        where-join-query-strs (get-join-table-query where-join-table)
        sql (str "DELETE " model-db-name " FROM " (get-model-db-name model) " "
                 (clojure.string/join " " where-join-query-strs) " "
                 where-query-str)
        query-vec (into [sql] values)]
    (when debug?
      (prn query-vec))
    `(jdbc/execute! (db-connection) ~query-vec)))



(defmacro aggregate
  "Returns the aggregate values (averages, sums, count etc.) "
  [model & {fields-list :fields where-condition :where debug? :debug?}]
  (let [model (get-model model)
        model-db-name (get-model-db-name model)
        *tables (atom {:tables {model-db-name {}} :count 1})
        fields-query-strs (get-aggregate-fields-query model fields-list)
        [where-query-str values where-join-table] (get-where-query model where-condition *tables)
        where-query-str (if where-query-str (str "where " where-query-str))
        where-join-query-strs (get-join-table-query where-join-table)
        sql (str "select " (clojure.string/join ", " fields-query-strs) " from " model-db-name " "
                 (clojure.string/join " " where-join-query-strs) " "
                 where-query-str)
        query-vec (into [sql] values)]
    (when debug?
      (prn query-vec))
    `(jdbc/query (db-connection) ~query-vec)
    ))



(defn raw-query
  "raw sql query for select"
  [& args]
  (apply jdbc/query (db-connection) args)
  )

(defn raw-execute!
  "raw sql for insert, update, delete ..."
  [& args]
  (apply jdbc/execute! (db-connection) args)
  )


