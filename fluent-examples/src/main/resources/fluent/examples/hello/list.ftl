# keeping $userName instead of $userNames just for demonstration purposes in example
hello-users = Hello, { JOIN($userName, separator:", ", junction:", and ", pairSeparator:" and ") }!
