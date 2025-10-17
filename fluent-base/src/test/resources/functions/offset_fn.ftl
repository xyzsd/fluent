# OFFSET function tests
#
#

# parse exceptions (named arguments cannot be variables)
msg_offset_variable = |{OFFSET($value, increment:$offset)}|

# error conditions (using literals)
msg_string_inc = |{OFFSET("ALiteralString", increment:1)}|
msg_string_dec = |{OFFSET("ALiteralString", decrement:1)}|
msg_float_inc = |{OFFSET(3.14, increment:1)}|
msg_float_dec = |{OFFSET(3.14, decrement:1)}|
msg_invalid_inc_f = |{OFFSET(10, increment:3.5)}|
msg_invalid_dec_f = |{OFFSET(10, decrement:3.5)}|
msg_invalid_inc_s = |{OFFSET(10, increment:"ALiteralString")}|
msg_invalid_dec_s = |{OFFSET(10, decrement:"ALiteralString")}|
msg_invalid_badoptions = |{OFFSET(10, increment:10, decrement:5)}|


# basic messages
msg_offset_inc = |{OFFSET($value, increment:10)}|
msg_offset_dec = |{OFFSET($value, decrement:10)}|

