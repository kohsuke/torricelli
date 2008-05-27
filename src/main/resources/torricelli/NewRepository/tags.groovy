L l = taglib(L)

l.layout(title:that.name+" \u00BB Tags") {
    l.left {
        UL {
            that.parse("/tags").tag.each { tag ->
                LI {
                    A(HREF:"rev/"+tag.@node, tag.@name)
                }
            }
        }
    }
}
