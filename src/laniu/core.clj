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
        type-spec `#(or (int? %) (float? %))
        ]
    (filterv identity [type-spec max-value-spec min-value-spec choices-spec])
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



(defn model-name-to-key
  [model-name]
  (keyword (clojure.string/lower-case model-name)))


(defn optimi-model-fields
  "index more db field data from field type
  a primary key field will automatically be added to model if you don’t specify otherwise.
  "
  [model-name fields]
  (let [*primary_key (atom nil)
        default {}
        new-fields (reduce (fn [r [k v]]
                             (if (:primary-key? v)
                               (reset! *primary_key k))
                             (assoc r k
                                      (case (:type v)
                                        :foreignkey
                                        (merge default {:db_column (str (name k) "_id") :to_field :id :related-key (model-name-to-key model-name)} v)
                                        (merge default {:db_column (name k)} v)
                                        ))
                             ) {} fields)]
    (if (not @*primary_key)
      [(assoc new-fields :id {:type :auto-field :primary-key? true :db_column "id"}) :id]
      [new-fields @*primary_key])))



(defn create-model-db-name
  "create a default model db name"
  [model-name ns-name]
  (clojure.string/lower-case
    (clojure.string/replace
      (str (clojure.string/join "_" (let [[a & b :as c] (clojure.string/split ns-name #"\.")]
                                      (if (seq b) b [a]))) "_" model-name) #"\-" "_")))



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
        [fields-configs pk] (optimi-model-fields model-name fields-configs)
        {req-fields :req opt-fields :opt opt-fields2 :opt2 un-insert-fields :ui foreignkey-fields :fr}
        (reduce (fn [r [k v]]
                  (if (or (contains? #{:auto-field :many-to-many-field} (:type v)) (contains? v :default) (:blank? v))
                    (-> (case (:type v)
                          :many-to-many-field
                          (update-in r [:ui] conj k)
                          :foreignkey
                          (assoc-in r [:fr k] v)
                          r)
                        (update-in [:opt] conj (keyword (str ns-name "." (name model-name)) (name k)))
                        (update-in [:opt2] assoc k (:default v)))
                    (-> (if (= :foreignkey (:type v)) (assoc-in r [:fr k] v) r)
                        (update-in [:req] conj (keyword (str ns-name "." (name model-name)) (name k))))
                    )
                  ) {:req [] :opt [] :opt2 {} :ui #{} :fr {}} fields-configs)
        models-fields (with-meta fields-configs
                                 {
                                  :fields               (set (keys fields-configs))
                                  :un-insert-fields     un-insert-fields
                                  :default-value-fields opt-fields2
                                  :name                 (name model-name)
                                  :ns-name              ns-name
                                  :primary-key          pk
                                  :meta                 (merge {:db_table (create-model-db-name model-name ns-name)} meta-configs)})]
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

                    :float-field
                    (float-field-spec field-opts)

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

                    :date-field
                    (date-field-spec field-opts)

                    (do
                      (println "(:type field-opts):" (:type field-opts))
                      ['string?])))))

       ($s/def ~(keyword ns-name (name model-name))
         ($s/keys :req-un ~req-fields
                  :opt-un ~opt-fields
                  ))

       (def ~(symbol model-name)
         ~models-fields
         )
       ~@(for [[k v] foreignkey-fields]
           `(def ~(symbol (:model v))
              (vary-meta ~(symbol (:model v)) assoc-in [:one2many ~(:related-key v)]
                         {:model ~(symbol model-name)
                          :field ~k
                          }))))))



(defn get-model-primary-key
  [model]
  (:primary-key (meta model)))



(defn- get-model-db-name
  ([model1 model2]
   (if (= model1 :self)
     (get-model-db-name model2)
     (if (keyword? model1)
       (throw (Exception. (model1 " is not valid model. do you want use :self ?")))
       (get-model-db-name model1))))
  ([model]
   (get-in (meta model) [:meta :db_table])))


(declare get-foreignkey-field-db-column)
(defn get-field-db-column
  [model field]
  (if-let [c (get-in model [field :db_column])]
    c
    ; if field db_column is nil , get from one2many self.id=foreignkey-model.foreignkey-field
    (let [m-data (meta model)]
      (if-let [[f-model f-field] [(get-in m-data [:one2many field :model]) (get-in m-data [:one2many field :field])]]
        (if-let [to-field (get-in f-model [f-field :to_field])]
          (if (keyword? to-field)
            (get-foreignkey-field-db-column f-model f-field to-field)
            to-field)
          )
        ))
    ))




(defn get-model-fields
  [model]
  (:fields (meta model)))



(defn get-model-default-fields
  [model]
  (:default-value-fields (meta model)))



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
   (if (and (not (contains? (get-model-fields model) field)) (not (get-in (meta model) [:one2many field])))
     (throw (Exception. (str "field " field " is not in model " (get-model-name model)))))
   (if (and field-type (not= (get-in model [field :type]) field-type))
     (throw (Exception. (str "only :foreignkey field can related search. "
                             field " is not a foreignkey field. "
                             field "'s type is " (or (get-in model [field :type]) "nil")
                             ))))
   true))




(defn get-foreignkey-field-db-column
  "获取foreignkey字段对应的db column
  如果当前没有这个foreignkey字段，那么可能是one2many的字段，从foreignkey model中来取对应字段
  "
  [model foreignkey-field field]
  (if (= :self (get-in model [foreignkey-field :model]))
    (get-in model [field :db_column])
    (if-let [c (get-in model [foreignkey-field :model field :db_column])]
      c
      (let [m-data (meta model)]
        ; 如果没有，那么就从one2many中拿
        (if-let [f-model (get-in m-data [:one2many foreignkey-field :model])]
          (get-field-db-column f-model field))))))




(defn get-foreignkey-to-field-db-column
  "获取foreignkey对应对面的db column，
  如果当前没有这个foreignkey的字段，那么可能是one2many字段，那么就要从m-data中获取对应字段和对应字段的db column
  "
  [model foreignkey-field]
  (if-let [to-field (get-in model [foreignkey-field :to_field])]
    (if (keyword? to-field)
      (get-foreignkey-field-db-column model foreignkey-field to-field)
      to-field)
    ; from one2many
    (let [m-data (meta model)]
      (if-let [[f-model f-field] [(get-in m-data [:one2many foreignkey-field :model]) (get-in m-data [:one2many foreignkey-field :field])]]
        (get-field-db-column f-model f-field)))))



(defn get-foreignkey-table
  "获取关联表相关信息
  [from-where-or-select-or-nil join-model-db-name foreignkey-field alias is-field-allow-null?]
  "
  [model *tables from foreignkey-field join-model-db-name]

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
      [nil join-model-db-name foreignkey-field (get-in tables [:tables join-model-db-name foreignkey-field]) null?])))



(defn get-foreignkey-model-db-name
  "获取foreignkey对应的数据库
  1 如果foreignkey 到:self，那么拿自身即可
  2 正常foreignkey,直接从 foreignkey model中获取
  3 如果是被foreignkey, 那么从one2many，中获取model，
  "
  [model foreignkey-field]
  (let [f-model (get-in model [foreignkey-field :model])
        m-data (get-in (meta model) [:one2many foreignkey-field :model])]
    (cond
      ; foreignkey to self
      (= f-model :self)
      (get-model-db-name model)
      ; one2many
      (and (nil? f-model) m-data)
      (get-model-db-name m-data)
      :else
      (get-model-db-name f-model)
      )))



(defn get-field-db-name
  [model k & {:keys [*join-table *tables from]}]
  (let [k_name (name k) model-db-table (get-model-db-name model)
        [_ foreingnkey-field-name link-table-field] (re-find #"(\w+)\.(\w+)" k_name)]
    (if (and foreingnkey-field-name link-table-field *join-table *tables)
      (let [foreignkey-field (keyword foreingnkey-field-name)
            link-table-field (keyword link-table-field)
            _ (check-model-field model foreignkey-field)
            join-model-db-name (get-foreignkey-model-db-name model foreignkey-field)
            join-table (get-foreignkey-table model *tables from foreignkey-field join-model-db-name)
            ; 处理数据库别名问题
            join-model-db-name (if-let [table-alias (get join-table 3)] table-alias (get join-table 1))]
        (if (= :id link-table-field)
          (str model-db-table "." (get-field-db-column model foreignkey-field))
          (do
            (swap! *join-table conj [join-table
                                     (str model-db-table "."
                                          (get-field-db-column model foreignkey-field)
                                          " = " join-model-db-name "."
                                          (get-foreignkey-to-field-db-column model foreignkey-field)
                                          )])
            (println "link-table-field:" link-table-field)
            (str join-model-db-name "." (get-foreignkey-field-db-column model foreignkey-field link-table-field)))))
      (do
        (check-model-field model k)
        (str model-db-table "." (get-field-db-column model k))))))



(defn clean-insert-model-data
  [model data]
  (reduce (fn [r [k v]]
            (cond
              (= :many-to-many-field (get-in model [k :type]))
              (assoc-in r [1 k] (if (fn? v) (v) v))
              (and (get-in model [k :primary-key?]) (not v))
              r
              :else
              (assoc-in r [0 (get-field-db-name model k)] (if (fn? v) (v) v))))
          [{} {}]
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
  "trans (+ 1 2) to (1 + 2)
  or (* 4 (+ 3 2)) to \"(4*(3+2))\"
  "
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
  "get model by symbol, if not find, throw error."
  [model-symbol]
  (if-let [v (resolve model-symbol)]
    (var-get v)
    (throw (Exception. (str "Not find model " model-symbol)))))


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



(defn get-aggregate-alias
  "fix the count *"
  [k]
  (if (not= '* k)
    (str "__" (name k))
    ))


(defn get-aggregate-fields-query
  [model fields]
  (map (fn a-func [item & x]
         (if (string? item)
           ; support raw sql count
           item
           (let [[as-k op k] (if (vector? item)
                               ; 如果需要用到别名
                               (cons (str " as " (name (last item))) (first item))
                               (cons (str " as " (first item) (get-aggregate-alias (second item))) item)
                               )

                 k2 (cond (keyword? k)
                          (get-field-db-name model k)
                          (= '* k)
                          "*"
                          :else
                          (a-func k true))]
             (if (nil? x)
               (str op "(" k2 ")" as-k)
               (str op "(" k2 ")")))))
       fields))



(defn insert!
  "insert the data into the database."
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)]
    (if ($s/valid? (keyword (str (ns-name *ns*)) model-name) values)
      (let [[insert-data mdata]
            (if clean-data?
              (clean-insert-model-data model values)
              values
              )
            ]
        (when debug?
          (prn "insert data to db " (keyword db-table-name) " : " insert-data))
        (let [[{:keys [generated_key]}] (jdbc/insert! (db-connection) (keyword db-table-name) insert-data)]
          generated_key))
      ($s/explain-data (keyword (str (ns-name *ns*)) model-name) values))))



(defn insert-multi!
  "一次插入多条数据"
  [model & {:keys [values debug? clean-data?] :or {debug? false clean-data? true}}]
  (let [[model-name db-table-name] (get-model&table-name model)
        model-key (keyword (str (ns-name *ns*)) model-name)
        new-items (if clean-data?
                    (map-indexed (fn [idx item]
                                   (if ($s/valid? model-key item)
                                     (first (clean-insert-model-data model item))
                                     (throw (Exception. (str "error-data in row : " idx " , " item)))
                                     )
                                   ) values)
                    values)]
    (when debug?
      (prn "db:" (keyword db-table-name) " items:" new-items))
    (let [result (jdbc/insert-multi! (db-connection) (keyword db-table-name) new-items)]
      (map (fn [{:keys [generated_key]}] generated_key) result))))



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
    `(first (jdbc/execute! (db-connection) ~query-vec))))




(defn get-annotate-query
  [model fields]
  (println "get-annotage-query" fields)
  )


(defmacro select
  [model & {fields-list :fields aggregate-fields :aggregate annotate-fields :annotate where-condition :where debug? :debug?}]
  (let [model (get-model model)
        model-db-name (get-model-db-name model)
        *tables (atom {:tables {model-db-name {}} :count 1})
        [where-query-str values where-join-table] (get-where-query model where-condition *tables)
        [fields-strs field-join-table] (if aggregate-fields
                                         [(get-aggregate-fields-query model aggregate-fields)]
                                         (get-select-fields-query model fields-list *tables))
        fields-join-query-strs (get-join-table-query field-join-table)
        where-join-query-strs (get-join-table-query where-join-table)
        sql (str "select " (clojure.string/join ", " fields-strs) " from " model-db-name
                 (when (seq fields-join-query-strs)
                   (str " " (clojure.string/join " " fields-join-query-strs)))
                 (when (seq where-join-query-strs)
                   (str " " (clojure.string/join " " where-join-query-strs)))
                 (when (and where-query-str (not= "" where-query-str))
                   (str " where " where-query-str)))
        query-vec (into [sql] (filter #(not (nil? %)) values))
        ]
    ;(println (get-annotate-query model annotate-fields))
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
    `(first (jdbc/execute! (db-connection) ~query-vec))))



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


