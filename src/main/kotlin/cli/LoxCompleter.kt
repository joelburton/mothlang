package com.joelburton.mothlang.cli

import com.joelburton.mothlang.scanner.keywordsToTokens
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

internal class LoxCompleter(val words: Set<String>) : Completer {
    override fun complete(
        reader: LineReader,
        line: ParsedLine,
        candidates: MutableList<Candidate>
    ) {
        for (word in words) candidates.add(Candidate(word))
        for (keyword in keywordsToTokens.keys) candidates.add(Candidate(keyword))
    }
}

