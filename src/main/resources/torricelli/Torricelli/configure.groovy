package torricelli.Torricelli;

import org.kohsuke.scotland.core.FormTags;
import org.kohsuke.scotland.extensibility.Descriptor
import org.kohsuke.scotland.extensibility.DescriptorFormTags
import torricelli.listeners.CommitListener
import torricelli.L

L l = taglib(L)
FormTags f = taglib(FormTags);
DescriptorFormTags ft = taglib(DescriptorFormTags)

l.layout {
    H2("Configuration")

    FORM {
        TABLE(STYLE:"width:100%") {
            f.block {
                ft.heteroList("notifier",null,true,Descriptor.ALL.subList(CommitListener),null)
            }
        }
    }
}
