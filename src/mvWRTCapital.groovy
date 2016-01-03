#!/usr/bin/env groovy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.Normalizer

import static groovy.io.FileType.FILES

def computeDestinationDirectory = { String filename ->
    if (filename.empty) return ""

    def filenameWithoutAccent = Normalizer.normalize(filename, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")

    def destDir = filenameWithoutAccent.toUpperCase()[0] // default case -> first letter in upper case

    def destDirIfMatch = {
        def matcher = filenameWithoutAccent =~ it
        if (matcher.find() && matcher.end() < filenameWithoutAccent.size()) {
            destDir = filenameWithoutAccent[matcher.end()].toUpperCase()
        }
        destDir
    }
    destDirIfMatch ~/^(L'|l')/
    destDirIfMatch ~/^(La|la|Le|le|Les|les) /

    if (destDir ==~ /\W/) {
        destDir = ''
    }

    if (destDir ==~ /\d/) {
        destDir = '0-9'
    }

    destDir
}

def testComputeDestinationDirectory = {
    assert computeDestinationDirectory("abcd") == "A"
    assert computeDestinationDirectory("àbcd") == "A"
    assert computeDestinationDirectory("Ébcd") == "E"
    assert computeDestinationDirectory("1234") == "0-9"
    assert computeDestinationDirectory("L'abcd") == "A"
    assert computeDestinationDirectory("l'abcd") == "A"
    assert computeDestinationDirectory("La abcd") == "A"
    assert computeDestinationDirectory("la abcd") == "A"
    assert computeDestinationDirectory("Le abcd") == "A"
    assert computeDestinationDirectory("le abcd") == "A"
    assert computeDestinationDirectory("Les abcd") == "A"
    assert computeDestinationDirectory("les abcd") == "A"
    assert computeDestinationDirectory("l' (1234)") == ""
    assert computeDestinationDirectory("les (1234)") == ""
    assert computeDestinationDirectory("l'") == "L"
    assert computeDestinationDirectory("La") == "L"
    assert computeDestinationDirectory("") == ""
    assert computeDestinationDirectory("Vingt mille lieues sous les mers (1954).mkv") == "V"
}

testComputeDestinationDirectory()

def currentdirectory = new File('.');
currentdirectory.eachFile(FILES) {
    def filename = it.name
    def destDir = computeDestinationDirectory(filename)

    Path srcPath = Paths.get(filename)
    Path destPath = Paths.get(destDir)
    if (Files.notExists(destPath)) {
        Files.createDirectory(destPath)
    }
    Path destFile = Paths.get(destDir, filename)
    printf("%s -> %s\n", srcPath, destFile)
    Files.move(srcPath, destFile)
}
