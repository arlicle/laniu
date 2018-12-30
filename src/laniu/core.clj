(ns laniu.core)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))



(defmacro defdb
  "Define a database specification. The last evaluated defdb will be used by
  default for all queries where no database is specified by the entity."
  [db-name spec]
  `(let [spec# ~spec]
     (defonce ~db-name (create-db spec#))
     (default-connection ~db-name)))


(defmacro defmodel
  "A model is the single, definitive source of information about your data.
  It contains the essential fields and behaviors of the data you’re storing.
  Generally, each model maps to a single database table.

  The basic:

  Each attribute of the model represents a database field.
  With all of this, Laniu gives you an automatically-generated database-access API

  "

  [model-name fields & specs]

  )