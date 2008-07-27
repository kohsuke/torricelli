import torricelli.L;
import org.kohsuke.scotland.dir.DirectoryTags;

L l = taglib(L)
DirectoryTags dt = taglib(DirectoryTags);

l.layout(title:my.name) {
    l.left {
        H2("Recent Changes")
        my.parseChangelog().changelog.each { e ->
            DIV(CLASS:"changelog") {
                l.rev(e.@rev)
                text(" by ")
                l.author(e.@author)
                text(" ${e.@date} (${e.@age} ago)")

                l.tags(e.tag*.text())

                DIV(CLASS:"comment",e.description.text())

                DIV(CLASS:"files") {
                    e.file.each { f ->
                        DIV(CLASS:"file") {
                            A(HREF:my.repository.url+"/browse/"+f.text(), f.text())
                            text(' (')
                            int rev = Integer.parseInt(e.@rev);
                            A(HREF:"${f.text()}?r1=${rev-1}&r2=${rev}", "diff")
                            text(')')
                        }
                    }
                }
            }
        }


        include(my,"files.groovy")
    }

    l.right {
        DIV(CLASS:"box") {
            H2("Directories")
            DIV(CLASS:"dirtree") {
                my.listDirs(dt);
            }
        }
    }
}