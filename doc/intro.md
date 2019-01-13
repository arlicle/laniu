# Introduction to laniu

ManyToManyField

(insert! Article {:name "The joy book" :authors [1 2 3]})

(select Article :fields [:name [:authors [:first_name :last_name]]])

(select Article :where [:authors.name "aaa"])



