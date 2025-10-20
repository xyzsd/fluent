# STRINGSORT function tests
#
#

# example of what happens when we combine strings and non-strings (numbers, here).
stringsort_heterogeneous = |{STRINGSORT(1, "three", "four", "9999.888", 2, order:"natural")}|

# function under test
# This test is simple. A better test would show examples under different locales.
stringsort_default = |{STRINGSORT($value)}|
stringsort_ascending = |{STRINGSORT($value, order:"natural")}|
stringsort_descending = |{STRINGSORT($value, order:"reversed")}|


