# COUNT function tests
#
#

# nothing to count
count_none = |{COUNT()}|

# literal counts
count_literal = |{COUNT("string","string2",5.0,111)}|

# one value (most common)
# value could be one item or a list (sequenced collection)
count_one_variable = |{COUNT($value)}|

# multiple variables
count_multiple_variables = |{COUNT($value_1, $value_2, $value_3)}|

# variables and literals (? why ?)
count_mixed = |{COUNT($value_1, "String Value", $value_2, 111)}|


