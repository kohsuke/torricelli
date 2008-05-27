def layout(body) {
    def rootURL = request.contextPath // TODO
    response.contentType = "text/html;charset=UTF-8"
    response.outputStream.println '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">'

    HTML {
        HEAD {
            TITLE("Torricelli")
            LINK(REL:"stylesheet",TYPE:"text/css",HREF:"${rootURL}/css/style.css")
        }
        BODY {
            DIV(ID:"wrap") {
                DIV(ID:"top") {
                    H2 {
                        A([HREF:"${rootURL}/"],"Torricelli")
                    }
                }

                DIV(ID:"content") {
                    body();
                    DIV(ID:"clear")
                }

                DIV(ID:"footer") {
                    P("""
                        Powered by <a href="https://torricelli.dev.java.net/">Torricelli</a>
                        <st:nbsp/><st:nbsp/><st:nbsp/>
                        HTML design by <a href="http://loadfoo.org/" rel="external">LoadFoO</a>
                    """)
                }
            }
        }
    }
}

def left(body) {
    DIV(ID:"left",body)
}

def right(body) {
    DIV(ID:"right",body)
}

def nav(List navDefs) {
    UL(ID:"nav") {
        navDefs.each {
            LI("<A HREF='${it.HREF}'>${it.TITLE}</A>")
        }
    }
}