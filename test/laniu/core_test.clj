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


(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {:db_table "ceshi_reporter"})


(defmodel category
          :fields {:name       {:type :char-field :max-length 30}
                   :sort_order {:type :int-field :default 0}}
          :meta {:db_table "ceshi_category"})

(defmodel article
          :fields {:headline   {:type :char-field :max-length 200}
                   :content    {:type :text-field}
                   :view_count {:type :int-field :default 0}
                   :reporter   {:type :foreignkey :model reporter :on-delete :cascade}
                   :category   {:type :foreignkey :model category :on-delete :set-null :blank true}
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}
          :meta {:db_table "ceshi_article"})

(defmodel tree
          :fields {:name       {:type :char-field :max-length 30}
                   :parent     {:type :foreignkey :model :self}
                   :sort-order {:type :int-field :default 0}
                   })

(insert! reporter :values {:full_name "edison"})
(insert! reporter :values {:full_name "chris"})

(insert! category :values {:name "Fun" :sort_order 3} :debug? true)
(insert! article
         :values {:headline "just a test"
                  :content  "hello world"
                  :reporter 45
                  :category 11})


(insert-multi! article
               :values [{:headline "Apple make a phone"
                         :content  "bala babla ...."
                         :reporter 46
                         :category 9}
                        {:headline "A good movie recommend"
                         :content  "bala babla ...."
                         :reporter 45
                         :category 10}
                        {:headline "A funny joke"
                         :content  "bala babla ...."
                         :reporter 46
                         :category 11}
                        ])

(insert! reporter :values {:full_name2 "chris"})


(insert! category :values {:name "Flower" :sort_order "a"})


(defmodel user
          :fields {
                   :first-name {:type :char-field :verbose-name "First name" :max-length 30}
                   :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                   :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "Male"], [2, "Female"]] :default 0}
                   :created    {:type :int-field :verbose-name "Created" :default #(quot (System/currentTimeMillis) 1000)}
                   })


(create-table user :only-sql? true)
(meta @user)

@user



(defmodel tree
          :fields {:name       {:type :char-field :max-length 30}
                   :parent     {:type :foreignkey :model :self}
                   :sort-order {:type :int-field :default 0}
                   })
(create-table tree :only-sql? true)

(insert! reporter :values {:full_name "edison"})

(insert! reporter :values {:full_name "chris"})
(insert! category :values {:name "IT" :sort_order 1})

(insert! category :values {:name "Movie" :sort_order 2})


(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age {:type :int-field}}
          :meta {:db_table "ceshi_author"})

(create-table Author :only-sql? true)


(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {:db_table "ceshi_reporter"})

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "unrate"] [0 "0 star"] [1 "1 star"] [2 "2 star"] [3 "3 star"] [4 "4 star"] [5 "5 star"]]}
                   :authors   {:type :many-to-many-field :model Author :through-db "ceshi_book_authors" :through-field-columns ["book_id" "author_id"]}
                   :publisher {:type :foreignkey :model Publisher :related-name :book}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})

(meta @Book)

(meta @Author)

(create-table Book :only-sql? true)

(select Book :where [:authors.name "Chris Zheng"] :debug? true)

(select Author :where [:book.name "Living Clojure"] :debug? true)

(select article
        :where [:id `(in [6 7 8 9])]
        :debug? true)


(select article
        :where [:id `(> 7)] :debug? true)

(select article
        :where [:id 7]
        :order-by [:-id]
        :debug? true)


(select article
        :where [:id 7]
        ;:order-by [:id]
        :limit 10
        :debug? true)


(select article
        :where [:id `(> 7)]
        :order-by [:-id]
        :limit 1
        :debug? true)




(select article
        :where [:id `(> 7)]
        :limit [3, 7]
        :order-by [:id]
        :debug? true)

(first (name :-id))
(select article
        :where [:id 7]
        :order-by [:-id]
        :debug? true)


(first "-fdsaf")

(select article
        :where [:id `(not= 7)]
        :debug? true)

(select article
        :where [:id `(in [6 7 8])]
        :debug? true)

(select article
        :where [:headline `(startswith "a")]
        :debug? true)

(select article
        :where [:id `(rawsql "in (6,7,8)")]
        :debug? true)

(select article
        :where [:id `(rawsql "in (?,?,?)" [6 7 8])]
        :debug? true)

(delete! article :where [:id 3])

(select article
        :aggregate `[(count :id) (max :view_count) (min :view_count) (avg :view_count) (sum :view_count)]
        :debug? true)

(select article
        :aggregate `[[(count :id) :count_id] [(max :view_count) :max_view_count]]
        :debug? true)

(select Book :where [:publisher.name "BaloneyPress"]
        :aggregate `[(count *)]
        :debug? true
        )


(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where [:category.name "IT" :reporter.full_name "Edison Rao"]
        :debug? true
        )

(select Book
        :aggregate `[(avg :price)]
        :debug? true
        )

(select Book
        :aggregate [`(avg :price)]
        :debug? true
        )

(select category :annotate [[`(count :article) :article_count]] :debug? true)

(select category :annotate `[[(count :article) :article_count]] :debug? true)



(select Author :annotate ['(count :book)] :debug? true)
(select Author :annotate [`(count :book)] :debug? true)
(select Author :annotate `[(count :book)] :debug? true)
(select Book :annotate [`(count :authors)] :debug? true)

(select article
        :where [:id (> 7)])

(select article
        :where [:id (not= 7)]
        :debug? true)
(select article
        :where [:id `(in [6 7 8])]
        :debug? true)



(def a 30)
(update! article
         :values {:view_count [`* :view_count a]}
         :where [:id 7])


(update! article
         :values {:view_count `(rawsql "view_count+id")}
         :debug? true)

(update! reporter
         :values {:full_name "Chris Zheng"}
         :where [:id 46 :full_name "chris"]
         :debug? true)

(update! article
         :values {:reporter 1}
         :where [:category.name "IT"])

(update! article
         :values {:category 9 :reporter 45}
         :where [:id 7])

(update! article
         :values {:view_count `(+ :view_count 10)})


(update! article
         :values {:view_count ('+ :view_count 10)} :debug? true)

(update! article
         :values {:view_count ['+ :view_count 10]} :debug? true)


(def a 10)

(update! article
         :values {:view_count `(+ :view_count ~a)} :debug? true)

(select article)



(insert-multi! article
               :values [{:headline "Apple make a phone"
                         :content  "bala babla ...."
                         :reporter 46
                         :category 9}
                        {:headline "A good movie recommend"
                         :content  "bala babla ...."
                         :reporter 45
                         :category 10}
                        {:headline "A funny joke"
                         :content  "bala babla ...."
                         :reporter 46
                         :category 11}
                        ])

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(not [(or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng")])
        :debug? true
        )

(select article :where `(not [:id 1]) :debug? true)
(update! reporter
         :values {:full_name "Edison Rao"}
         :where [:id 45])


(create-table user :debug? true)

(insert! user
         :values {:first-name "Edison"
                  :last-name  "Rao"
                  :gender     1} :debug? true)

(update! reporter
         :values {:full_name "Edison Rao"}
         :where [:id 45])

(update! reporter
         :values {:full_name "Chris Zheng"}
         :where [:id 46 :full_name "chris"]
         :debug? true)

(update! article
         :values {:reporter 1}
         :where [:category.name "IT"])

(update! article
         :values {:category 9 :reporter 45}
         :where [:id 7])

(comment
  (defdb
    {:default {:adapter       "mysql"
               :username      "root"
               :password      "123"
               :database-name "projectx3"
               :server-name   "localhost"
               :port-number   3306
               :engine        "InnoDB"
               :charset       "utf8"
               :use-ssl       false}}))


(create-table reporter :only-sql? true)



(create-table reporter :debug? true :only-sql? true)



(create-table Publisher :only-sql? true)



(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})


(update-or-insert! Publisher :values {:name "Hello world"} :where [:id 300])


(select category)

(select Publisher)


(select category
        :fields [:id :name]
        :where [:name "IT"]
        )

(select category
        :fields [:id [:name :category_name]]
        :where [:name "IT"]
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where [:category.name "IT" :reporter.full_name "Edison Rao"]
        :debug? true
        )


(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )


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




(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )

(select category
        :fields [:id [:name :category_name]]
        :where [:name "IT"]
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where [:category.name "IT" :reporter.full_name "Edison Rao"]
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or [:category.name "IT" :reporter.full_name "Edison Rao"] [:category.name "Fun" :reporter.full_name "Chris Zheng"])
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or (and :category.name "IT" :reporter.full_name "Edison Rao") (and :category.name "Fun" :reporter.full_name "Chris Zheng"))
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and (or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng"))
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and (or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng"))
        :debug? true
        )

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `[(or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng")]
        :debug? true
        )

(select article :where '(not (or :id 1 :id 3)))

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(not [(or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng")])
        :debug? true
        )

(select article
        :fields [:id :headline :category.name]
        :where [:id 7])

(select article
        :fields [:id :headline :category.name [:reporter.full_name :reporter_full_name]]
        :where [:id 7])

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})




