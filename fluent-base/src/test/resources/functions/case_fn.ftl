# CASE function tests
#
#

# function under test
msg_case_default = |{CASE($value)}|
msg_case_upper = |{CASE($value, style:"upper")}|
msg_case_lower = |{CASE($value, style:"lower")}|

# multiple positionals
msg_case_multi = |{CASE($value_1, $value_2, style:"upper")}|

# errors
msg_case_invalid = |{CASE($value, style:"custom")}|
