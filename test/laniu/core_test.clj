(ns laniu.core-test
  (:require [clojure.test :refer :all]
            [laniu.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))


(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {
                 :db_table "ceshi_reporter"})

(defmodel category
          :fields {:name       {:type :char-field :max-length 30}
                   :sort_order {:type :int-field :default 0}
                   }
          :meta {
                 :db_table "ceshi_category"
                 }
          )

(defmodel article
          :fields {:headline   {:type :char-field :max-length 200}
                   :content    {:type :text-field}
                   :view_count {:type :int-field :default 0}
                   :reporter   {:type :foreignkey :model reporter :on-delete :cascade}
                   :category   {:type :foreignkey :model category :on-delete :set-null :blank true}
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}
                   }
          :meta {
                 :db_table "ceshi_article"
                 }
          )




(prn (get-aggregate-fields-query article '[(count :id) (sum :view_count) (avg :id) (ceiling (avg :created))]))



(aggregate article
           :fields [(count :id) (max :view_count) (min :view_count) (avg :view_count) (sum :view_count)]
           :debug? true
           )