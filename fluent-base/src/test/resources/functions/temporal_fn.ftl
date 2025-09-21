
# ERRORS
invalid_pattern = {TEMPORAL($temporal, pattern:"badpattern")}
missing_required = {TEMPORAL($temporal)}
invalid_predefined = {TEMPORAL($temporal, as:"badpredefined")}

# PREDEFINED
predefined_basic_iso_date = {TEMPORAL($temporal, as:"BASIC_ISO_DATE")}
predefined_iso_time = {TEMPORAL($temporal, as:"iso_time")}
predefined_rfc_1123 = {TEMPORAL($temporal, as:"RFC_1123_DATE_TIME")}

# PATTERN EXAMPLES
pattern_date = {TEMPORAL($temporal, pattern:"yyyy-MM-dd")}
pattern_time = {TEMPORAL($temporal, pattern:"HH:mm:ss.S")}
pattern_both = {TEMPORAL($temporal, pattern:"yyyy-MM-dd, HH:mm:ss.S")}