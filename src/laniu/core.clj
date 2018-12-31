(ns laniu.core
  (:require [clojure.spec.alpha :as s])
  )


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

    (filterv identity [string? max-length-spec choices-spec])
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

    (filterv identity [int? max-value-spec min-value-spec choices-spec])
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


    (filterv identity [int? max-value-spec min-value-spec choices-spec])
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

  [model-name & args]

  (let [first-arg (first args)
        doc-string (if (string? first-arg) first-arg)
        fields-configs (eval (if (string? first-arg) (second args) first-arg))
        fields-keys (keys fields-configs)
        name-space "laniu."
        {req-fields :req opt-fields :opt}
        (reduce (fn [r [k v]]
                  (if (or (= :auto-field (:type v)) (:default v))
                    (update-in r [:opt] conj k)
                    (update-in r [:req] conj k)
                    )
                  ) {:req [] :opt []} fields-configs)
        ]

    `(do
       ~@(for [[k field-opts] fields-configs]
           `(s/def
              ~(keyword (str name-space (name model-name)) (name k))
              (s/and
                ~@(case (:type field-opts)
                    :char-field
                    (char-field field-opts)

                    :int-field
                    (int-field field-opts)

                    :tiny-int-field
                    (tiny-int-field field-opts)
                    )
                )
              ))

       (s/def ::user
         (s/keys :req-un ~req-fields
                 :opt-un ~opt-fields
                 ))
       )
    )
  )


(defn insert
  [model data]
  ; 验证数据
  (if (s/valid? (keyword "laniu.learn" (get-in model [:---meta :name])) data)
    (let [default-value-fields (:---default-value-fields model)

          ; 填充默认值
          new-data
          (if (seq default-value-fields)
            (reduce (fn [s k]
                      (let [v (get-in model [k :default])
                            default-val (if (fn? v) (v) v)
                            ]
                        (if (nil? (k s))
                          (assoc s k default-val)
                          s
                          ))
                      ) data default-value-fields)
            data
            )
          ]
      ; 把数据插入数据库
      (println "insert data to db :" new-data)
      )
    (s/explain-data ::user data)
    ))



; define a user spec
(s/def :laniu.user/first-name (s/and string? #(<= (count %) 30)))
(s/def :laniu.user/last-name (s/and string? #(<= (count %) 30)))
(s/def :laniu.user/gender (s/and #(contains? {1 "男" 2 "女"} %)))
(s/def :laniu.user/created (s/and int?))
(s/def :laniu.user/id (s/and int?))

(s/def ::user
  (s/keys :req-un [:laniu.user/first-name :laniu.user/last-name :laniu.user/gender]
          :opt-un [:laniu.user/id :laniu.user/created]
          ))

(def user
  {
   :id                      {:type :auto-field :primary_key true}
   :first-name              {:type :char-field :verbose-name "First name" :max-length 30}
   :last-name               {:type :char-field :verbose-name "Last name" :max-length 30}
   :created                 {:type :int-field :verbose-name "Created timestamp" :auto-now-add true :default #(quot (System/currentTimeMillis) 1000)}
   :---default-value-fields [:created]
   :---meta                 {:name "user"}
   }
  )


(insert user {:first-name "hello" :last-name "nihao" :gender 1})

(defmodel user
          "define a model"
          {
           :first-name {:type :char-field :verbose-name "First name" :max-length 30}
           :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
           :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[1 "Male"] [2 "Female"]] :default 0}
           :created    {:type :int-field :verbose-name "Created" :default #(quot (System/currentTimeMillis) 1000)}
           }
          )

(s/valid? (keyword "laniu.user" "first-name") "hello")