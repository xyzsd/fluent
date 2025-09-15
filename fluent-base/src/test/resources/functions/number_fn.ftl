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

#
# Skeletons: see https://unicode-org.github.io/icu/userguide/format_parse/numbers/skeletons.html#examples
#
skeleton_too_many_options = |{NUMBER($value, skeleton:"my skeleton", minimumSignificantDigits:6)}|
skeleton_invalid_skeleton = |{NUMBER($value, skeleton:"scary skeleton")}|
skeleton_one = |{NUMBER($value, skeleton:"compact-long")}|
skeleton_one_concise = |{NUMBER($value, skeleton:"KK")}|
skeleton_two = |{NUMBER($value, skeleton:"scary skeleton")}|

#
cardinal_simple = |{NUMBER($value, kind: "cardinal")}|

# cardinal selectors
unreadEmails_implicit = { $value ->
    [one] You have one unread message.
    *[other] You have { $value } unread messages.
}

unreadEmails_explicit = { NUMBER($value, kind:"cardinal", ) ->
    [one] You have one unread message.
    *[other] You have { $value } unread messages.
}

# ordinal selectors (never implicit)
turnRightMessage = { NUMBER($value, kind:"ordinal") ->
    [one] Take the {NUMBER($value)}st right.
    [two] Take the {NUMBER($value)}nd right.
    [few] Take the {NUMBER($value)}rd right.
    *[other] Take the {NUMBER($value)}th right.
}

# Exact selector (for number)
# Remember, if the value being selected on is a string, we do an exact match against variants.
# But if the value being selected is a number, we default to a cardinal (plural) match, unless we override
# (e.g., with 'exact' or 'ordinal')
exactMessageExample = { NUMBER($value, kind:"exact") ->
    [0] Zero.
    [2.5] Two point five.
    [3] Three.
    *[other] The value is {$value}.
}
