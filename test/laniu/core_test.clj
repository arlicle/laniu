(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]))


(defdb
  {:default {
             :classname   "com.mysql.jdbc.Driver"
             :subprotocol "mysql"
             :subname     "//127.0.0.1:3306/projectx2"
             :user        "root"
             :password    "123"
             :useSSL      false
             }}
  )

(defmodel Author
          :fields {:name {:type :char-field :max-length 30}
                   :age  {:type :tiny-int-field}}
          :meta {:db_table "ceshi_author"})


(insert! Author :values {:name "Chris" :age 23})

(select Author :fields [:name :age])

(select Author)

(def AuthorBook)

(def xf (comp (filter odd?) (map inc)))
(transduce xf + (range 5))

class Person(models.Model):
friends = models.ManyToManyField("self")


(where-parse Node '[:title [:parent.title :parent_title]])

(defmodel Node
          :fields {:title {:type :char-field :max-length 60}
                   :parent {:type :foreignkey :model :self}
                   :nest_node {:type :foreignkey :model :self}
                   :copy_node {:type :foreignkey :model :self}
                   }
          :meta [:db_table "ceshi_node"])

(get-field-db-name Node :parent.title (atom []))

(name :parent.title)

(get-in Node [:parent :model])

(get-field-db-name )

(get-select-fields-query Node [:title [:parent.title :parent_title]])

(select Node
        :fields [:title [:parent.title :parent_title]]
        :debug? true)
(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})


(defmodel Book
          :fields {:name    {:type :char-field :max-length 60}
                   :pages   {:type :int-field}
                   :price   {:type :float-field :default 0}
                   :rating  {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
                   :authors {:type :many-to-many-field :model Publisher}
                   :pubdate {:type :date-field}}
          :meta {:db_table "ceshi_book"})


(defmodel Store
          :fields {:name  {:type :char-field :max-length 60}
                   :books {:type :many-to-many-field :model Book}}
          :meta {:db_table "ceshi_store"})



