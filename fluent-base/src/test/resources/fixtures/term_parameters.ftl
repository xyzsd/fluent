-term = { $arg ->
   *[key] Value
}

key01 = { -term }
key02 = { -term () }
key03 = { -term(arg: 1) }
key04 = { -term("positional", narg1: 1, narg2: 2) }


-anotherterm = { $arg ->
       [first] Value1
       [second] Value2
      *[key] Value
}

key201 = { -anotherterm }
key202 = { -anotherterm () }
key203 = { -anotherterm(arg: 1) }
key204 = { -anotherterm("positional", narg1: 1, narg2: 2) }
key205 = { -anotherterm(arg: "first") }
key206 = { -anotherterm(arg: "second") }

