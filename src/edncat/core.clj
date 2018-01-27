(ns edncat.core
  (:require [clojure.edn :as edn]))

;;; edn
(def cats (edn/read-string (slurp "cats.edn")))
(get-in cats [1 :name]) ;;=> "クーちゃん"

(let [r (java.io.PushbackReader.
          (clojure.java.io/reader "cats.edn"))]
  (def cats' (edn/read r))
  (def others (edn/read r)))
(get-in cats' [2 :age]) ;;=> 6
(get-in others [1 2]) ;;=> 3


;;; printer
(def nine 9)
(def hello "Hello")
(def nine-str "9")
;; human friendly
(println nine)     ;;=> 出力は9
(println hello)    ;;=> 出力はHello
(println nine-str) ;;=> 出力は9
;; reader friendly
(prn nine)     ;;=> 出力は9
(prn hello)    ;;=> 出力は"Hello"
(prn nine-str) ;;=> 出力は"9"
;; print-method
(defn- print-method-to-str [d]
  (let [w (java.io.StringWriter.)]
    (print-method d w)
    (.toString w)))
(print-method-to-str nine)      ;;=> "9""
(print-method-to-str hello)     ;;=> "\"Hello\""
(print-method-to-str nine-str)  ;;=> "\"9\""


;;; reader
(defn- mk-comparator [printer]
  (fn [d]
    (= d (read-string (printer d)))))
(let [eq? (mk-comparator print-method-to-str)]
  (println (eq? nine))
  (println (eq? hello))
  (println (eq? nine-str))) ;;=> all true
(let [eq? (mk-comparator str)]
  (println (eq? nine))
  (println (eq? hello))
  (println (eq? nine-str))) ;;=> true, false, false
(let [eq? (mk-comparator pr-str)]
  (println (eq? nine))
  (println (eq? hello))
  (println (eq? nine-str))) ;;=> all true
;; eval reader
(read-string "(println \"Hello\")")     ;;=> (println "Hello")
(read-string "#=(println \"Hello\")")   ;;=> Helloが出力され、nilが返る
(comment (binding [*read-eval* false]
           (read-string "#=(println \"Hello\")")))
      ;;=> java.lang.RuntimeException:
      ;;=> EvalReader not allowed when *read-eval* is false.

;; *print-dup*
(let [print-to-str #(with-out-str (print %))
      eq? (mk-comparator print-to-str)]
  (println (eq? nine))
  (println (eq? hello))
  (println (eq? nine-str))) ;;=> true, false, false
(let [print-to-str #(with-out-str (binding [*print-dup* true] (print %)))
      eq? (mk-comparator print-to-str)]
  (println (eq? nine))
  (println (eq? hello))
  (println (eq? nine-str))) ;;=> all true
(with-out-str (pr (get cats 0))) ;;=> "{:name \"ターちゃん\", :age 9}"
(binding [*print-dup* true]
  (with-out-str (pr (get cats 0))))
  ;;=> "#=(clojure.lang.PersistentArrayMap/create {:name \"ターちゃん\", :age 9})"


;;; record
(defrecord Cat [name age])
(def ta (->Cat "ターちゃん" 9))
(def kuro (->Cat "クロコ" 6))
(def ku (map->Cat {:name "クーちゃん" :age 12}))

(with-out-str (print ta))
  ;;=> "#edncat.core.Cat{:name ターちゃん, :age 9}"
(with-out-str (pr ta))
  ;;=> "#edncat.core.Cat{:name \"ターちゃん\", :age 9}"
(binding [*print-dup* true]
  (with-out-str (print ta)))
  ;;=> "#edncat.core.Cat[\"ターちゃん\", 9]"

(let [print-to-str #(with-out-str (print %))
      eq? (mk-comparator print-to-str)]
  (println (eq? ta))) ;;=> false
(let [print-to-str #(with-out-str (pr %))
      eq? (mk-comparator print-to-str)]
  (println (eq? ta))) ;;=> true
(let [print-to-str #(with-out-str (binding [*print-dup* true] (print %)))
      eq? (mk-comparator print-to-str)]
  (println (eq? ta))) ;;=> true

;;; use Cat in edn
(defmethod print-method edncat.core.Cat [cat writer]
  (.write writer (str "#edncat.core/Cat " [(:name cat) (:age cat)])))
(defn cat-reader [v]
  (apply ->Cat v))
(with-out-str (pr ta))
  ;;=> "#edncat.core/Cat [\"ターちゃん\" 9]"
(let [cat (edn/read-string
            {:readers {'edncat.core/Cat cat-reader}}
            "#edncat.core/Cat [\"ターちゃん\" 9]")]
  (= ta cat)) ;;=> true
(let [cat (edn/read-string
            {:readers {'edncat.core.Cat map->Cat}}
            "#edncat.core.Cat{:name \"ターちゃん\", :age 9}")]
  (= ta cat)) ;;=> true

;;; print-method and print-dup
(let [w (java.io.StringWriter.)]
    (print-method cats w)
    (.toString w))
(let [w (java.io.StringWriter.)]
    (print-dup cats w)
    (.toString w))
  ;;=> どちらも"[{:name \"ターちゃん\", :age 9}
  ;;=>           {:name \"クーちゃん\", :age 12}
  ;;=>           {:name \"クロコ\", :age 6}]"

(def special-cats (with-meta cats {:type :special-cats}))
(meta special-cats) ;;=> {:type :special-cats}
(class special-cats) ;;=> clojure.lang.PersistentVector
(defmethod print-method :special-cats [cat writer]
  (.write writer "They are so cute."))
(let [w (java.io.StringWriter.)]
    (print-method special-cats w)
    (.toString w))  ;;=> "They are so cute."
(let [w (java.io.StringWriter.)]
    (print-dup special-cats w)
    (.toString w))  ;;=> "They are so cute."
