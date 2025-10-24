
# Basic message
search-result = Match found: {$value}.

# Mapped messages
search-result-mapped = Match found: {$lastName}, {$firstName}.

# Note that the default here is not wise.
search-result-mapped-fancy = Match found: {$lastName}, {$firstName} { $role ->
    [none] -> no longer with this organization
    *[other] -> role: {$role}
}.

# using our custom function
search-role = Found person { PEOPLE($value, field:"lastName") } with role: { PEOPLE($value, field:"role") }!


