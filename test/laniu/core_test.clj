(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]
            ))







(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})


(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age  {:type :int-field}}
          :meta {:db_table "ceshi_author"})

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "unrate"] [0 "0 star"] [1 "1 star"] [2 "2 star"] [3 "3 star"] [4 "4 star"] [5 "5 star"]]}
                   :authors   {:type :many-to-many-field :model Author :through-db "ceshi_book_authors" :through-field-columns ["book_id" "author_id"]}
                   :publisher {:type :foreignkey :model Publisher :related-name :book}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})


(select Book :where [:authors.name "Chris Zheng"] :debug? true)


(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306
             :use-ssl       false}})

(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306}
   :read-db {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx3"
             :server-name   "localhost"
             :port-number   3306
             :read-only     true}})

(meta @*current-pooled-dbs)

(def datasource-options {:adapter       "mysql"
                         :username      "root"
                         :password      "123"
                         :database-name "projectx2"
                         :server-name   "localhost"
                         :port-number   3306
                         })


(defonce datasource
         (delay (hikari-cp/make-datasource datasource-options)))

(let [conn {:datasource @datasource}]
  (let [rows (jdbc/query conn "SELECT * from ceshi_article")]
    (println rows))
  )

(jdbc/with-db-connection [conn {:datasource @datasource}]
                         (let [rows (jdbc/query conn "SELECT * from ceshi_article")]
                           (println rows)))





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

(select category :where [:article.headline "A funny joke"] :debug? true)
(select article :fields [[:category.name :category_name]] :where [:category.name "ccc"] :debug? true)