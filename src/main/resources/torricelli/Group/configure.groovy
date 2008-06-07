L l = taglib(L)

l.layout(title:my.name) {
    l.left {
        H2(my.name+" Configuration")

        FORM(METHOD:"post",ACTION:"configSubmit") {
            text("there's nothing to configure yet")            
        }
    }


        l.right {
            l.nav([
                [HREF:"delete",  TITLE:"Delete this group"]
            ])
        }
}