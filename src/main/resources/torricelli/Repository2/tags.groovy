L l = taglib(L)

l.layout(title:my.name+" \u00BB Tags") {
    l.left {
        UL {
            my.parse("/tags").tag.each { tag ->
                LI {
                    A(HREF:"rev/"+tag.@node, tag.@name)
                }
            }
        }
    }
}
