(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]
            [hikari-cp.core :as hikari-cp]
            [clojure.java.jdbc :as jdbc]))






(meta @*current-pooled-dbs)
(defdb
  {:default {:adapter            "mysql"
             :username           "root"
             :password           "123"
             :database-name      "projectx2"
             :server-name        "localhost"
             :port-number        3306
             :use-ssl false}
   :red-db {:adapter            "mysql"
            :username           "root"
            :password           "123"
            :database-name      "projectx3"
            :server-name        "localhost"
            :port-number        3306
            :use-ssl false}})


(select )

(def datasource-options {:adapter            "mysql"
                         :username           "root"
                         :password           "123"
                         :database-name      "projectx2"
                         :server-name        "localhost"
                         :port-number        3306
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