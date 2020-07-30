class lib {
    fun call(f: (Int) -> String, a: Int): String {
        return f(a)
    }

    fun callUpTo(f: (Int) -> String, a: Int): List<String> {
        return sequence {
            for (i in 0..a) {
                yield(f(i))
            }
        }.toList()
    }
}