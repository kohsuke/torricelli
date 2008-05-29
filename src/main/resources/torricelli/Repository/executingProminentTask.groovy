import org.kohsuke.stapler.framework.io.LargeText

lt = jelly(LargeText);
L l = taglib(L)

l.layout(title:that.name) {
    l.left {
        H2(that.task.name)

        PRE(ID:"out")
        DIV(ID:"spinner") {
            img(LargeText,"spinner.gif")
        }
        lt.progressiveText(href:"task/log/progressText",spinner:"spinner",idref:"out")

        // TODO: we need this to show up when the progressText completes, too.
        if(that.task.isDone()) {
            FORM(ACTION:"task/clear") {
                INPUT(TYPE:"submit",VALUE:"OK")
            }
        }
    }
}