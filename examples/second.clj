(deframaop intake-retry
  [*agent-name {:keys [*agent-task-id *agent-id *expected-retry-num]}]
  (<<with-substitutions
   [$$root (po/agent-root-task-global *agent-name)
    $$stream-shared (po/agent-stream-shared-task-global *agent-name)
    *agent-graph (po/agent-graph-task-global *agent-name)]
   (hook:received-retry *agent-task-id *agent-id *expected-retry-num)
   (local-select> (keypath *agent-id)
                  $$root
                  :> {*root-invoke-id :root-invoke-id
                      *curr-retry-num :retry-num
                      *graph-version :graph-version
                      *args :invoke-args
                      *result :result
                      *source :source
                      *metadata :metadata

                      {:keys [*fork-context
                              *parent-root-invoke-id]}
                      :fork-of})
   ;; - this is mostly a sanity check, though it is technically possible for
   ;; multiple retries to come through from stall checker if it runs multiple
   ;; times before any retries are processed (e.g. stream topology is paused)
   ;; - don't need to remove from active-invokes in this case since writing
   ;; result and removing from active-invokes is done atomically
   (filter> (nil? *result))
   ;; if it got GC'd, ignore
   (filter> (some? *root-invoke-id))
   (filter> (= *expected-retry-num *curr-retry-num))
   (hook:running-retry> *agent-task-id *agent-id *expected-retry-num)
   (fetch-graph-version *agent-name :> *curr-graph-version)
   (<<cond
    (case> (= *curr-graph-version *graph-version))
     (identity :continue :> *handle-mode)

    (case> (= *curr-graph-version (inc *graph-version)))
     (po/agent-graph-task-global *agent-name :> {*handle-mode :update-mode})

    (default>)
     ;; if somehow two or more module updates got through before the retry could
     ;; be processed, drop the retry since don't know if it's valid to continue
     ;; it
     (identity :drop :> *handle-mode))

   (inc *expected-retry-num :> *retry-num)

   (<<if (= :drop *handle-mode)
     (complete-with-failure! *agent-name *agent-id "Retry dropped")
    (else>)
     (<<if (= :restart *handle-mode)
       (local-transform> [:gc-root-invokes (keypath *root-invoke-id) (termval nil)]
                         $$stream-shared)
       (init-root *agent-name *agent-id *retry-num *args *metadata *source :> *root-invoke-id)
      (else>)
       (identity *root-invoke-id :> *root-invoke-id))

     (anode/read-config *agent-name
                        aor-types/MAX-RETRIES-CONFIG
                        :> *max-retries)
     (<<if (> *retry-num *max-retries)
       (complete-with-failure! *agent-name *agent-id "Max retry limit exceeded")
      (else>)
       (local-transform> [(keypath *agent-id)
                          (multi-path
                           [:retry-num (termval *retry-num)]
                           [:graph-version
                            (termval *curr-graph-version)]
                           [:ack-val (termval (h/half-uuid *root-invoke-id))])]
                         $$root)

       (aor-types/->valid-NodeOp *root-invoke-id
                                 *parent-root-invoke-id
                                 *fork-context
                                 (get *agent-graph :start-node)
                                 *args
                                 nil
                                 :> *op)
       (:> *agent-task-id
           *agent-id
           (aor-types/->valid-AgentExecutionContext *metadata *source)
           *retry-num
           *op)
     ))))
