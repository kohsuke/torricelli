import org.kohsuke.scotland.dir.DirectoryTags;

L l = taglib(L)
DirectoryTags dt = taglib(DirectoryTags);

l.layout(title:my.name) {
    DIV(ID:"center",STYLE:"text-align:center; margin:1em;") {
        raw("Check out this repository by: <tt style='margin-left:1em'>hg clone ${request.requestURL}</tt>")
    }

    l.left {
        H2("Recent Changes")
        my.parse("/changelog").changelog.each { e ->
            DIV(CLASS:"changelog") {
                l.rev(e.@rev)
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
                            A(HREF:"rev/${e.@rev}/diff/${f.text()}",f.text())
                        }
                    }
                }
            }
        }

        include(my.getRev("tip").dirTree(),"files.groovy")
    }

    l.right {
        l.nav([
            [HREF:"configure",  TITLE:"Configure"],
            [HREF:"tags",       TITLE:"Tags"]
        ])

        DIV(CLASS:"box") {
            H2("Directories")
            DIV(CLASS:"dirtree") {
                my.getRev("tip").dirTree().listDirs(dt);
            }
        }
    }
}