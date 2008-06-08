L l = taglib(L)

l.layout(title:my.key) {
    l.left {
        // TODO: prev/next link

        // make sure we got the data to display
        my.parse();

        DIV {
            l.rev(my.node)
            text(" by ")
            l.author(my.author)
            text(" on ${my.dateString} ")
            l.tags(my.tags)
        }
        PRE(my.description)

        UL {
            my.files.each { f->
                LI {
                    A(HREF:my.parent.url+'/browse/'+f,f)
                    // TODO: diff
                }
            }
        }
    }
}