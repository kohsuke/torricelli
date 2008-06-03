import org.kohsuke.scotland.core.FormTags;
L l = taglib(L)
FormTags f = taglib(org.kohsuke.scotland.core.FormTags)

l.layout(title:my.name) {
    l.left {
        H2(my.name+" Configuration")

        FORM(METHOD:"post",ACTION:"configSubmit") {
            TABLE(STYLE:"width:100%") {
                f.entry(name:"Description") {
                    TEXTAREA(CLASS:"setting-input",NAME:"description", my.description)
                }
                f.entry(name:"Upstream") {
                    SELECT(CLASS:"setting-input",NAME:"upstream") {
                        OPTION(VALUE:"(none)","(none)")
                        my.group.listRepositories().each { r ->
                            OPTION(SELECTED:my.upstream==r?"true":null, r.name)
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