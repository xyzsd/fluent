# NumberFn tests
#
# implicit
msg_implicit = |{$value}|

#
min_integer_a = |{NUMBER($value, minimumIntegerDigits:0)}|
min_integer_b = |{NUMBER($value, minimumIntegerDigits:4)}|

#
grouping_a = |{NUMBER($value, useGrouping:"ALWAYS")}|
grouping_b = |{NUMBER($value, useGrouping:"FALSE")}|
grouping_c = |{NUMBER($value, useGrouping:"AUTO")}|

#
style_currency = |{NUMBER($value, style:"CURRENCY")}|
style_currency_alt = |{NUMBER($value, style:"CURRENCY", unitDisplay:"LONG")}|
style_percent = |{NUMBER($value, style:"PERCENT")}|
style_percent_alt = |{NUMBER($value, style:"PERCENT", unitDisplay:"LONG")}|

#
fx_min_0 = |{NUMBER($value, minimumFractionDigits:0)}|
fx_min_4 = |{NUMBER($value, minimumFractionDigits:4)}|
fx_max_0 = |{NUMBER($value, maximumFractionDigits:0)}|
fx_max_2 = |{NUMBER($value, maximumFractionDigits:2)}|
fx_min_2_max_2 = |{NUMBER($value, minimumFractionDigits:2, maximumFractionDigits:2)}|
fx_min_0_max_3 = |{NUMBER($value, minimumFractionDigits:0, maximumFractionDigits:3)}|
fx_minmax_bad = |{NUMBER($value, minimumFractionDigits:10, maximumFractionDigits:1)}|

#
sig_min = |{NUMBER($value, minimumSignificantDigits:6)}|
sig_minmax_confusing = |{NUMBER($value, minimumSignificantDigits:10, maximumSignificantDigits:1)}|
sig_max_4 = |{NUMBER($value, maximumSignificantDigits:4)}|
