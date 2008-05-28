import org.kohsuke.stapler.framework.io.LargeText

lt = jelly(LargeText);

taglib(L).layout {
    H2("test")
    PRE(ID:"out")
    DIV(ID:"spinner") {
        IMG(SRC:"spinner.gif")
    }
    lt.progressiveText(href:"href",idref:"out")
}