# laniu

It’s django model for clojure

A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.

## Usage


``` clojure

;define a model

(defmodel user
          ;model user document
          :fields {:id         {:type :auto-field :verbose-name "pk" :primary_key true}
                   :first-name {:type :char-field :verbose-name "First name" :max-length 30}
                   :last-name  {:type :char-field :verbose-name "Last name" :max-length 30}
                   :gender     {:type :small-int-field :verbose-name "Gender" :choices [[0, "uninput"], [1, "male"], [5, "female"]] :default 0}
                   :remarks    {:type :text-field :default ""}
                   :is-deleted {:type :boolean-field :default false}
                   :created    {:type :datetime-field :auto-now-add true}}

          :meta {:ordering [:sort-order]
                 :db_table "db_user"}
          :method {}
          )


;insert a data
(insert user :values {:first-name "Edison" :last-name "Rao" :gender 1 :parent-id 0 :sort-order 1})
;=> {:pk 1}

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

```



## License

Copyright © 2018

Distributed under the Eclipse Public License, the same as Clojure.

