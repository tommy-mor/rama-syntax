(deframaop send-emits>
  [*agent-name *agent-task-id *agent-id *retry-num *invoke-id *agg-invoke-id
   *emits *result *stats *fork-context]
  (<<with-substitutions
   [$$root (po/agent-root-task-global *agent-name)
    $$stream-shared (po/agent-stream-shared-task-global *agent-name)]
   (anchor> <root>)
   (ops/explode *emits
                :> {:keys [*invoke-id *fork-invoke-id *target-task-id
                           *node-name *args]
                    :as   *emit})
   (hook:emit> *emit)
   (apart/|aor [*agent-name *agent-task-id *agent-id *retry-num]
               |direct
               *target-task-id)
   (aor-types/->valid-NodeOp
    *invoke-id
    *fork-invoke-id
    *fork-context
    *node-name
    *args
    *agg-invoke-id
    :> *op)
   (anchor> <regular-emit>)

   (hook> <root>)
   (mapv (comp h/half-uuid :invoke-id) *emits :> *next-ack-vals)
   (reduce bit-xor (h/half-uuid *invoke-id) *next-ack-vals :> *ack-val)
   (apart/|aor [*agent-name *agent-task-id *agent-id *retry-num]
               |direct
               *agent-task-id)
   ;; <<atomic here only because tests override the hook to elide this
   (<<atomic
     (hook:update-last-progress>)
     (local-transform>
      [(keypath *agent-id)
       (multi-path [:last-progress-time-millis (termval (h/current-time-millis))]
                   [:stats (term (stats/agent-stats-merger *stats))])]
      $$root))

   (<<if (some? *result)
     (hook:writing-result *agent-task-id *agent-id *result)
     ;; if race with retry and it happened to have finished, don't change the
     ;; result here – this can happen if the agent has other branches that fail
     ;; besides the one that created the result
     (h/current-time-millis :> *finish-time-millis)
     (local-transform>
      [(keypath *agent-id)
       (selected? :result nil?)
       (multi-path [:result (termval *result)]
                   [:finish-time-millis (termval *finish-time-millis)])]
      $$root)
     (local-transform> [:active-invokes (set-elem *agent-id) NONE>] $$stream-shared))
   (<<if (some? *agg-invoke-id)
     (aor-types/->valid-AggAckOp *agg-invoke-id *ack-val :> *op)
     (anchor> <agg-ack-emit>)
    (else>)
     (<<ramafn %update-ack-val
       [*v]
       (:> (bit-xor *v *ack-val)))
     (local-transform>
      [(keypath *agent-id)
       :ack-val
       (term %update-ack-val)]
      $$root)
     (local-select> (keypath *agent-id)
                    $$root
                    :> {*root-ack-val :ack-val *result :result})
     (<<if (= 0 *root-ack-val)
       (<<if (nil? *result)
         (h/current-time-millis :> *finish-time-millis)
         (local-transform>
          [(keypath *agent-id)
           (multi-path [:result
                        (termval (aor-types/->AgentResult
                                  "Agent completed without result"
                                  true))]
                       [:finish-time-millis (termval *finish-time-millis)])]
          $$root))
       (finished-streaming-chunk :> *finished-streaming-chunk)
       (local-transform>
        [(keypath *agent-id)
         :streaming
         MAP-VALS
         :all
         AFTER-ELEM
         (termval *finished-streaming-chunk)]
        $$root))
   )

   (unify> <regular-emit> <agg-ack-emit>)
   (:> *op)))
