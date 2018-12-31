(ns laniu.core
  (:require [clojure.spec.alpha :as s])
  )


(defn- char-field
  "
  A string field, for small- to large-sized strings.
  For large amounts of text, use text-field.
  "
  [& opts]
  (let [opts-map (apply hash-map opts)
        max-length-spec (if-let [max-length (:max-length opts-map)]
                          #(<= (count %) max-length))
        choices-spec (if-let [choices (:choices opts-map)]
                       (let [choices-map (into {} choices)]
                         #(contains? choices-map %)))
        ]

    {
     :valid-spec (filter identity [string? max-length-spec choices-spec])
     :options    opts-map
     })
  )


(defn- integer-field
  "
  An integer. Values from -2147483648 to 2147483647 are safe in all databases.
  "
  [& opts]
  (let [opts-map (apply hash-map opts)
        max-value (:max-value opts-map)
        min-value (:min-value opts-map)

        ;max-value (if (and max-value (> max-value 2147483647)) 2147483647 max-value)
        ;min-value (if (and min-value (< min-value -2147483648)) -2147483648 min-value)

        max-value-spec (if max-value
                         #(<= % max-value))
        min-value-spec (if min-value
                         #(>= % min-value))

        choices-spec (if-let [choices (:choices opts-map)]
                       (let [choices-map (into {} choices)]
                         #(contains? choices-map %)))
        validator-spec (if-let [validator (:validator opts-map)]
                         validator)
        ]

    {
     :valid-spec (filter identity [int? max-value-spec min-value-spec choices-spec validator-spec])
     :options    opts-map
     })
  )

(defn- tiny-integer-field
  "
  Like an integer-field, but only allows values under a certain (database-dependent) point.
  Values from -128 to 127 are safe in all databases.
  "
  [& opts]
  (let [opts-map (apply hash-map opts)
        max-value (:max-value opts-map)
        min-value (:min-value opts-map)

        ;max-value (if (and max-value (> max-value 127)) 127 max-value)
        ;min-value (if (and min-value (< min-value -128)) -128 min-value)

        max-value-spec (if max-value
                         #(<= % max-value))
        min-value-spec (if min-value
                         #(>= % min-value))

        choices-spec (if-let [choices (:choices opts-map)]
                       (let [choices-map (into {} choices)]
                         #(contains? choices-map %)))
        validator-spec (if-let [validator (:validator opts-map)]
                         validator)
        ]

    {
     :valid-spec (filter identity [int? max-value-spec min-value-spec choices-spec validator-spec])
     :options    opts-map
     })
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
        fields-config (eval (if (string? first-arg) (second args) first-arg))
        fields-keys (keys fields-config)
        ]
    `(let [fields-config# ~fields-config fields-keys# ~fields-keys]

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