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


# simple selection example
simple = {$photoCount ->
 [one] added a new photo
*[other] added {$photoCount} new photos
}.

# slightly more complex. just to simplify our code example.
example-selection = {$userName} {$photoCount ->
 [one] added a new photo
*[other] added {$photoCount} new photos
}


# A formatted list
formatted-list = { LIST($data, type:"and", width:"wide") }.