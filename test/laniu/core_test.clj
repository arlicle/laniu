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