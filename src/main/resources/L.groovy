import org.kohsuke.stapler.Stapler;
import torricelli.Repository;

def layout(body) {
    layout [:],body
}

def layout(args,body) {
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
                        if(args.title==null)     title = "Torricelli";
                        else                     title = "Torricelli \u00BB "+args.title;
                        A([HREF:"${rootURL}/"],title)
                    }
                }

                DIV(ID:"content") {
                    body();
                    DIV(ID:"clear")
                }

                DIV(ID:"footer") {
                    raw("""
                        Powered by <a href="https://torricelli.dev.java.net/">Torricelli</a>
                        &nbsp;&nbsp;&nbsp;
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
    def uri = request.getRequestURI();
    UL(ID:"nav") {
        navDefs.each { n ->
            LI {
                A(HREF:n.HREF,CLASS:(uri.endsWith(n.HREF)?"current":null), n.TITLE)
            }
        }
    }
}

/**
 * Generates a hyperlink to a specific revision
 */
def rev(String href, String rev) {
    A(CLASS:"csRevision",HREF:href,rev)
}

/**
 * Generates a hyperlink to a specific revision
 */
def rev(String rev) {
    def href = Stapler.currentRequest.findAncestor(Repository).url+"/changeSet/"+rev;
    A(CLASS:"csRevision",HREF:href,rev)
}

def author(String author) {
    SPAN(CLASS:"csAuthor",author)
}

/**
 * Generates a tag list.
 */
def tags(tagList) {
    tagList.each { t ->
        text(' ')
        SPAN(CLASS:"csTag", t)
    }
}

def img(Map args, String href) {
    args.SRC = "${rootURL}/img/${href}"
    IMG(args)
}

def img(String href) {
    img([:],href)
}
