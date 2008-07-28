import org.kohsuke.scotland.core.FormTags
import torricelli.L;

FormTags f = taglib(org.kohsuke.scotland.core.FormTags)

f.entry(name:"URL") {
    f.textBox("url");
}
