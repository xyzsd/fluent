# BOOLEAN function tests
#
#

# no value (error!)
msg_no_booleans = |{BOOLEAN()}|

# A string is not a boolean. It is just passed through.
msg_not_boolean = |{BOOLEAN("false")}|
msg_not_boolean_2 = |{BOOLEAN("This is a String")}|
msg_not_boolean_3 = |{BOOLEAN("This is a String", as:"number")}|

# single variable
msg_boolean = |{BOOLEAN($value)}|

# multiple variables
msg_multiple_variables = |{BOOLEAN($value_1, $value_2)}|

# String output explicitly specified (same as 'msg_boolean' above)
msg_boolean_as_string = |{BOOLEAN($value, as:"string")}|

# Function under test with conversion to number
msg_boolean_as_number = |{BOOLEAN($value, as:"number")}|

# let's offset the converted boolean
msg_boolean_adjusted = |{OFFSET( BOOLEAN($value, as:"number"), increment:10)}|

# selector example
select_boolean = |{ $value ->
    [true] Success!
    *[false] Failure.
}|