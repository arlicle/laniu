# laniu

It’s django model for clojure

A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.

## Usage


``` clojure

;define a model

(defmodel reporter
          :fields {:full_name {:type :char-field :max-length 70}}
          :meta {
                 :db_table "ceshi_reporter"
                 }
          )

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


;insert a data

(insert! reporter {:full_name "edison"})
;=> ({:generated_key 45})
(insert! reporter {:full_name "chris"})
;=> ({:generated_key 46})

(insert! category {:name "IT" :sort_order 1})
;=> ({:generated_key 9})
(insert! category {:name "Movie" :sort_order 2})
;=> ({:generated_key 10})
(insert! category {:name "Fun" :sort_order 3})
;=> ({:generated_key 11})

(insert! article {:headline "just a test"
                  :content  "hello world"
                  :reporter 1
                  :category 3
                  }
         )
; => ({:generated_key 6})

(insert-multi! article
               [{:headline "Apple make a phone"
                 :content  "bala babla ...."
                 :reporter 2
                 :category 1}
                {:headline "A good movie recommend"
                 :content  "bala babla ...."
                 :reporter 1
                 :category 2}
                {:headline "A funny joke"
                 :content  "bala babla ...."
                 :reporter 2
                 :category 3}
                ])
;=> ({:generated_key 7} {:generated_key 8} {:generated_key 9})

; get a user
(def a-user (get user (where [:id 1])))

; get field value
(:first-name a-user)

; update a user
(update user
        :values {:last-name "Arlicle"}
        :where [:id 1])

; select users
(filter user 
        :where [:gender 1])

; select users with fields
(filter user
        :fields [:first-name :last-name :gender]
        :where [:gender 1])

;; you can alias a field using a vector of [field alias]
(filter user
        :fields [[:first-name :first] [:last-name :last] :gender]
        :where [:gender 1])

; foreinkey field
(filter user
        :fields [:first-name :last-name :group.name [:group.sort-order :sort-order] :gender]
        :where [:gender 1])

; If you dont's spec the  fields , the default it select *


; more where condition
; 中括号中的多个元素，默认带了and关系
(filter user 
        :where [:id 1 :name "hello"])
; It's equal to 
(filter user
        :where (and [:id 1 :name "hello"]))

; or condition
(filter user
        :where (or :id 1 :name "hello"))
;=> [" select * from user where id=? or name=?" 1 "hello"]

; sql with and , or
(filter user
        :where [(or :id 1 :name "hello")
                (or :id 3 :name "cool")
                ])
; ["select * from user where (id=? or name=?) and (id=? or name=?)" 1 "hello" 3 "cool"]

(filter user
        :where [:id [> 1]]
        )
;=> ["select * from user where id > ?" 1]

(filter user
        :where [:id [not= 1]]
        )
;=> ["select * from user where id != ?" 1]

; User.objects.filter(id__gte=1, created__year__gt=2015)
(filter user
        :where [:id [>= 1] :created.year [> 2015]])

; Post.objects.filter(user__username__startwith="a")
(filter post
        :where [:user.username [startwith "a"]])


; aggregates

; count

```



## License

Copyright © 2018

Distributed under the Eclipse Public License, the same as Clojure.

