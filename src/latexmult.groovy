#!/usr/bin/env groovy
def dim = args[0].toInteger()
1.upto(dim) { a ->
//    def ligne = (1..10).collect { it * a }
//    println "${a} & ${ligne.join(' & ')}\\\\"
    print "${a} & "
    1.upto(dim) { b ->
        def prefix = ''
        def suffix = ''
        if (b < a) {
            prefix = '\\textcolor{Gray}{'
            suffix = '}'
        }
        print "${prefix}${a * b}${suffix}"
        if (b < dim) {
            print ' & '
        }
    }
    println '\\\\'
}
