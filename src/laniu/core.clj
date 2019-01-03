(ns laniu.core
  (:require [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            ))




(defn- foreignkey-spec
  "
  A many-to-one relationship.
  Requires two positional arguments: which the model is related and the on_delete option.
  To create a recursive relationship – an object that has a many-to-one relationship with itself
  – use {:type \"foreinkey\" :model \"self\" :on-delete  :CASCADE}
  "
  [{field-type :type model :model on-delete :on-delete blank? :blank}]
  [`int?]
  )

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
    )
  )

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
    )
  )


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
    )
  )


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
    )
  )




(defn optimi-model-fields
  [fields]
  (reduce (fn [r [k v]]
            (assoc r k
              (case (:type v)
                :foreignkey
                (merge {:db_column (str (name k) "_id") :to_field "id"} v)
                v
                ))
            ) {} fields))

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
                  (if (or (= :auto-field (:type v)) (contains? v :default))
                    (-> r
                        (update-in [:opt] conj (keyword (str ns-name "." (name model-name)) (name k)))
                        (update-in [:opt2] assoc k (:default v)))
                    (update-in r [:req] conj (keyword (str ns-name "." (name model-name)) (name k)))
                    )
                  ) {:req [] :opt [] :opt2 {}} fields-configs)


        models-fields (assoc (optimi-model-fields fields-configs)
                        :---fields (vec (keys fields-configs))
                        :---default-value-fields opt-fields2
                        :---sys-meta {:name (name model-name) :ns-name ns-name}
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

                    (do
                      (println "(:type field-opts):" (:type field-opts))
                      ['string?])))))

       (s/def ~(keyword ns-name (name model-name))
         (s/keys :req-un ~req-fields
                 :opt-un ~opt-fields
                 ))

       (def ~(symbol model-name)
         ~models-fields
         )
       )
    )
  )


(defn clean-model-data
  [model data]
  (select-keys (merge (:---default-value-fields model) data)
               (:---fields model)
               )
  )


(declare db-spec)

(defn insert

  ([model data] (insert model data true))
  ; 验证数据
  ([model data clean-data?]
   (let [model-name (get-in model [:---sys-meta :name])]
     (if (s/valid? (keyword (str (ns-name *ns*)) model-name) data)
       (let [new-data
             (if clean-data?
               (clean-model-data model data)
               data
               )
             ]
         ; 把数据插入数据库
         (println "insert data to db :" new-data)
         ;(jdbc/insert! db-spec (keyword model-name) data)
         )
       (s/explain-data (keyword (str (ns-name *ns*)) model-name) data)
       ))))


(defn insert-multi!
  "一次插入多条数据"
  ([model items] (insert-multi! model items true))
  ([model items clean-data?]
   (let [model-name (get-in model [:---sys-meta :name]) model-key (keyword (str (ns-name *ns*)) model-name)
         data (if clean-data?
                (map-indexed (fn [idx item]
                               (if (s/valid? model-key item)
                                 (clean-model-data model item)
                                 (throw (Exception. (str "error-data in row : " idx " , " item)))
                                 )
                               ) items)
                items
                )
         ]

     (println "insert multi:")
     (println data)
     (comment
       (jdbc/insert-multi! db-spec (keyword model-name) data))
     ))
  )


(update! user {:last-name "Arlicle"} {:where {:id 1}})

(defn update!
  "更新数据
  (update! user {:last-name \"Arlicle\"} {:where {:id 1}})
  "
  ([model data where-condition] (update! model data where-condition true))
  ([model data where-condition clean-data?]
   (let [[new-data new-where-condition]
         (if clean-data?
           [(select-keys data (:---fields model))
            (select-keys {:where where-condition} (:---fields model))
            ]
           [data {:where where-condition}]
           )
         ]
     ; 更新数据
     (println new-data)
     (println where-condition)
     (comment
       (jdbc/update! db-spec (keyword model-name)
                     new-data
                     new-where-condition))
     )))


(defn insert-or-update!
  "插入或者更新, 如果有就更新，如果没有就插入"
  ()
  )



(defn get-field-name-fields
  "根据keyword获取字段名称
  如果存在连表查询，需要把对应表的table名进行进行替换
  "
  [model k *join-table]
  (let [k_name (name k) table-name (get-in model [:---meta :db_table])]
    (if-let [[_ foreingnkey-field-name link-table-field] (re-find #"(\w+)\.(\w+)" k_name)]
      ; 获取其它表的表明，替换第一个部分
      (let [foreignkey-field (keyword foreingnkey-field-name)
            _ (if (not= (get-in model [foreignkey-field :type]) :foreignkey)
                (throw (Exception. (str "only :foreignkey field can related search. "
                                        foreingnkey-field-name " is not a foreignkey field. "
                                        foreingnkey-field-name "'s type is " (get-in model [foreignkey-field :type])
                                        )))
                )
            join-table-name (get-in model [foreignkey-field :model :---meta :db_table])]
        (swap! *join-table conj (str "INNER JOIN `" join-table-name "` ON (" table-name "." (get-in model [foreignkey-field :db_column])
                                     " = " join-table-name "." (get-in model [foreignkey-field :to_field])
                                     ")"
                                     ))
        (str join-table-name "." link-table-field)
        )
      (str table-name "." k_name)
      )))


(defn get-field-name-where
  "根据keyword获取字段名称
  如果存在连表查询，需要把对应表的table名进行进行替换
  "
  [model k]
  (let [k_name (name k) table-name (get-in model [:---meta :db_table])]
    (if-let [[_ foreingnkey-field-name link-table-field] (re-find #"(\w+)\.(\w+)" k_name)]
      ; 获取其它表的表明，替换第一个部分
      (let [foreignkey-field (keyword foreingnkey-field-name)
            _ (if (not= (get-in model [foreignkey-field :type]) :foreignkey)
                (throw (Exception. (str "only :foreignkey field can related search. "
                                        foreingnkey-field-name " is not a foreignkey field. "
                                        foreingnkey-field-name "'s type is " (get-in model [foreignkey-field :type])
                                        )))
                )
            join-table-name (get-in model [foreignkey-field :model :---meta :db_table])]
        (str join-table-name "." link-table-field)
        )
      (str table-name "." k_name)
      )))

INNER JOIN '' `` `ceshi_reporter` ON (`ceshi_article`.`reporter_id` = `ceshi_reporter`.`id`)

(defn fields-parse
  [model fields]
  (if (seq fields)
    (let [*join-table (atom [])]
      [
       (clojure.string/join ", "
                            (map (fn [k]
                                   (if (= (type k) clojure.lang.Keyword)
                                     ; 一种是字段直接就是关键字，表示字段名
                                     (get-field-name-fields model k *join-table)
                                     ; 一种字段是有中括号，表示有别名
                                     (let [[k0 k1] k]
                                       (str (get-field-name-fields model k0 *join-table) " as " (name k1))
                                       )))
                                 fields))
       (clojure.string/join " " @*join-table)
       ]
      )
    ["*" nil]
    ))

(fields-parse article '[:id :headline :content [:reporter.full_name :full_name]])


article


(defn parse-sql-func
  [[func-name v]]
  (case func-name
    > ["> ?" v]
    >= [">= ?" v]
    < ["< ?" v]
    <= ["<= ?" v]
    (startswith :startswith) ["like ?" (str v "%")]
    nil? [(if v
            " IS NULL "
            " IS NOT NULL "
            ) nil]
    (in :in) [(str " in " "(" (clojure.string/join "," (repeat (count v) "?")) ")") v]
    [" **** " " none "]
    ))




(defn and-parse
  [model and-condition]
  (let [c (count and-condition)]
    (let [[fields vals] (reduce (fn [r [k v]]
                                  (let [[s-type new-val]    ;查询类型
                                        (if (= (type v) clojure.lang.PersistentVector)
                                          ; 如果是vector进行单独的处理
                                          (parse-sql-func v)

                                          ; 否则就是普通的值, 直接等于即可
                                          ["= ?" v]
                                          )
                                        conj-func (if (= (type new-val) clojure.lang.PersistentVector)
                                                    #(apply conj %1 %2)
                                                    conj
                                                    )
                                        ]
                                    (-> r
                                        (update-in, [0] conj (str (get-field-name-where model k) s-type))
                                        (update-in, [1] conj-func new-val))))
                                [[] []]
                                (partition 2 and-condition)
                                )]
      [(str "where " (clojure.string/join " and " fields)) vals]
      )
    )
  )




(defn where-parse
  [model where-condition]
  (let [[r-func where-condition] (if (= (type where-condition) clojure.lang.PersistentList)
                                   [(first where-condition) (rest where-condition)]
                                   ['and where-condition])
        _
        (if (not (contains? #{'or 'and} r-func))
          (throw (Exception. (str "() must first of 'or' or 'and', " r-func " is not valid.")))
          )
        first-type (type (first where-condition))
        [fields vals]
        (if (= first-type clojure.lang.Keyword)
          (reduce (fn [r [k v]]
                    (let [[s-type new-val]                  ;查询类型
                          (if (= (type v) clojure.lang.PersistentVector)
                            ; 如果是vector进行单独的处理
                            (parse-sql-func v)

                            ; 否则就是普通的值, 直接等于即可
                            ["= ?" v]
                            )
                          conj-func (if (= (type new-val) clojure.lang.PersistentVector)
                                      #(apply conj %1 %2)
                                      conj
                                      )
                          ]
                      (-> r
                          (update-in, [0] conj (str (get-field-name-where model k) s-type))
                          (update-in, [1] conj-func new-val))))
                  [[] []]
                  (partition 2 where-condition)
                  )
          (reduce (fn [r where-condition2]
                    (let [[fields-str2 vals2] (where-parse article where-condition2)]
                      (-> r
                          (update-in, [0] conj (str "(" fields-str2 ")"))
                          (update-in, [1] #(apply conj %1 %2) vals2))
                      ))

                  [[] []]
                  where-condition)
          )

        r-func-str (str " " r-func " ")
        ]
    (if (seq fields)
      [(clojure.string/join r-func-str fields) vals]
      ))
  )




(defmacro select
  [model & {fields-list :fields where-condition :where debug :debug}]
  (let [model (var-get (resolve model))
        [where-str values] (where-parse model where-condition)
        where-str (if where-str (str "where " where-str) "")
        [fields-str join-str] (fields-parse model fields-list)
        sql (str "select " fields-str " from " (get-in model [:---meta :db_table]) " " join-str " " where-str)
        query-vec (into [sql] values)
        ]
    (println "join-str:" join-str)
    (println "fields-str:" fields-str)
    (when debug
      (prn query-vec)
      )
    `'~(jdbc/query db-spec query-vec)
    ))













(select article
        :fields [:id :headline :content [:reporter.full_name :full_name]]
        :where [:id [in [1 2 3]]]
        :debug true
        )

(def db-spec
  {
   :classname   "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname     "//127.0.0.1:3306/projectx2"
   :user        "root"
   :password    "123"
   :useSSL      false}
  )

(println ["select ceshi_article.id, ceshi_article.headline, ceshi_article.content from ceshi_article where ceshi_article.id in (?,?,?)" 1 2 3])
(jdbc/query db-spec ["select ceshi_article.id, ceshi_article.headline, ceshi_article.content, ceshi_reporter.full_name as full_name from ceshi_article where ceshi_article.id in (?,?,?)" 1 2 3])
(jdbc/query db-spec ["select ceshi_article.id, ceshi_article.headline, ceshi_article.content from ceshi_article where ceshi_article.id in (?,?,?)" 1 2 3])

(jdbc/query db-spec ["SELECT * FROM ceshi_tablea WHERE id in (?,?,?)" 1 2 3])
(jdbc/query db-spec ["SELECT * FROM ceshi_tableb"])
(jdbc/query db-spec ["SELECT * FROM ceshi_tablea WHERE name like ?" "hello%"])



(resolve 'a)

(select article)

(var-get (find-var (symbol "boot.user/b")))


(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {
                 :db_table "ceshi_reporter"
                 }
          )


(defmodel article
          :fields {:headline {:type :char-field :max-length 200}
                   :content  {:type :text-field}
                   :reporter {:type :foreignkey :model reporter :on-delete :CASCADE
                              ;:to_field :pid
                              }
                   }
          :meta {
                 :db_table "ceshi_article"
                 }
          )





(select article
        :fields [:id :headline :content [:reporter.full_name :full_name]]
        :where [
                [:id ['in [1 2 3]]
                 :headline [:startswith "aaa"]
                 :reporter.full_name "edison"
                 ]
                ('or
                  [:id 1 :headline "ddd"]
                  [:id 2 :headline "jjj"]
                  )
                ]
        )

(or
  [:id 1 :headline "ddd"]
  [:id 2 :headline "jjj"]
  )

;(throw (Exception. "my exception message"))

; define a user spec
;(s/def :laniu.user/first-name (s/and string? #(<= (count %) 30)))
;(s/def :laniu.user/last-name (s/and string? #(<= (count %) 30)))
;(s/def :laniu.user/gender (s/and #(contains? {1 "男" 2 "女"} %)))
;(s/def :laniu.user/created (s/and int?))
;(s/def :laniu.user/id (s/and int?))
;
;(s/def ::user
;  (s/keys :req-un [:laniu.user/first-name :laniu.user/last-name :laniu.user/gender]
;          :opt-un [:laniu.user/id :laniu.user/created]
;          ))
;
;(def user
;  {
;   :id                      {:type :auto-field :primary_key true}
;   :first-name              {:type :char-field :verbose-name "First name" :max-length 30}
;   :last-name               {:type :char-field :verbose-name "Last name" :max-length 30}
;   :created                 {:type :int-field :verbose-name "Created timestamp" :auto-now-add true :default #(quot (System/currentTimeMillis) 1000)}
;   :---default-value-fields [:created]
;   :---meta                 {:name "user"}
;   }
;  )



(defmodel user
          ;model user document
          :fields {:id         {:type :auto-field :verbose-name "pk" :primary_key true}
                   :first-name {:type :char-field :verbose-name "First name" :max-length 30}
                   :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                   :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "male"], [5, "female"]] :default 0}
                   :remarks    {:type :text-field :default ""}
                   :is-deleted {:type :boolean-field :default false}
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}

          :meta {:ordering [:sort-order]
                 :db_table "db_user"}
          :method {}
          )


(defmodel user
          ;model user document
          :fields {:id         {:type :auto-field :verbose-name "pk" :primary_key true}
                   :first-name {:type :foreinkey :model group}
                   :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                   :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "male"], [5, "female"]] :default 0}
                   :remarks    {:type :text-field :default ""}
                   :is-deleted {:type :boolean-field :default false}
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}

          :meta {:ordering [:sort-order]
                 :db_table "db_user"}
          :method {}
          )
(macroexpand-1
  '(defmodel user
             ;model user document
             :fields {:id         {:type :auto-field :verbose-name "pk" :primary_key true}
                      :first-name {:type :char-field :verbose-name "First name" :max-length 30}
                      :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                      :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "male"], [5, "female"]] :default 0}
                      :remarks    {:type :text-field :default ""}
                      :is-deleted {:type :boolean-field :default false}
                      :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}

             :meta {:ordering [:sort-order]
                    :db_table "db_user"}
             :method {}
             )
  )




(insert user {:first-name "hello" :last-name "nihao" :gender 2})


(insert-multi! user [{:first-name "hello" :last-name "nihao" :gender 1}
                     {:first-name "hello" :last-name "nihao" :gender 1}
                     {:first-name "hello" :last-name "nihao" :gender 1}])





(s/valid? ::user {:first-name "hahah" :last-name "lulu" :gender 3})
(s/valid? ::user {:first-name "" :last-name ""})
(def a (s/explain-data ::user {:first-name "" :last-name "" :gender 2}))


reporter


(or
  [:id 1 :headline "ddd"]
  [:id 2 :headline "jjj"]
  )

(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}})

(defmodel article
          :fields {:headline {:type :char-field :max-length 200}
                   :content  {:type :text-field}
                   :reporter {:type :foreignkey :model reporter :on-delete :CASCADE}
                   }
          )

(s/explain-data ::article {:headline "aa" :content "aa" :reporter 1})

(macroexpand-1
  '(defmodel article
             :fields {:headline {:type :char-field :max-length 200}
                      :content  {:type :text-field}
                      :reporter {:type :foreignkey :model reporter :on-delete :CASCADE}
                      }
             ))
article

