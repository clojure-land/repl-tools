(ns lambdaisland.repl-tools.ring-history
  (:require [com.stuartsierra.component :as component])
  (:import [clojure.lang PersistentQueue]))

(defn queue-handler [queue handler size]
  (fn [req]
    (let [res (handler req)]
      (swap! queue conj [req res])
      (when (> (count @queue) size)
        (swap! queue pop))
      res)))

(defrecord RingHistory [next-handler queue handler size]
  component/Lifecycle
  (start [this]
    (if queue
      this
      (let [queue (atom PersistentQueue/EMPTY)]
        (assoc this
               :queue queue
               :handler (queue-handler queue (:handler next-handler) size)))))
  (stop [this]
    (dissoc this
            :queue
            :handler)))


(defn new-ring-history
  "Create a new RingHistory component.

  Will keep track of the `size` last request and response objects. To do this,
  set is as the web handler, pointing it at the :next-handler to forward
  requests to.

  This component follows the convention from danielsz/system of exposing a
  `:handler` key which contains the actual ring handler function, so the
  `:next-handler` component must expose such a `:handler` key.

      (component/system-map
        :jetty (-> (new-jetty :port 1234)
                  (component/using {:handler :ring-history}))
        :ring-history (-> (new-ring-history 10)
                         (component/using {:next-handler :handler}))
        :handler (new-handler))

  See also `inject-ring-history` for a shortcut for adding this to your system
  map.
  "
  [size]
  (map->RingHistory {:size size}))


(defn inject-ring-history
  "Inject the ring-history component into a system map.

  Optionally takes the key that the ring adaptor component is associated with.

      (-> (component/system-map
            :some-handler-component ;; e.g. system.component.handler
            :jetty (-> (new-jetty)
                       (component/using {:handler :some-handler-component})))
          (inject-ring-history {:adaptor :jetty}))
  "
  [system-map & [{:keys [adaptor] :or {adaptor :http}}]]
  (-> system-map
      (assoc :ring-history (-> (new-ring-history 10)
                               (component/using {:next-handler (-> system-map
                                                                   (get adaptor)
                                                                   meta
                                                                   (get-in [::component/dependencies :handler])) })))
      (update adaptor component/using {:handler :ring-history})))

(defn last-request
  ([per-req-com]
   (last-request per-req-com 0))
  ([{queue :queue} n]
   (first (nth (reverse @queue) n))))

(defn last-response
  ([per-req-com]
   (last-response per-req-com 0))
  ([{queue :queue} n]
   (second (nth (reverse @queue) n))))
