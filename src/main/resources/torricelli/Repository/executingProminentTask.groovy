import org.kohsuke.stapler.framework.io.LargeText

lt = jelly(LargeText);
L l = taglib(L)

l.layout(title:my.name) {
    l.left {
        H2(my.task.name)

        PRE(ID:"out")
        DIV(ID:"spinner") {
            img(LargeText,"spinner.gif")
        }
        lt.progressiveText(href:"task/log/progressText",spinner:"spinner",idref:"out")

        // TODO: we need this to show up when the progressText completes, too.
        if(my.task.isDone()) {
            FORM(ACTION:"task/clear") {
                INPUT(TYPE:"submit",VALUE:"OK")
            }
        }
    }
}