import org.kohsuke.scotland.dir.DirectoryTags;

L l = taglib(L)
DirectoryTags dt = taglib(DirectoryTags);

l.layout(title:my.name) {
    l.left {
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