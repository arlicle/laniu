(defproject laniu "0.1.74.8-SNAPSHOT"
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
                 [cprop "0.1.13"]
                 [org.clojure/tools.logging "0.4.1"]])