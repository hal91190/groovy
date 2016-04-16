#!/usr/bin/env groovy
import groovy.transform.Field

import java.nio.file.Paths
import java.util.regex.Pattern

@Field final List PATTERNS = [
        new Tuple2(~/^h1. (?<title>.*)$/, '# ${title}' ),
        new Tuple2(~/^h2. (?<title>.*)$/, '## ${title}'),
        new Tuple2(~/^h3. (?<title>.*)$/, '### ${title}'),
        new Tuple2(~/^\* "(?<title>[^"]*)":(?<url>[^ \t]*)(?<comment> .*)?$/, '* [${title}](${url})${comment}'),
        new Tuple2(~/^\*\* "(?<title>[^"]*)":(?<url>[^ \t]*)(?<comment> .*)?$/, '  * [${title}](${url})${comment}'),
        new Tuple2(~/^\*\*\* "(?<title>[^"]*)":(?<url>[^ \t]*)(?<comment> .*)?$/, '    * [${title}](${url})${comment}')
        ]

def source = Paths.get args[0]
source.eachLine { line ->
    def nbMatch = 0
    PATTERNS.each { Tuple2 pattern ->
        def matcher = (line =~ pattern.first)
        if (matcher) {
            println line.replaceFirst(pattern.first, pattern.second)
            nbMatch++
        }
    }
    if (nbMatch == 0) {
        println line.replaceAll(~/"(?<title>[^"]*)":(?<url>[^ \t]*)/, '[${title}](${url})')
    }
}
