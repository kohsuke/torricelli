L l = taglib(L)

l.layout(title:that.name) {
    DIV(ID:"center",STYLE:"text-align:center; margin:1em;",
        "Check out this repository by: <tt style='margin-left:1em'>hg clone ${request.requestURL}</tt>")

    l.left {
        H2("Recent Changes")
        that.parse("/changelog").changelog.each { e ->
            DIV(CLASS:"changelog") {
                l.rev(e.@rev)
                text(" by ")
                l.author(e.@author)
                text(" ${e.@date} (${e.@age} ago)")

                DIV(CLASS:"comment",e.description.text())

                DIV(CLASS:"files") {
                    e.file.each { f ->
                        DIV(CLASS:"file") {
                            A(HREF:"rev/${e.@rev}/diff/${f.text()}",f.text())
                        }
                    }
                }

                def tags = e.tag
                if(!tags.isEmpty()) {
                    DIV(CLASS:"tags") {
                        text("<b>Tags</b>: "+tags*.text().join(","));
                    }
                }
            }
        }


        H2("Files")
        
    }

    l.right {
        l.nav([
            [HREF:"configure",  TITLE:"Configure"],
            [HREF:"tags",       TITLE:"Tags"]
        ])
    }
}