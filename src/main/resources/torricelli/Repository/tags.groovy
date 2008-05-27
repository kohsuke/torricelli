L l = taglib(L)

l.layout {
    l.left {
        UL {
            that.parse("/tags").tag.each {
                LI(it.@node)
            }
    }
}
