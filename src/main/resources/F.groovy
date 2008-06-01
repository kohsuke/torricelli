def textBox(String name) {
    INPUT(TYPE:"text",NAME:name,VALUE:my."${name}")
}

def entry(args,body) {
    raw("""
<TR>
    <TD class="setting-leftspace"> </TD>
    <TD class='setting-name>${args.name}</TD>
    """)
    TD(CLASS:"setting-body",body)
    if(args.help!=null)
        TD(CLASS:"setting-help")
    raw("</TR>")
}