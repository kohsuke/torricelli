L l = taglib(L)

// make sure we got the data to display
my.parse();

l.layout(title:my.key) {
    // previous/next links
    TABLE(CLASS:"changeSetNav") {
        Iterator prevs = my.parents.iterator();
        Iterator nexts = my.children.iterator();
        while(prevs.hasNext() || nexts.hasNext()) {
            TR {
                TD(CLASS:"prev") {
                    if(prevs.hasNext()) {
                        l.img("24x24/previous.gif")
                        l.rev(prevs.next())
                    }
                }
                TD(CLASS:"next") {
                    if(nexts.hasNext()) {
                        l.rev(nexts.next())
                        l.img("24x24/next.gif")
                    }
                }
            }
        }
    }

    l.left {
        DIV(STYLE:"clear:both") {
            text("ChangeSet ")
            l.rev(my.node)
            text(" by ")
            l.author(my.author)
            text(" on ${my.dateString} ")
            raw("(<A HREF='patch'>patch</A>) ")
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