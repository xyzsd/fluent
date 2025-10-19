# NUMSORT function tests
#
#

# invalid (no passthrough!)
numsort_invalid = |{NUMSORT(1, 2, 3, "string")}|

# function under test
numsort_default = |{NUMSORT($value)}|
numsort_ascending = |{NUMSORT($value, order:"ascending")}|
numsort_descending = |{NUMSORT($value, order:"descending")}|


# combo
# we want to control formatting of the displayed numbers
# we will use a skeleton for engineering notation, and always display the sign unless it is zero
# (see: https://github.com/unicode-org/icu/blob/main/docs/userguide/format_parse/numbers/skeletons.md)
numsort_combo = |{ NUMBER( NUMSORT($value), skeleton:"scientific/sign-except-zero" ) }|