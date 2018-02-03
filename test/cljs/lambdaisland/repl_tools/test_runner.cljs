(ns lambdaisland.repl-tools.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [lambdaisland.repl-tools.core-test]))

(enable-console-print!)

(doo-tests 'lambdaisland.repl-tools.core-test)
