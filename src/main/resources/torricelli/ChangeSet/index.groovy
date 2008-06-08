L l = taglib(L)

l.layout(title:my.key) {
    l.left {
        // TODO: prev/next link

        my.parse("/changelog").changelog.each { e ->
            DIV(CLASS:"changelog") {
                l.rev("?rev="+e.@rev,e.@rev)
                text(" by ")
                l.author(e.@author)
                text(" ${e.@date} (${e.@age} ago)")

                e.tag.each { t ->
                    text(' ')
                    SPAN(CLASS:"csTag", t.text())
                }

                DIV(CLASS:"comment",e.description.text())

                DIV(CLASS:"files") {
                    e.file.each { f ->
                        DIV(CLASS:"file") {
                            A(HREF:f.text(), f.text())
                            text(' (')
                            int rev = Integer.parseInt(e.@rev);
                            A(HREF:"${f.text()}?r1=${rev-1}&r2=${rev}", "diff")
                            text(')')
                        }
                    }
                }
            }
        }

        include(my.getRev("tip").dirTree(),"files.groovy")
    }
}