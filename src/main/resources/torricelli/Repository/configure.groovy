import org.kohsuke.scotland.core.FormTags;
L l = taglib(L)
FormTags f = taglib(org.kohsuke.scotland.core.FormTags)

l.layout(title:my.name) {
    l.left {
        H2(my.name+" Configuration")

        FORM(METHOD:"post",ACTION:"configSubmit") {
            TABLE(STYLE:"width:100%") {
                f.entry(name:"Description") {
                    TEXTAREA("foo")
                }
                f.entry(name:"Upstream") {
                    SELECT {
                        app.listRepositories().each { r ->
                            OPTION(r.name)
                        }
                    }
                }
            }
            DIV(STYLE:"text-align:right") {
                INPUT(TYPE:"submit",VALUE:"OK")
            }
        }
    }
}