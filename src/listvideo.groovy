#!/usr/bin/env groovy
@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import groovy.json.JsonSlurper
import groovy.transform.ToString
import au.com.bytecode.opencsv.*

import java.text.Collator

import static groovy.io.FileType.FILES

def pattern = ~'.*\\.(avi|mpg|mkv|mp4)$'

def ffprobeCmd = ['ffprobe', '-v', 'quiet', '-of', 'json', '-show_format', '-show_streams']

String[] outputSchema = [
        'Titre', 'Titre (tri)', 'Année', 'Série', 'Épisode',
        'Nom', 'Conteneur', 'Durée (s)', 'Taille (o)', 'Bitrate',
        'Video idx', 'Codec vidéo', 'Largeur', 'Hauteur',
        'Audio idx', 'Codev audio', 'Canaux',
        'Sous-titre idx', 'Codec sous-titre', 'Langues'
]

@ToString
class VideoDescription {
    public static final int INFO_OFFSET = 0;
    public static final int FORMAT_OFFSET = 5;
    public static final int VIDEO_OFFSET = 10;
    public static final int AUDIO_OFFSET = 14;
    public static final int SUBTITLES_OFFSET = 17;

    File videoFile
    List format = []
    List video = []
    List audio = []
    List subtitles = []

    VideoDescription(File videoFile, Map videoDesc) {
        this.videoFile = videoFile
        loadFormat(videoDesc)
        loadStreams(videoDesc)
    }

    private void loadFormat(Map videoDesc) {
        format << videoDesc.format.filename.substring(2) // remove ./
        format << videoDesc.format.format_long_name
        format << videoDesc.format.duration
        format << videoDesc.format.size
        format << videoDesc.format.bit_rate
    }

    private void loadStreams(Map videoDesc) {
        videoDesc.streams.each {
            switch (it.codec_type) {
                case "video":
                    loadVideo(it)
                    break
                case "audio":
                    loadAudio(it)
                    break
                case "subtitle":
                    loadSubtitle(it)
                    break
            }
        }
    }

    private void loadVideo(Map stream) {
        video << [ stream.index, stream.codec_long_name, stream.width, stream.height, ]
    }

    private void loadAudio(Map stream) {
        audio << [ stream.index, stream.codec_long_name, stream.channel_layout ]
    }

    private void loadSubtitle(Map stream) {
        subtitles << [ stream.index, stream.codec_long_name, stream.tags.language ]
    }

    private List extractInfo() {
        def firstDotPosition = videoFile.name.indexOf('.')
        def basename = videoFile.name.substring(0, firstDotPosition)
        def info = [ basename, '', '', '', '' ]
        def openParPosition = basename.lastIndexOf('(')
        if (openParPosition != -1) {
            def closeParPosition = basename.lastIndexOf(')')
            info[0] = basename.substring(0, openParPosition - 1)
            info[2] = basename.substring(openParPosition + 1, closeParPosition)
        }
        basename = info[0]
        def firstDash = basename.indexOf(' - ')
        if (firstDash != -1) {
            String[] elements = basename.split(' - ')
            switch (elements.size()) {
                case 3:
                    info[0] = elements[2]
                    info[3] = elements[0]
                    info[4] = elements[1]
                    if (info[4] ==~ /\d{4}/) {
                        info[2] = info[4]
                    }
                    break
                case 2:
                    info[0] = elements[1]
                    info[3] = elements[0]
                    break
                default:
                    assert false : basename
            }
        }
        info[1] = info[0]
        def matcherStart = info[0] =~ /^(?i)(l'|le|la|les) /
        if (matcherStart) {
            def start = matcherStart.group(1)
            info[1] = "${info[0].substring(start.size() + 1)}, ${start}"
        }
        return info
    }

    String[] toArray() {
        List info = extractInfo()
        def videoTranspose = video.transpose()
        def audioTranspose = audio.transpose()
        def subtitlesTranspose = subtitles.transpose()
        def totalSize = info.size() + format.size() + videoTranspose.size() + audioTranspose.size() + subtitlesTranspose.size()
        String[] output = [""]*totalSize
        for (i in 0..info.size()-1) {
            output[INFO_OFFSET + i] = info[i]
        }
        for (i in 0..format.size()-1) {
            output[FORMAT_OFFSET + i] = format[i]
        }
        for (i in 0..videoTranspose.size()-1) {
            output[VIDEO_OFFSET + i] = videoTranspose[i].join(", ")
        }
        for (i in 0..audioTranspose.size()-1) {
            output[AUDIO_OFFSET + i] = audioTranspose[i].join(", ")
        }
        if (!subtitlesTranspose.isEmpty()) {
            for (i in 0..subtitlesTranspose.size() - 1) {
                output[SUBTITLES_OFFSET + i] = subtitlesTranspose[i].join(", ")
            }
        }
        output
    }
}

List<String[]> output = []

def currentdirectory = new File('.');
currentdirectory.eachFileRecurse(FILES) {
    def filename = it.name.toLowerCase()
    if (filename ==~ pattern) {
        System.err.println "Processing ${filename}..."
        def stdOut = new StringBuffer()
        def stdErr = new StringBuffer()
        def cmd = ffprobeCmd.collect()
        cmd << it
        def ffprobe = cmd.execute()
        ffprobe.waitForProcessOutput(stdOut, stdErr)
        def jsonSlurper = new JsonSlurper()
        def jsonVideoDesc = jsonSlurper.parseText(stdOut.toString())
        def videoDesc = new VideoDescription(it, jsonVideoDesc)
        output << videoDesc.toArray()
    } else {
        System.err.println "${filename} is not a video file..."
    }
}

output.sort { t1, t2 -> Collator.getInstance().compare(t1[0], t2[0]) }
output.add(0, outputSchema)

def writer = new CSVWriter(new OutputStreamWriter(System.out))
writer.writeAll output
writer.close()
