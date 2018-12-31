# laniu

It’s django model for clojure

A Clojure library designed to normal human that don't like SQL, well, if you don't like SQL , that part is up to you.

## Usage


``` clojure

;define a model

(defmodel user
          "model user document"
          {:id         {:type :auto-field :verbose-name "pk" :primary_key true}
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

(insert user {:first-name "Edison" :last-name "Rao" :gender 1 :parent-id 0 :sort-order 1})
;=> {:pk 1}

; get a user
(def a-user (get user (where {:id 1})))

; get field value
(:first-name a-user)

; update a user
(update user {:last-name "Arlicle"} (where {:id 1}))

; select users
(select user (where {:gender 1}))

; select users with fields
(select user [:first-name :last-name :gender] (where {:gender 1}))
```



## License

Copyright © 2018

Distributed under the Eclipse Public License, the same as Clojure.

