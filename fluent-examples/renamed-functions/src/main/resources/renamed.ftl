
# format a date.
date-implicit = The meeting is on { $date }. Don't be late! [implicit]
date-datetime = The meeting is on { DATETIME($date) }. Don't be late! [explicit call to DATETIME()]
date-temporal = The meeting is on { TEMPORAL($date, as:"RFC_1123_DATE_TIME") }. Don't be late! [explicit call to TEMPORAL()]

# Renamed
date-renamed = The meeting is on { SIMPLEDATE($date) }. Don't be late!
