(defproject laniu "0.1.6.7-SNAPSHOT"
  :description "Laniu can help you rapid development and clean.

  It’s django model for clojure. \n\n

  A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.
  "
  :url "https://github.com/arlicle/laniu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [mysql/mysql-connector-java "5.1.47"]
                 [hikari-cp "2.6.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 ])




(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306
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
           :category   {:type :foreignkey :model category :on-delete :set-null :blank true :related-key :article}
           :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}
  :meta {:db_table "ceshi_article"})






(select category :annotate [[(count :article) :article_count]] :debug? true)




(select Publisher :annotate [[(count :my-book) :book_count]] :debug? true)

(meta Publisher)
(defmodel Publisher
  :fields {:name {:type :char-field :max-length 60}}
  :meta {:db_table "ceshi_publisher"})

(meta Publisher)
(meta Author)

(defmodel Author
  :fields {:name {:type :char-field :max-length 100}
           :age  {:type :int-field}}
  :meta {:db_table "ceshi_author"})
(meta Book)
(defmodel Book
  :fields {:name      {:type :char-field :max-length 60}
           :pages     {:type :int-field}
           :price     {:type :float-field :default 0}
           :rating    {:type :tiny-int-field :choices [[-1 "未评分"] [0 "0分"] [1 "1分"] [2 "2分"] [3 "3分"] [4 "4分"] [5 "5分"]]}
           :authors   {:type :many-to-many-field :model Author}
           :publisher {:type :foreignkey :model Publisher :related-key :my-book}
           :pubdate   {:type :int-field}}
  :meta {:db_table "ceshi_book"})

(defmodel Store
  :fields {:name             {:type :char-field :max-length 30}
           :books            {:type :many-to-many-field :model Book}
           :registered_users {:type :int-field}}
  :meta {:db_table "ceshi_store"})

; Book.objects.all().aggregate(Avg('price'))
(select Book :aggregate [(avg :price)])
(select Book :aggregate [(max :price)])
