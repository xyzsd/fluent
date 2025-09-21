# XTEMPORAL
#
month_raw = September is month { XTEMPORAL($temporal, field:"MONTH_OF_YEAR") }.

month_ordinal = We are in the { NUMBER(XTEMPORAL($temporal, field:"MONTH_OF_YEAR"), kind:"ordinal") ->
    [one] {XTEMPORAL($temporal, field:"MONTH_OF_YEAR")}st
    [two] {XTEMPORAL($temporal, field:"MONTH_OF_YEAR")}nd
    [few] {XTEMPORAL($temporal, field:"MONTH_OF_YEAR")}rd
    *[other] {XTEMPORAL($temporal, field:"MONTH_OF_YEAR")}th
} month of the year.

