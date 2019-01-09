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




;(insert-multi! Publisher :values [{:name "Yunda"}
;                                  {:name "Kungong"}
;                                  {:name "Caida"}
;                                  {:name "Renmin"}
;                                  {:name "Beijing"}
;                                  ]
;               :debug? true)






(defmodel Author
          :fields {:name {:type :char-field :max-length 30}
                   :age  {:type :tiny-int-field}}
          :meta {:db_table "ceshi_author"})


;(insert-multi! Author :values [{:name "Lushenggan" :age 23}
;                               {:name "Lixiaolong" :age 28}
;                               {:name "Xudanyan" :age 30}
;                               {:name "Luogang" :age 34}
;                               {:name "SunYanhuan" :age 34}
;                               ])


(defmodel Node
          :fields {:title     {:type :char-field :max-length 60}
                   :parent    {:type :foreignkey :model :self}
                   :nest_node {:type :foreignkey :model :self}
                   :copy_node {:type :foreignkey :model :self}
                   }
          :meta [:db_table "ceshi_node"])


(select Node :fields [:title :parent.title]
        :debug? true
        )

;(get-field-db-name Node :parent.title)


(def a '({:generated_key 10} {:generated_key 11} {:generated_key 12} {:generated_key 13}))

(map (fn [{:keys [generated_key]}] generated_key) a)
(select Node
        :fields [:title [:parent.title :parent_title]]
        :debug? true)
(meta Node)

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(defmodel Book
          :fields {:name    {:type :char-field :max-length 60}
                   :pages   {:type :int-field}
                   :price   {:type :float-field :default 0}
                   :rating  {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
                   :authors {:type :many-to-many-field :model Publisher}
                   :pubdate {:type :int-field}}
          :meta {:db_table "ceshi_book"})



(defmodel tree-data)


(create-model-db-name "model-name" "ns-name.fdfa-fdsaf")

(meta Book)
(macroexpand-1
  '(defmodel Book
            :fields {:name    {:type :char-field :max-length 60}
                     :pages   {:type :int-field}
                     :price   {:type :float-field :default 0}
                     :rating  {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
                     :authors {:type :many-to-many-field :model Publisher}
                     :pubdate {:type :int-field}}
            :meta {:db_table "ceshi_book"}))


(def a '({:generated_key 50}))

(let [[{:keys [generated_key]}] '({:generated_key 50})]
  (println "generated_key" generated_key)
  )

(defmodel Store
          :fields {:name  {:type :char-field :max-length 60}
                   :books {:type :many-to-many-field :model Book}}
          :meta {:db_table "ceshi_store"})

(insert! Book
         :values {:name "Living Clojure" :pages 250 :price 23 :rating 5 :pubdate 2005}
         :debug? true
         )
(select Book)

(select Publisher)

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(insert! Publisher :values {:name "aaa"})
(defmodel Book
          :fields {:name    {:type :char-field :max-length 60}
                   :pages   {:type :int-field}
                   :price   {:type :float-field :default 0}
                   :rating  {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
                   :authors {:type :many-to-many-field :model Publisher}
                   :publisher {:type :foreignkey :model Publisher}
                   :pubdate {:type :int-field}}
          :meta {:db_table "ceshi_book"})


(insert! Book
         :values {:name "Living Clojure" :pages 250 :price 23 :rating 5 :pubdate 2005 :publisher 1}
         :debug? true
         )