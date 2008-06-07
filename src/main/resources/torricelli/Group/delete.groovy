import torricelli.Group.Def;
L l = taglib(L)

l.layout {
    H2("Are you sure about deleting repository group?")
    FORM(ACTION:"doDelete",METHOD:"post") {
        INPUT(TYPE:"submit",VALUE:"Yes")
    }

    l.right {
        Def.navList(l);
    }
}