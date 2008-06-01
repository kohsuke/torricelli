L l = taglib(L)

l.layout {
    src = request.getParameter('src')
    H2("Are you sure about deleting ${src}?")
    FORM(ACTION:"doDelete",METHOD:"post") {
        INPUT(TYPE:"hidden",NAME:"src",VALUE:src)
        INPUT(TYPE:"submit",VALUE:"Yes")
    }
}