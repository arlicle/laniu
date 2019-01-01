(ns laniu.core
  (:require [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            ))





(defn- char-field
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


(defn- int-field
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


(defn- tiny-int-field
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
        {req-fields :req opt-fields :opt opt-fields2 :opt2}
        (reduce (fn [r [k v]]
                  (if (or (= :auto-field (:type v)) (contains? v :default))
                    (-> r
                        (update-in [:opt] conj (keyword (str ns-name "." (name model-name)) (name k)))
                        (update-in [:opt2] assoc k (:default v)))
                    (update-in r [:req] conj (keyword (str ns-name "." (name model-name)) (name k)))
                    )
                  ) {:req [] :opt [] :opt2 {}} fields-configs)


        models-fields (assoc fields-configs
                        :---fields (vec (keys fields-configs))
                        :---default-value-fields opt-fields2
                        :---sys-meta {:name (name model-name) :ns-name "ns-name"}
                        :---meta meta-configs)

        ]


    `(do
       ~@(for [[k field-opts] fields-configs]
           `(s/def
              ~(keyword (str ns-name "." (name model-name)) (name k))
              (s/and
                ~@(case (:type field-opts)
                    :char-field
                    (char-field field-opts)

                    :int-field
                    (int-field field-opts)

                    :tiny-int-field
                    (tiny-int-field field-opts)

                    ['string?]
                    )
                )
              ))

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
  ([model data check&fill-default-data?]
   (let [model-name (get-in model [:---sys-meta :name])]
     (if (s/valid? (keyword (str (ns-name *ns*)) model-name) data)
       (let [new-data (clean-model-data model data)]
         ; 把数据插入数据库
         (println "insert data to db :" new-data)
         ;(jdbc/insert! db-spec (keyword model-name) data)
         )
       (s/explain-data (keyword (str (ns-name *ns*)) model-name) data)
       ))))


(defn insert-multi!
  "一次插入多条数据"
  ([model items] (insert-multi! model items true))
  ([model items check&fill-default-data?]
   (let [model-name (get-in model [:---sys-meta :name]) model-key (keyword (str (ns-name *ns*)) model-name)
         data (if check&fill-default-data?
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
     ;(jdbc/insert-multi! db-spec (keyword model-name) data)
     ))
  )





(defn insert-or-update!
  "插入或者更新, 如果有就更新，如果没有就插入"
  ()
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
(println a)
(:problems a)

(throw (Exception. (str "error-data in row : " idx " , " item)))
(throw
  (Exception.
   (ex-info (str "error-data in row : " idx " , " item) a)))







