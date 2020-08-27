Identity кодера - строка, которая должна быть одинаковой у кодеров, работающих с одними и теми же данными
(совместимые кодеры должны иметь одинаковую identity)

Предлагается брать за identity строковое представление кодируемого типа в Котлине

Например:
 - Для кодера для интов identity будет "Int"
 - Для кодера даблов "Double"
 - Для кодера списка строк "List<String>"
 - Для кодера списка списков строк "List<List<String>>"

Для кодера композитного объекта предлагается делать identity вида "Object<A, B, C, D>"

Для пользовательского кодера пользователь сам определяет его identity как хочет