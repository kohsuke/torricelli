L l = taglib(L)

H2("Files")
TABLE(ID:"files") {
    my.parseFileSummary().file.each { f ->
        TR {
            TD { IMG(SRC:"${rootURL}/img/16x16/text.gif") }
            TD { A(HREF:f.@name,f.@name) }
            TD { l.rev(f.@rev) }
            // TODO: age
            TD { l.author(f.author.text()) }
        }
        TR(CLASS:"comment") {
            TD()
            TD(COLSPAN:3, f.summary.text())
        }
    }
}
