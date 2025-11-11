# Simple things are simple.
hello-user = Hello, {$userName}!

# Complex things are possible.
shared-photos =
{$userName} {$photoCount ->
[one] added a new photo
*[other] added {$photoCount} new photos
} to {$userGender ->
[male] his stream
[female] her stream
*[other] their stream
}.

# instead of the default list format (in English, comma-separated),
# use and 'and' if there are 2 or more items.
hello-all-users = Hello, { LIST($users, type:"and", width:"wide") }!