# NumberFn tests
#
# implicit
msg_implicit = |{$list}|

#
list_default = |{LIST($list)}|
list_and = |{LIST($list, type:"and")}|
list_or = |{LIST($list, type:"or")}|
list_unit = |{LIST($list, type:"units")}|

# invalid ('unit' instead of 'units')
list_bad_1 = |{LIST($list, type:"unit", width:"wide")}|
list_bad_2 = |{LIST($list, width:"reallyhuge")}|

# width wide
list_default_wide = |{LIST($list, width:"wide")}|
list_and_wide = |{LIST($list, type:"and", width:"wide")}|
list_or_wide = |{LIST($list, type:"or", width:"wide")}|
list_unit_wide = |{LIST($list, type:"units", width:"wide")}|

# width short
list_default_short = |{LIST($list, width:"short")}|
list_and_short = |{LIST($list, type:"and", width:"short")}|
list_or_short = |{LIST($list, type:"or", width:"short")}|
list_unit_short = |{LIST($list, type:"units", width:"short")}|

# combined with number format, implicit and explicit
list_default_percent = |{NUMBER($list, style:"PERCENT")}|
list_or_percent = |{LIST(NUMBER($list, style:"PERCENT"), type:"or", width:"wide")}|