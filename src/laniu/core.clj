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

  [model-name fields & body]
  (defonce model-name)
  )



(defmodel user
          "define a model"
          {
           :id         (auto-field :verbose-name "pk" :primary_key true)
           :first-name (char-field :verbose-name "First name" :max-length 30)
           :last-name  (char-field :verbose-name "Last name" :max-length 30)
           :gender     (small-integer-field :verbose-name "Gender" :choices '((0, "uninput"), (1, "male"), (5, "female")) :default 0)
           :remarks    (text-field "Remarks" :default "")
           :is-deleted (boolean-field :default false)
           :created    (datetime-field :auto-now-add true)
           }

          (meta {
                 :ordering [:sort-order]
                 :db_table "db_user"
                 })

          (defn get-name
            [self]
            (str (:first-name self) " " (:last-name self))
            ))


(defn- make-query-then-exec [query-fn-var body & args]
  `(let [query# (-> (~query-fn-var ~@args)
                    ~@body)]
     (exec query#)))


(defn- confirm-model-data
  "验证确认数据字段
  验证插入的每个数据字段和defmodel中是否一致
  如果字段没有，判断字段是否有默认值设置，如果有，自动填写默认值
  "
  [model data]
  (println model)
  )


(defmacro insert
  "insert data to table
  model will valid data before insert
  "
  [model data & body]
  (confirm-model-data model data))


(insert user {:first-name "Edison" :last-name "Rao" :gender 1 :parent-id 0 :sort-order 1})

(def a-user (get user (where {:id 1})))

(update user {:last-name "Arlicle"} (where {:id 1}))


;insert a data

(insert user {:first-name "Edison" :last-name "Rao" :gender 1 :parent-id 0 :sort-order 1})
;=> {:pk 1}

; get a user
(def a-user (get user (where {:id 1})))

; get field value
(:first-name a-user)

; get value by model func


; update a user
(update user {:last-name "Arlicle"} (where {:id 1}))

; select users

(select user (where {:gender 1}))

; select users with fields
(select user [:first-name :last-name :gender] (where {:gender 1}))