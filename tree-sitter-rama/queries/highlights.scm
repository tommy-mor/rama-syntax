(ramaop_definition name: (operator_name) @function)
(ramafn_definition name: (binding_name) @function)
(binding_name) @starVar (#match? @starVar "^\*")
(binding_name) @variable (#match? @variable "^%")
(anchor_reference) @label
(keyword) @property
(string_literal) @string
(comment) @comment
(pstate_reference) @type
