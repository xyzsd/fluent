# misc select tests
#

selection = { $value ->
  *[ENUMAAA] The first and default value, ENUMAAA
   [ENUMBBB] The second value, ENUMBBB
   [ENUMCCC] The third value, ENUMCCC
}.

selection_lowercase = { $value ->
  *[enumaaa] The first and default value, ENUMAAA
   [enumbbb] The second value, ENUMBBB
   [enumccc] The third value, ENUMCCC
}.

selection_underscore = { $value ->
  *[ENUM_AAA] The first and default value, ENUM_AAA
   [ENUM_BBB] The second value, ENUM_BBB
   [ENUM_CCC] The third value, ENUM_CCC
}.


# This is not legal -- an identifier must start with A-Z or a-z, NOT an underscore.
# This entry "selection_underscore_nope" will not be present in resource.entries(), and will be in resource.junk()
selection_underscore_nope = { $value ->
 *[ENUM_AAA] The first and default value, ENUM_AAA
  [_ENUM_BBB] The second value, ENUM_BBB
  [_ENUM_CCC] The third value, ENUM_CCC
}.