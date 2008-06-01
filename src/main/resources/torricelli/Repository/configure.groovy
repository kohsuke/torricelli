L l = taglib(L)
F f = taglib(F)

l.layout(title:my.name) {
    l.left {
        H2(my.name+" Configuration")

        TABLE {
            f.entry(name:"Upstream") {
                text("Hello!")
            }
        }
    }
}