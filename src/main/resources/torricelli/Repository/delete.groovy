import torricelli.L;
L l = taglib(L)

l.layout {
    H2("Are you sure about deleting this repository?")
    FORM(ACTION:"doDelete",METHOD:"post") {
        INPUT(TYPE:"submit",VALUE:"Yes")
    }
}