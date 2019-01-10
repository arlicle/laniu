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


(use 'laniu.core-test :reload)

;(insert-multi! Publisher :values [{:name "Yunda"}
;                                  {:name "Kungong"}
;                                  {:name "Caida"}
;                                  {:name "Renmin"}
;                                  {:name "Beijing"}
;                                  ]
;               :debug? true)




(defmodel tree
          :fields {:name       {:type :char-field :max-length 30}
                   :parent     {:type :foreignkey :model :self}
                   :sort-order {:type :int-field :default 0}
                   })



(defmodel Author
          :fields {:name   {:type :char-field :max-length 30}
                   :parent {:type :foreignkey :model Author}
                   :age    {:type :tiny-int-field}}
          :meta {:db_table "ceshi_author"})

Author


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


(select Node :fields [:title :parent.id]
        :where [:id 1 :parent.id 3]
        :debug? true
        )

["select ceshi_node.title, ceshi_node.parent_id from ceshi_node ceshi_node.id= ? and ceshi_node.parent_id= ?" 1 3]


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

(insert! Publisher :values {:name "ccc"} :debug? true)

(insert-multi! Publisher
               :values [
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        {:name "aaa"}
                        ]
               :debug? true
               )

(let [[a b] [1 2 3 4]]
  (println "a:" a "b:" b)
  )

(let [{a :a ee :c} {:a 3 :c 33}]
  (println a)
  )

{keyword :keyword}


(let [a (future (apply + (range 1000)))
      b (future (apply + (range 2000)))
      ]
  (+ @a @b)
  )

(def a (promise))
(future (println "Hello the value a is " @a))
(def b (promise))

(future (println "I am b:" @b))

(deliver a "哈哈哈")
(deliver b "姐姐哦解耦")
@b

(deliver b "jjj")
(deliver a "Cool")





(let [[a b c & {:keys [cc]} :as bb] [1 2 3 :cc 333]]
  (println cc "bb:" bb)
  )

(let [[a b c :as cc] [1 2]]
  (println "a:" a "b:" b "c:" c "cc:" cc)
  )
(update! Publisher :values {:name "ccccJJJ" :where [:id 44]})
(select Publisher :where [:id 44])

;(defmodel Book
;          :fields {:name    {:type :char-field :max-length 60}
;                   :pages   {:type :int-field}
;                   :price   {:type :float-field :default 0}
;                   :rating  {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
;                   :authors {:type :many-to-many-field :model Publisher}
;                   :pubdate {:type :int-field}}
;          :meta {:db_table "ceshi_book"})







(create-model-db-name "model-name" "ns-name.fdfa-fdsaf")

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




(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(insert! Publisher :values {:name "aaa"})



(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age {:type :int-field}}
          :meta {:db_table "ceshi_author"})

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
                   :authors   {:type :many-to-many-field :model Publisher}
                   :publisher {:type :foreignkey :model Publisher}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})

;Book.objects.filter(publisher__name='BaloneyPress').count()
(select Book :where [:publisher.name "BaloneyPress"]
        :aggregate [(count *)]
        :debug? true
        )

(select Book
        :aggregate [(avg :price)]
        :debug? true
        )

(insert! Book
         :values {:name "Living Clojure" :pages 250 :price 23 :rating 5 :pubdate 2005 :publisher 1}
         :debug? true
         )