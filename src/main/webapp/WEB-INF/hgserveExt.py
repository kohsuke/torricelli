# Mercurial extension module to be loaded into the "hg serve" process.
# This defines a set of additional commands that the front end uses
from mercurial.hgweb.hgweb_mod import hgweb
from mercurial import templater
import cgi,os

def head(req):
    req.header([('Content-Type','application/xml')])

# test
def do_kohsuke(self, req):
    head(req)
    req.write("Hello")
hgweb.do_kohsuke = do_kohsuke

# list up all directories. unsorted.
def do_dirtree(self,req):
    ctx = self.changectx(req)
    manifest = ctx.manifest()

    dirs = set()
    for n in manifest.iterkeys():
        # what is the values of the manifest map?
        idx=n.rfind('/')
        # am I handling empty directory at the top level right?
        #  -> hg can't seem to handle empty directories
        if idx>-1:
            dirs.add(n[:idx])
    head(req)

    req.write("<dirs>")
    for k in dirs:
        req.write("<dir name='%s'/>"%escape(k))
    req.write("</dirs>")
hgweb.do_dirtree = do_dirtree


# list files in a directory, with its key information
def do_filesummary(self,req):
    head(req)

    ctx = self.changectx(req)
    path = self.cleanpath(req.form['path'][0])
    if path and path[-1] != "/":
        path += "/"
    l = len(path)
    abspath = "/" + path

    req.write("<files>") # TODO: node='%s'>"%hex(ctx.node()))
    for f in ctx.manifest().keys():
        if f[:l] != path:
            continue

        remain = f[l:]
        if "/" not in remain:       # found a file in this path
            short = os.path.basename(remain)
            fctx = ctx.filectx(f)
            req.write("<file name='%s' rev='%s'><author>%s</author><summary>%s</summary></file>" % (short, fctx.rev(), escape(fctx.user()), escape(firstline(fctx.description())) ))
    req.write("</files>")
hgweb.do_filesummary = do_filesummary

def escape(x):
    return cgi.escape(x,True)

def firstline(x):
    return templater.firstline(x)