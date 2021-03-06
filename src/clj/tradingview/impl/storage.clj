(ns tradingview.impl.storage
  (:require
   [clojure.set]
   [tradingview.impl.time :refer [to-epoch-no-ms date->ui-int]]
   [clj-time.core :as t]
   [monger.collection :as mc]))

; charts_storage_url/charts_storage_api_version/charts?client=client_id&user=user_id
; status: ok or error
; data: Array of Objects
; timestamp: UNIX time when the chart was saved (example, 1449084321)
; symbol: base symbol of the chart (example, AA)
; resolution: resolution of the chart (example, D)
; id: unique integer identifier of the chart (example, 9163)
; name: chart name (example, Test)


(defn patch-one [result]
  (-> result
      (update :timestamp to-epoch-no-ms)
      (clojure.set/rename-keys {:chart_id :id})))

(defn patch [results]
  (map patch-one results))


(defn chart-list [db client_id user_id]
 (-> (mc/find-maps db "tvchart"
                   {:client_id client_id
                    :user_id user_id}
                   {:_id 0
                    :symbol 1
                    :resolution 1
                    :id 1
                    :name 1
                    :timestamp 1
                    :chart_id 1})
     (patch)))

(defn load-chart
  [db client_id user_id chart_id]
   (-> (mc/find-maps db "tvchart"
                     {:client_id client_id 
                      :user_id user_id 
                      :chart_id chart_id}
                     {:_id 0 
                      :symbol 1 
                      :resolution 1 
                      :id 1 
                      :name 1 
                      :timestamp 1 
                      :description 1
                      :listed_exchange 1
                      :exchange 1
                      :chart_id 1 
                      :charts 1
                      :legs 1
                      :layout 1
                      :is_realtime 1
                      :short_name 1
                      :publish_request_id 1
                      :symbol_type 1}
                     true)
       (patch)
       (first)))

(defn generate-id []
  (date->ui-int (t/now)))

; POST REQUEST: charts_storage_url/charts_storage_api_version/charts?client=client_id&user=user_id&chart=chart_id

(defn save-chart
  [db client-id user-id data]
  (let [chart-id (generate-id)
        query {:client_id client-id 
               :user_id user-id 
               :chart_id chart-id}
        doc (merge data query)
        doc (merge doc {:timestamp (t/now)})]
    (mc/update db "tvchart" query doc {:upsert true})
    chart-id))

(defn modify-chart
  [db client-id user-id chart-id data]
  (let [query {:client_id client-id 
               :user_id user-id 
               :chart_id chart-id}
        doc (merge data query)
        doc (merge doc {:timestamp (t/now)})]
    (mc/update db "tvchart" query doc {:upsert true})))

(defn delete-chart
  [db client-id user-id chart-id]
  (mc/remove db "tvchart"
             {:client_id client-id :user_id user-id :chart_id chart-id}))


