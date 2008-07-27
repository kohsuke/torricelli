import torricelli.L;
import torricelli.Group.Def;
L l = taglib(L)

l.layout {
    l.right {
        Def.navList(l);
    }

    H2("Are you sure about deleting repository group?")
    FORM(ACTION:"doDelete",METHOD:"post") {
        INPUT(TYPE:"submit",VALUE:"Yes")
    }
}