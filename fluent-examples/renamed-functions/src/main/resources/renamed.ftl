
# format a date.
date-implicit = The meeting is on { $date }. Don't be late! [implicit]
date-datetime = The meeting is on { DATETIME($date) }. Don't be late! [DATETIME()]
date-temporal = The meeting is on { TEMPORAL($date, as:"RFC_1123_DATE_TIME") }. Don't be late! [TEMPORAL()]

# Renamed
date-renamed = The meeting is on { SIMPLEDATE($date) }. Don't be late!
