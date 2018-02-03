(ns lambdaisland.repl-tools.browse-url
  (:require [clojure.java.browse :as browse]
            [com.stuartsierra.component :as component]))

(defrecord BrowseURL [url]
  component/Lifecycle
  (start [this]
    (browse/browse-url url))
  (stop [this]))

(defn new-browse-url-component [url]
  (->BrowseURL url))
