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




(defmacro defmodel
  "A model is the single, definitive source of information about your data.
  It contains the essential fields and behaviors of the data you’re storing.
  Generally, each model maps to a single database table.

  The basic:

  Each attribute of the model represents a database field.
  With all of this, Laniu gives you an automatically-generated database-access API

  "

  [model-name fields & body]
  )