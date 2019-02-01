(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]
            [laniu.db :refer :all]))


(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306
             :engine        "InnoDB"
             :charset       "utf8"
             :use-ssl       false}})


(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx3"
             :server-name   "localhost"
             :port-number   3306
             :engine        "InnoDB"
             :charset       "utf8"
             :use-ssl       false}})


(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {:db_table "ceshi_reporter"})

(create-table reporter :debug? true :only-sql? true)

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(create-table Publisher2 :only-sql? true)


(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(select Publisher)



(meta @*current-pooled-dbs)


(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(create-table Publisher :only-sql? true)

(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age  {:type :int-field}}
          :meta {:db_table "ceshi_author"})

(create-table Author :only-sql? true)

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "unrate"] [0 "0 star"] [1 "1 star"] [2 "2 star"] [3 "3 star"] [4 "4 star"] [5 "5 star"]]}
                   :authors   {:type :many-to-many-field :model Author}
                   :publisher {:type :foreignkey :model Publisher :related-name :book}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})

(create-table Book :only-sql? true)


(deftest defmodel-test-1
  (testing "model定义测试"
    (let [m {:name {:db_column "name", :type :char-field, :max-length 60},
             :id   {:type :auto-field, :primary-key? true, :db_column "id"}}]
      (is (= Publisher m)))))


(deftest simple-select-test
  (testing "简单的select测试"
    (let [sql (select Publisher :only-sql? true)
          right_sql ["select ceshi_publisher.name, ceshi_publisher.id from ceshi_publisher"]]
      (is (= sql right_sql)))))


(deftest simple-update!-test
  (testing "简单的update测试"
    (let [sql (update! Book :values {:name "aaa"} :where [:id 1] :only-sql? true)
          right_sql ["update ceshi_book set ceshi_book.name=? where ceshi_book.id= ?" "aaa" 1]]
      (is (= sql right_sql)))))


(deftest simple-delete!-test
  (testing "简单的delete!测试"
    (let [sql (delete! Book :where [:id 1] :only-sql? true)
          right_sql ["DELETE ceshi_book FROM ceshi_book WHERE ceshi_book.id= ?" 1]]
      (is (= sql right_sql)))))



(deftest simple-insert!-test
  (testing "insert!测试"
    (let [sql (insert! Publisher :values {:name "hello"} :only-sql? true)
          right_sql {"ceshi_publisher.name" "hello"}]
      (is (= sql right_sql)))))


(defmulti Hello :type)

(defmethod Hello :int-field [data] (println "hello")
  )

(defmethod Hello :default [d]
  (println "cool")
  (println d)
  )

(defmulti area :shape)
(defmethod area :circle [data]
  (println "area 1")
  (println data)
  )

(area {:shape :circle :r 10})


(Hello {:type :int-field :data "aaa"})

(defmulti my-hello :type)

(defmethod my-hello :int-field [data]
  (println "jjj")
  (println data)
  )

(defmethod my-hello :char-field [data]
  (println "char-field")
  (println data)
  )

(my-hello {:type :char-field :max-length 10})

(my-hello {:type :int-field})


(defn beget [this proto]
  (assoc this ::prototype proto))

(beget {:id 10} {:name "edison"})



(defprotocol Foo
  "Foo doc string"
  (bar [this b] "bar doc string")
  (baz [this] [this b] "baz doc string"))


(deftype Bar [data] Foo
  (bar [this param]
    (println "111 data:" data " param:" param)
    (println data param))
  (baz [this]
    (println "222")
    (println (class this)))
  (baz [this param]
    (println "333 data:" data " param:" param)
    (println param)))

(deftype Aaa [] Foo
  (bar [this param]
    (println "999" param)
    ))

(.bar (Aaa.) "jj")

(Bar. "some data")

(let [b (Bar. "some data")]
  (.bar b "param")
  (.baz b)
  (.baz b "baz with param"))


(extend-protocol Foo String
  (bar [this param] (println this param)))

(bar "hello" "world")

(defmulti my-hello :type)

(defmethod my-hello :int-field [data]
  (println "jjj")
  (println data)
  )

(my-hello {:type :int-field :max-length 10})


(defmulti nihaoa :type)

(defmethod nihaoa :int [data & args]
  (println "111" data args)
  )

(defmethod nihaoa :char [data]
  (println "jjj")
  (println data)
  )

(nihao {:type :char :max-length 10})

(:type {:type :char :max-length 10})
(nihaoa {:type :char :max-length 10})

(nihaoa {:type :int :val "hello"})


(defprotocol NiHaoMa
  (baz [this data])
  (bac [data])
  )

(deftype MyNiHao [data] NiHaoMa
  (baz [this data]
    (println "barz" this data))
  (bac [data] (println "data:" data))
  )

(let [b (MyNiHao. "dd")]
  (.baz b "eee")
  (.bac b)
  )

(def *names (ref []))
(dosync
  (ref-set *names ["hello"])
  (alter *names #(if (not-empty %) (conj % "Jza")))
  )

@*names

(def session (atom {:user "Bob"}))
(def session (atom {}))


(defn load-content []
  (if (:user @session)
    (println "load content")
    (println "Pealse login")))

(load-content)

(defmacro defprivate [name args & body]
  `(defn ~(symbol name) ~args
     (if (:user @session)
       (do
         ~@body)
       (println "please login"))))


(macroexpand-1 '(defprivate load-news [aaa] (println "this is the news")))

(load-news "ddd")

(defprivate foo [message] (println message))

(foo "hello edison")




(for [a [1 2 3 4 5]]
  (do
    (println a)
    (println "jj"))
  )

(for [x (range 40)
      :when (= 1 (rem x 4))]
  x)

(for [x (range 40)
      :while (= 1 (rem x 4))]
  x)

(for [x (iterate #(+ 4 %) 0)
      :let [z (inc x)]
      :while (< z 40)]
  z)

(for [[x y] (partition 2 (range 20))]
  (+ x y))
