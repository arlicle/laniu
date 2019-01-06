# laniu

It’s django model for clojure

A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.

## Usage

### define a model
``` clojure

(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {
                 :db_table "ceshi_reporter"})

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
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}
                   }
          :meta {
                 :db_table "ceshi_article"
                 }
          )

```

when you define a model , It's automatic create the data spec.

### insert data

``` clojure
(insert! reporter :values {:full_name "edison"})
;=> ({:generated_key 45})

(insert! reporter :values {:full_name "chris"})
;=> ({:generated_key 46})

(insert! category :values {:name "IT" :sort_order 1})
;=> ({:generated_key 9})

(insert! category :values {:name "Movie" :sort_order 2})
;=> ({:generated_key 10})

(insert! category :values {:name "Fun" :sort_order 3})
;=> ({:generated_key 11})

(insert! article
         :values {:headline "just a test"
                  :content  "hello world"
                  :reporter 45
                  :category 11})
; => ({:generated_key 6})
```

### insert wrong data with spec error


### insert multi rows

``` clojure
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
;=> ({:generated_key 7} {:generated_key 8} {:generated_key 9})

```

### update data

``` clojure
; update
(update! reporter
         :values {:full_name "Edison Rao"}
         :where [:id 45])
; => (1)

; update with multi conditions
(update! reporter
         :values {:full_name "Chris Zheng"}
         :where [:id 46 :full_name "chris"])
; => (1)

; update value , search with foreignkey model
(update! article
         :values {:reporter 1}
         :where [:category.name "IT"])
; => (1)

(update! article
         :values {:category 9 :reporter 45}
         :where [:id 7])
; => (1)
```

### select data
``` clojure

; select
(select category)
; =>
({:id 1, :name "aaa", :sort_order 0}
 {:id 2, :name "bbb", :sort_order 0}
 {:id 3, :name "ccc", :sort_order 0}
 {:id 4, :name "ccc", :sort_order 0}
 {:id 5, :name "aaa", :sort_order 0}
 {:id 6, :name "bbb", :sort_order 0}
 {:id 7, :name "ccc", :sort_order 0}
 {:id 8, :name "IT news", :sort_order 1}
 {:id 9, :name "IT", :sort_order 1}
 {:id 10, :name "Movie", :sort_order 2}
 {:id 11, :name "Fun", :sort_order 3}
 {:id 12, :name "IT", :sort_order 1}
 {:id 13, :name "Fun", :sort_order 3})

;select with condition
(select category 
        :fields [:id :name]
        :where [:name "IT"]
        )
;=> 
({:id 9, :name "IT"} {:id 12, :name "IT"})

```

### select with field alias

``` clojure
(select category
        :fields [:id [:name :category_name]]
        :where [:name "IT"]
        )
;=> 
({:id 9, :category_name "IT"} {:id 12, :category_name "IT"})
```

### select foreignkey field

``` clojure
(select article
        :fields [:id :headline :category.name]
        :where [:id 7])
; => 
({:id 7, :headline "Apple make a phone", :name "IT"})

; select with multi foeinkey field
(select article
        :fields [:id :headline :category.name [:reporter.full_name :reporter_full_name]]
        :where [:id 7])
;=> 
({:id 7, :headline "Apple make a phone", :name "IT", :reporter_full_name "Edison Rao"})
```

### select foreignkey condition

``` clojure
; select with foreignkey condition
(select article
        :fields [:id :headline :content :category.name [:reporter.full_name :reporter_full_name]]
        :where [:category.name "IT"])
;=>
({:id 7, :headline "Apple make a phone", :content "bala babla ....", :name "IT", :reporter_full_name "Edison Rao"}
 {:id 14, :headline "Apple make a phone", :content "bala babla ....", :name "IT", :reporter_full_name "aaa"})


(select article
        :fields [:id :headline :content :category.name :reporter.full_name]
        :where [:category.name "IT" :reporter.full_name "Edison Rao"])
; => 
({:id 7, :headline "Apple make a phone", :content "bala babla ....", :name "IT", :full_name "Edison Rao"})

```

### select with function

``` clojure
; select with function
(select article
        :where [:id [> 7]])

(select article
        :where [:headline [startswith "a"]])
```



### update with function

``` clojure
(update! article
         :values {:view_count (+ :view_count 10)})

(def a 30)
(update! article
         :values {:view_count (* :view_count a)}
         :where [:id 7])
```


###  delete data

``` clojure
(delete! article :where [:id 3])


; aggregates

; count

```

## To do list
#### Document
#### Create table
#### Migration
#### Insert or update
#### Connection Pooling

## License

Copyright © 2018

Distributed under the Eclipse Public License, the same as Clojure.

