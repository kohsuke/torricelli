import org.kohsuke.stapler.framework.io.LargeText

lt = jelly(LargeText);

taglib(L).layout {
    H2("test")
    PRE(ID:"out")
    DIV(ID:"spinner") {
        img(LargeText,"spinner.gif")
    }
    lt.progressiveText(href:"logFile/progressText",idref:"out")
}