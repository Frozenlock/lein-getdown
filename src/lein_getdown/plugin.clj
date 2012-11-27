(ns lein-getdown.plugin
  (:require [robert.hooke :as hooke]
            [leiningen.uberjar]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:import (com.threerings.getdown.tools Digester)))


(def getdown-subdir "/getdown")

;;; Some helper functions

(defn mdir-spit
  "Try to make the directories leading to the file if they don't
  already exists." [f content]
  (clojure.java.io/make-parents f)
  (spit f content))


(defn keyword-to-java [k]
  (s/replace (name k) #"-" "_"))

(defn config-map-to-string [map]
  (s/join "\n\n"
          (for [[k val] map
                v (if (coll? val) val [val])]
            (str (keyword-to-java k) " = " (if (string? v) v (str v))))))



;;; Now the main stuff
          
(defn export-getdown-txt
  "Export the getdown txt file based on the argument provided by the
  user in the project.clj file."[project & args]
  (let [{:keys [target-path main name version]} project
        getdown-args (:getdown project)]

    (when-not (:appbase getdown-args)
            (throw
             (Exception. "Must at least provide the :appbase argument in the project's getdown config.")))
    (->> (str     ;; add any user provided Getdown configuration
          (config-map-to-string getdown-args)
          
          ;; Now build the basic config from the available project-data
          "\n\n"
          (clojure.string/join
           "\n\n"
           [(str "# The main entry point for the application\n class = "
                 (clojure.string/replace main #"-" "_"))
            
            (str "# Application jar files\ncode = " name ".jar")]))
         
         (mdir-spit (str target-path getdown-subdir "/getdown.txt")))))


(defn copy-uberjar
  "Copy the uberjar into the getdown subdir and rename it."
  [project & args]
  (let [{:keys [target-path name version]} project
        uberjar-name (str name "-" version "-standalone.jar")]
    (io/copy (io/file (str target-path "/" uberjar-name))
             (io/file (str target-path getdown-subdir "/" name ".jar")))))

(defn make-digest
  "Make the digest.txt file."
  [project & args]
  (Digester/main (into-array [(str (:target-path project) getdown-subdir)])))


(defn uberjar-hook [task & args]
  (apply task args)
  (apply export-getdown-txt args)
  (apply copy-uberjar args)
  (apply make-digest args))

(defn hooks []
  (hooke/add-hook #'leiningen.uberjar/uberjar #'uberjar-hook))