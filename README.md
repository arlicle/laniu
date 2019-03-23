# laniu

Laniu can help you rapid development and clean.

It’s django model for clojure.

A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.

I use this lib for my work everyday, so I'm the first user, I try my best to make it better everyday.

PS. connection pooling change to hikari-cp library now.

### Leiningen/Boot
[![Clojars Project](https://clojars.org/laniu/latest-version.svg)](https://clojars.org/laniu)

### Dependency Information

Requires Clojure 1.9 or later!

And I Just test mysql 5.7.20 now, It will support more database later.

## Usage

``` clojure
(require '[laniu.core :refer :all])
(require '[laniu.db :as laniu-db])
```

### config the database connection

``` clojure
(defdb
  {:default {:adapter            "mysql"
             :username           "root"
             :password           "123"
             :database-name      "projectx2"
             :server-name        "localhost"
             :port-number        3306
             :use-ssl            false
             }})
```

or save the config to file settings.edn, laniu will auto load the config data and connection.


### Multiple databases
This setting maps database aliases, which are a way to refer to a specific database throughout query, to a dictionary of settings for that specific connection. 
``` clojure
(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306}
   :read-db  {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx3"
             :server-name   "localhost"
             :port-number   3306
             :read-only     true}})
; the default read and write
; the default database engine is InnoDB
; the default database charset is utf8

; defdb by database engine and charset
(defdb
  {:default {:adapter       "mysql"
             :username      "root"
             :password      "123"
             :database-name "projectx2"
             :server-name   "localhost"
             :port-number   3306
             :engine        "InnoDB"
             :charset        "utf8"
             :use-ssl       false}})
```
The more detail about the database connection config is here [https://github.com/tomekw/hikari-cp](https://github.com/tomekw/hikari-cp)

### define a model
``` clojure

(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {:db_table "ceshi_reporter"})

; the database sql, the model will auto add a primary key :id
; create table

(laniu-db/create-table reporter :debug? true)
CREATE TABLE `ceshi_reporter` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `full_name` varchar(70) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

(defmodel category
          :fields {:name       {:type :char-field :max-length 30}
                   :sort_order {:type :int-field :default 0}}
          :meta {:db_table "ceshi_category"})

(laniu-db/create-table category :debug? true)
CREATE TABLE `ceshi_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `sort_order` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


(defmodel article
          :fields {:headline   {:type :char-field :max-length 200}
                   :content    {:type :text-field}
                   :view_count {:type :int-field :default 0}
                   :reporter   {:type :foreignkey :model reporter :on-delete :cascade}
                   :category   {:type :foreignkey :model category :on-delete :set-null :blank true}
                   :created    {:type :int-field :default #(quot (System/currentTimeMillis) 1000)}}
          :meta {:db_table "ceshi_article"})

CREATE TABLE `ceshi_article` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `headline` varchar(200) NOT NULL,
  `content` longtext NOT NULL,
  `reporter_id` int(11) NOT NULL,
  `view_count` int(11) NOT NULL,
  `category_id` int(11) DEFAULT NULL,
  `created` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ceshi_article_reporter_id_45762308_fk_ceshi_reporter_id` (`reporter_id`),
  KEY `ceshi_article_category_id_dc75abf4_fk_ceshi_category_id` (`category_id`),
  CONSTRAINT `ceshi_article_category_id_dc75abf4_fk_ceshi_category_id` FOREIGN KEY (`category_id`) REFERENCES `ceshi_category` (`id`),
  CONSTRAINT `ceshi_article_reporter_id_45762308_fk_ceshi_reporter_id` FOREIGN KEY (`reporter_id`) REFERENCES `ceshi_reporter` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

; foreignkey to self
(defmodel tree
          :fields {:name       {:type :char-field :max-length 30}
                   :parent     {:type :foreignkey :model :self}
                   :sort-order {:type :int-field :default 0}
                   })

CREATE TABLE `ceshi_tree` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `parent_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ceshi_tree_parent_id_c2f9831f_fk_ceshi_tree_id` (`parent_id`),
  CONSTRAINT `ceshi_tree_parent_id_c2f9831f_fk_ceshi_tree_id` FOREIGN KEY (`parent_id`) REFERENCES `ceshi_tree` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
```

when you define a model , It's automatic create the data spec.

### insert data
If the field has :default config, It will auto fill the default value to the field.

``` clojure
(insert! reporter :values {:full_name "edison"})
;=> 45

(insert! reporter :values {:full_name "chris"})
;=> 46

(insert! category :values {:name "IT" :sort_order 1})
;=> 9

(insert! category :values {:name "Movie" :sort_order 2})
;=> 10

; add :debug? true will print the sql info
(insert! category :values {:name "Fun" :sort_order 3} :debug? true)
"insert data to db " :ceshi_category " : " {"ceshi_category.name" "Fun", "ceshi_category.sort_order" 3}
;=> 11
```



### insert with default value
:created field and :view_count field will auto fill the default value

``` clojure
(insert! article
         :values {:headline "just a test"
                  :content  "hello world"
                  :reporter 45
                  :category 11})
; => 6
```

### insert wrong data with spec error
When you define a model, the defmodel will auto define a data spec, when you insert data or update data, the spec will valid the data.

``` clojure
(insert! reporter :values {:full_name2 "chris"})
;=>
{:full_name2 "chris"} - failed: (contains? % :full_name) spec: :laniu.core-test/reporter



(insert! category :values {:name "Flower" :sort_order "a"})
;=>
"a" - failed: int? in: [:sort_order] at: [:sort_order] spec: :laniu.core-test.category/sort_order

```

### field with choices valid

``` clojure
(defmodel user
          :fields {
                   :first-name {:type :char-field :verbose-name "First name" :max-length 30}
                   :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                   :gender     {:type :tiny-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "Male"], [2, "Female"]] :default 0}
                   :created    {:type :int-field :verbose-name "Created" :default #(quot (System/currentTimeMillis) 1000)}
                   })

(insert! user
         :values {:first-name "Edison"
                  :last-name  "Rao"
                  :gender     4})
;=>
4 - failed: (contains? {0 "uninput", 1 "Male", 2 "Female"} %) in: [:gender] at: [:gender] spec: :laniu.core-test.user/gender


```
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
;=> (7 8 9)

```

### update data

``` clojure
; update
(update! reporter
         :values {:full_name "Edison Rao"}
         :where [:id 45])
; => 1

; update with multi conditions
(update! reporter
         :values {:full_name "Chris Zheng"}
         :where [:id 46 :full_name "chris"]
         :debug? true)
["update ceshi_reporter set ceshi_reporter.full_name=? where ceshi_reporter.id= ? and ceshi_reporter.full_name= ?" "Chris Zheng" 46 "chris"]
; => 1

; update value , search with foreignkey model
(update! article
         :values {:reporter 1}
         :where [:category.name "IT"])
; => 1

(update! article
         :values {:category 9 :reporter 45}
         :where [:id 7])
; => 1
```


### update with function

``` clojure
(update! article
         :values {:view_count `(+ :view_count 10)})

(def a 30)
(update! article
         :values {:view_count `(* :view_count ~a)}
         :where [:id 7])

; update with more complex raq sql
(update! article 
         :values {:view_count `(rawsql "view_count+id")}
         :debug? true)
["update ceshi_article set ceshi_article.view_count=(view_count+id) "]

```

### update or insert
``` clojure
(update-or-insert! Publisher :values {:name "Yunnan"} :where [:id 1])
; {:update-count 1}
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
        :debug? true
        )
["select ceshi_category.id, ceshi_category.name as category_name from ceshi_category   where ceshi_category.name= ?" "IT"]
;=> 
({:id 9, :category_name "IT"} {:id 12, :category_name "IT"})
```


### select with or, and, not

The default is `and`
``` clojure
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where [:category.name "IT" :reporter.full_name "Edison Rao"]
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where ceshi_category.name= ? and ceshi_reporter.full_name= ?" "IT" "Edison Rao"]
=> 
({:id 7, :headline "Apple make a phone", :name "IT", :full_name "Edison Rao"})

;It's the same to 
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where (ceshi_category.name= ? and ceshi_reporter.full_name= ?)" "IT" "Edison Rao"]
=> 
({:id 7, :headline "Apple make a phone", :name "IT", :full_name "Edison Rao"})
```

with `or`

``` clojure
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or :category.name "IT" :reporter.full_name "Edison Rao")
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where (ceshi_category.name= ? or ceshi_reporter.full_name= ?)" "IT" "Edison Rao"]
=>
({:id 7, :headline "Apple make a phone", :name "IT", :full_name "Edison Rao"}
 {:id 13, :headline "just a test", :name "Fun", :full_name "Edison Rao"}
 {:id 14, :headline "Apple make a phone", :name "IT", :full_name "aaa"}
 {:id 15, :headline "A good movie recommend", :name "Movie", :full_name "Edison Rao"})
```

with `and` & `or`

``` clojure
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or [:category.name "IT" :reporter.full_name "Edison Rao"] [:category.name "Fun" :reporter.full_name "Chris Zheng"])
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where (ceshi_category.name= ? and ceshi_reporter.full_name= ?) or (ceshi_category.name= ? and ceshi_reporter.full_name= ?)" "IT" "Edison Rao" "Fun" "Chris Zheng"]
=>
({:id 7, :headline "Apple make a phone", :name "IT", :full_name "Edison Rao"}
 {:id 16, :headline "A funny joke", :name "Fun", :full_name "Chris Zheng"})

;It's the same to 
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(or (and :category.name "IT" :reporter.full_name "Edison Rao") (and :category.name "Fun" :reporter.full_name "Chris Zheng"))
        :debug? true
        )


; another and/or select
(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(and (or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng"))
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where (ceshi_category.name= ? or ceshi_reporter.full_name= ?) and (ceshi_category.name= ? or ceshi_reporter.full_name= ?)" "IT" "Edison Rao" "Fun" "Chris Zheng"]
=> ({:id 13, :headline "just a test", :name "Fun", :full_name "Edison Rao"})

;It's same to 

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `[(or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng")]
        :debug? true
        )
```

with `not`, `not` only can contains one collection. (not [:id 1 :headline "xxx"])

``` clojure
(select article :where `(not [:id 1]) :debug? true)
["select * from ceshi_article where not (ceshi_article.id= ?)" 1]

(select article
        :fields [:id :headline :category.name :reporter.full_name]
        :where `(not [(or :category.name "IT" :reporter.full_name "Edison Rao") (or :category.name "Fun" :reporter.full_name "Chris Zheng")])
        :debug? true
        )
["select ceshi_article.id, ceshi_article.headline, ceshi_category.name, ceshi_reporter.full_name from ceshi_article  INNER JOIN ceshi_reporter ON (ceshi_article.reporter_id = ceshi_reporter.id) INNER JOIN ceshi_category ON (ceshi_article.category_id = ceshi_category.id) where not ((ceshi_category.name= ? or ceshi_reporter.full_name= ?) and (ceshi_category.name= ? or ceshi_reporter.full_name= ?))" "IT" "Edison Rao" "Fun" "Chris Zheng"]
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

; You also can filter from category to article
(select category :where [:article.headline "ccc"] :debug? true)
["select * from ceshi_category INNER JOIN ceshi_article ON (ceshi_category.id = ceshi_article.category_id) where ceshi_article.headline= ?" "A funny joke"]
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

### select many-to-many-field
``` clojure
(defmodel Publisher
          :fields {:name {:type :char-field :max-length 60}}
          :meta {:db_table "ceshi_publisher"})

CREATE TABLE `ceshi_publisher` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(300) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

(defmodel Author
          :fields {:name {:type :char-field :max-length 100}
                   :age {:type :int-field}}
          :meta {:db_table "ceshi_author"})

CREATE TABLE `ceshi_author` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `age` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

(defmodel Book
          :fields {:name      {:type :char-field :max-length 60}
                   :pages     {:type :int-field}
                   :price     {:type :float-field :default 0}
                   :rating    {:type :tiny-int-field :choices [[-1 "unrate"] [0 "0 star"] [1 "1 star"] [2 "2 star"] [3 "3 star"] [4 "4 star"] [5 "5 star"]]}
                   :authors   {:type :many-to-many-field :model Author :through-db "ceshi_book_authors" :through-field-columns ["book_id" "author_id"]}
                   :publisher {:type :foreignkey :model Publisher :related-name :book}
                   :pubdate   {:type :int-field}}
          :meta {:db_table "ceshi_book"})

CREATE TABLE `ceshi_book` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(300) NOT NULL,
  `pages` int(11) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `rating` double NOT NULL,
  `pubdate` int(11) NOT NULL,
  `publisher_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ceshi_book_publisher_id_7564e663_fk_ceshi_publisher_id` (`publisher_id`),
  CONSTRAINT `ceshi_book_publisher_id_7564e663_fk_ceshi_publisher_id` FOREIGN KEY (`publisher_id`) REFERENCES `ceshi_publisher` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

CREATE TABLE `ceshi_book_authors` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `book_id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ceshi_book_authors_book_id_author_id_66920651_uniq` (`book_id`,`author_id`),
  KEY `ceshi_book_authors_author_id_86478fd9_fk_ceshi_author_id` (`author_id`),
  CONSTRAINT `ceshi_book_authors_author_id_86478fd9_fk_ceshi_author_id` FOREIGN KEY (`author_id`) REFERENCES `ceshi_author` (`id`),
  CONSTRAINT `ceshi_book_authors_book_id_8f05f3f8_fk_ceshi_book_id` FOREIGN KEY (`book_id`) REFERENCES `ceshi_book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


(select Book :where [:authors.name "Chris Zheng"] :debug? true)
["select ceshi_book.pubdate, ceshi_book.publisher_id, ceshi_book.name, ceshi_book.pages, ceshi_book.id, ceshi_book.price, ceshi_book.rating from ceshi_book INNER JOIN ceshi_book_authors ON (ceshi_book.id = ceshi_book_authors.book_id) INNER JOIN ceshi_author ON (ceshi_book_authors.author_id = ceshi_author.id) where ceshi_author.name= ?" "Chris Zheng"]

(select Author :where [:book.name "Living Clojure"] :debug? true)
["select ceshi_author.age, ceshi_author.name, ceshi_author.id from ceshi_author INNER JOIN ceshi_book_authors ON (ceshi_author.id = ceshi_book_authors.author_id) INNER JOIN ceshi_book ON (ceshi_book_authors.book_id = ceshi_book.id) where ceshi_book.name= ?" "Living Clojure"]

; many to many annotate
(select Author :annotate [`(count :book)] :debug? true)
["select ceshi_author.age, ceshi_author.name, ceshi_author.id, count(ceshi_book.id) as count__book from ceshi_author LEFT JOIN ceshi_book_authors ON (ceshi_author.id = ceshi_book_authors.author_id) LEFT JOIN ceshi_book ON (ceshi_book_authors.book_id = ceshi_book.id) group by ceshi_author.id"]

(select Book :annotate [`(count :authors)] :debug? true)
["select ceshi_book.pubdate, ceshi_book.publisher_id, ceshi_book.name, ceshi_book.pages, ceshi_book.id, ceshi_book.price, ceshi_book.rating, count(ceshi_author.id) as count__authors from ceshi_book LEFT JOIN ceshi_book_authors ON (ceshi_book.id = ceshi_book_authors.book_id) LEFT JOIN ceshi_author ON (ceshi_book_authors.author_id = ceshi_author.id) group by ceshi_book.id"]
```


### select with function

``` clojure
; select with function
(select article
        :where [:id `(> 7)])

(select article
        :where [:id `(not= 7)]
        :debug? true)
["select * from ceshi_article where ceshi_article.id <> ?" 7]

(select article
        :where [:id `(in [6 7 8])]
        :debug? true)
["select * from ceshi_article where ceshi_article.id in (?,?,?)" 6 7 8]

(select article
        :where [:headline `(startswith "a")]
        :debug? true)
["select * from ceshi_article where ceshi_article.headline like ?" "a%"]

; you can also use original sql function with rawsql
(select article
        :where [:id `(rawsql "in (6,7,8)")]
        :debug? true)
["select * from ceshi_article where ceshi_article.id in (6,7,8)"]

; or 
(select article
        :where [:id `(rawsql "in (?,?,?)" [6 7 8])]
        :debug? true)
["select * from ceshi_article where ceshi_article.id in (?,?,?)" 6 7 8]
```

### order by
(select article
        :where [:id `(> 7)]
        :order-by [:id])

["select ceshi_article.category_id, ceshi_article.view_count, ceshi_article.headline, ceshi_article.content, ceshi_article.created, ceshi_article.reporter_id, ceshi_article.id from ceshi_article where ceshi_article.id > ? order by ceshi_article.id asc" 7]


(select article
        :where [:id `(> 7)]
        :order-by [:-id]
        :debug? true)
["select ceshi_article.category_id, ceshi_article.view_count, ceshi_article.headline, ceshi_article.content, ceshi_article.created, ceshi_article.reporter_id, ceshi_article.id from ceshi_article where ceshi_article.id > ? order by ceshi_article.id desc" 7]


### limit
(select article
        :where [:id `(> 7)]
        :limit 3
        :order-by [:id])

or 

(select article
        :where [:id `(> 7)]
        :limit [3]
        :order-by [:id])

["select ceshi_article.category_id, ceshi_article.view_count, ceshi_article.headline, ceshi_article.content, ceshi_article.created, ceshi_article.reporter_id, ceshi_article.id from ceshi_article where ceshi_article.id > ? order by ceshi_article.id asc limit 3" 7]


(select article
        :where [:id `(> 7)]
        :limit [3, 7]
        :order-by [:id]
        :debug? true)

["select ceshi_article.category_id, ceshi_article.view_count, ceshi_article.headline, ceshi_article.content, ceshi_article.created, ceshi_article.reporter_id, ceshi_article.id from ceshi_article where ceshi_article.id > ? order by ceshi_article.id asc limit 3,7" 7]

### get-one
(get-one article :where [:id 7] :debug? true)
["select ceshi_article.category_id, ceshi_article.view_count, ceshi_article.headline, ceshi_article.content, ceshi_article.created, ceshi_article.reporter_id, ceshi_article.id from ceshi_article where ceshi_article.id= ? limit 1" 7]

{:category_id 9,
 :view_count 30,
 :headline "Apple make a phone",
 :content "bala babla ....",
 :created 1546750837,
 :reporter_id 45,
 :id 7}

###  delete data

``` clojure
(delete! article :where [:id 3])
```

### aggregate

Returns the aggregate values (avg, sum, count, min, max), the aggregate field will return count__id, max__view_count.

``` clojure
(select article
           :aggregate `[(count :id) (max :view_count) (min :view_count) (avg :view_count) (sum :view_count)]
           :debug? true)
["select count(ceshi_article.id) as count__id, max(ceshi_article.view_count) as max__view_count, min(ceshi_article.view_count) as min__view_count, avg(ceshi_article.view_count) as avg__view_count, sum(ceshi_article.view_count) as sum__view_count from ceshi_article  "]
=> ({:count__id 13, :max__view_count 600, :min__view_count 20, :avg__view_count 67.6923M, :sum__view_count 880M})

; with alias
(select article
        :aggregate `[[(count :id) :count_id] [(max :view_count) :max_view_count]]
        :debug? true)
["select count(ceshi_article.id) as count_id, max(ceshi_article.view_count) as max_view_count from ceshi_article"]
=> ({:count_id 13, :max_view_count 600})
```
### Example from django aggregation
``` clojure


; Total number of books with publisher=BaloneyPress
(select Book :where [:publisher.name "BaloneyPress"]
        :aggregate `[(count *)]
        :debug? true
        )
["select count(*) as count from ceshi_book INNER JOIN ceshi_publisher ON (ceshi_book.publisher_id = ceshi_publisher.id) where ceshi_publisher.name= ?" "BaloneyPress"]

; Average price across all books.

(select Book
        :aggregate `[(avg :price)]
        :debug? true
        )
["select avg(ceshi_book.price) as avg__price from ceshi_book"]
```
### Annotate
``` clojure
(select category :annotate `[[(count :article) :article_count]] :debug? true)
["select ceshi_category.name, ceshi_category.sort_order, ceshi_category.id, count(ceshi_article.id) as article_count from ceshi_category INNER JOIN ceshi_article ON (ceshi_category.id = ceshi_article.category_id) group by ceshi_category.id"]
```

### run raw sql

If you need a more complex form of sql, you can use `raw-query` and `raw-execute!`, the jdbc sql document at here [http://clojure-doc.org/.../java_jdbc/using_sql.html](http://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql.html)

`raw-query` for select

``` sql
(raw-query ["SELECT * FROM ceshi_article where id=?" 15])

(raw-query ["SELECT * FROM ceshi_reporter where id=?" 15] {:as-arrays? true})
```

`raw-execute!` for insert, update, delete ...
``` sql
(raw-execute! ["update ceshi_article set content='jjjjj' where id=?" 15])

(raw-execute! ["update ceshi_article set view_count = ( 2 * view_count ) where view_count < ?" 50])
```

## To do list
#### Document
#### Create table
#### Migration
#### Insert or update
#### Interacting with multiple databases

## License

Copyright © 2019

Distributed under the Eclipse Public License, the same as Clojure.

